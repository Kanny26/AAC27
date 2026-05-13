package com.aac27.joyeria.module.compra.service;

import com.aac27.joyeria.module.catalogo.entity.Producto;
import com.aac27.joyeria.module.catalogo.repository.ProductoRepository;
import com.aac27.joyeria.module.compra.dto.CompraRequest;
import com.aac27.joyeria.module.compra.dto.CompraResponse;
import com.aac27.joyeria.module.compra.entity.*;
import com.aac27.joyeria.module.compra.repository.*;
import com.aac27.joyeria.module.proveedor.entity.Proveedor;
import com.aac27.joyeria.module.proveedor.repository.ProveedorRepository;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.module.seguridad.repository.UsuarioRepository;
import com.aac27.joyeria.shared.exception.ConflictoDatosException;
import com.aac27.joyeria.shared.exception.RecursoNoEncontradoException;
import com.aac27.joyeria.shared.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Compras — Módulo más complejo del backend junto con Ventas.
 *
 * <p>
 * <strong>Arquitectura de transacción al registrar una compra:</strong>
 * 
 * <pre>
 * ┌─────────────────────────────────────────────────────────┐
 * │  @Transactional (REQUIRED) — UNA sola unidad de trabajo │
 * │                                                         │
 * │  1. Validar proveedor activo                           │
 * │  2. Validar cada producto existe                        │
 * │  3. Construir Compra con Detalles                       │
 * │  4. Calcular subtotales y total                         │
 * │  5. Guardar compra (cascade → detalles)                 │
 * │  6. Por cada detalle:                                   │
 * │     a. Cargar producto con @Lock                        │
 * │     b. Incrementar stock                                │
 * │     c. Crear InventarioMovimiento                       │
 * │  7. Si crédito → crear CreditoCompra                    │
 * │                                                         │
 * │  Si CUALQUIER paso falla → ROLLBACK TOTAL              │
 * └─────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>
 * <strong>Optimistic Locking vs Pessimistic Locking:</strong>
 *
 * <p>
 * Para COMPRAS usamos <strong>Pessimistic Locking</strong>
 * ({@code SELECT FOR UPDATE}).
 * Razón: al recibir mercancía, el administrador SABE que está actualizando un
 * producto
 * específico. Bloquear la fila durante la transacción es seguro y evita
 * conflictos.
 * Solo el administrador registra compras (baja concurrencia).
 *
 * <p>
 * Para VENTAS usamos <strong>Optimistic Locking</strong> ({@code @Version}).
 * Razón: múltiples vendedores pueden vender el mismo producto simultáneamente
 * (alta concurrencia). Preferimos detectar conflictos y reintentar antes que
 * bloquear.
 *
 * <p>
 * Referencia: RF11, RF-C01, RF-C02, Sección 3.14, 3.15, 3.24. RNF-REN04.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompraService {

        private final CompraRepository compraRepository;
        private final CreditoCompraRepository creditoCompraRepository;
        private final InventarioMovimientoRepository inventarioMovimientoRepository;
        private final ProveedorRepository proveedorRepository;
        private final ProductoRepository productoRepository;
        private final UsuarioRepository usuarioRepository;
        private final MetodoPagoRepository metodoPagoRepository;

        // ============================================================
        // REGISTRAR COMPRA — TRANSACCIÓN COMPLETA
        // ============================================================

        /**
         * Registra una nueva orden de compra con todos sus efectos colaterales.
         *
         * <p>
         * {@code @Transactional}: todo ocurre en UNA transacción.
         * Si el paso 6 falla (stock), los pasos 3-5 también hacen rollback.
         * La BD queda en estado consistente siempre.
         *
         * <p>
         * {@code @Retryable}: si hay conflicto de locking al actualizar stock,
         * Spring Retry reintenta automáticamente hasta 3 veces con backoff exponencial.
         * Referencia: RNF-REN04.
         *
         * @param request   DTO con datos de la compra
         * @param usuarioId ID del administrador que registra
         * @return CompraResponse con todos los datos de la compra creada
         */
        @Transactional
        @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
        public CompraResponse registrarCompra(CompraRequest request, Long usuarioId) {
                log.info("Registrando compra para proveedor ID: {}", request.getProveedorId());

                // ── PASO 1: Validar entidades cabecera ─────────────────

                Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Proveedor", request.getProveedorId()));

                if (proveedor.getEstado() == Proveedor.EstadoProveedor.inactivo) {
                        throw new ReglaNegocioException(
                                        "No se puede registrar una compra con un proveedor inactivo: "
                                                        + proveedor.getNombre());
                }

                Usuario usuario = usuarioRepository.findById(usuarioId)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", usuarioId));

                MetodoPago metodoPago = metodoPagoRepository.findById(request.getMetodoPagoId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Método de pago", request.getMetodoPagoId()));

                // ── PASO 2: Validar fecha de entrega ───────────────────

                if (request.getFechaEntregaEsperada() != null &&
                                request.getFechaEntregaEsperada().isBefore(request.getFechaFactura())) {
                        throw new ReglaNegocioException(
                                        "La fecha de entrega esperada no puede ser anterior a la fecha de factura");
                }

                // Si es crédito, validar que venga fecha de vencimiento
                Compra.TipoPago tipoPago = Compra.TipoPago.valueOf(request.getTipoPago());
                if (tipoPago == Compra.TipoPago.credito && request.getFechaVencimientoCredito() == null) {
                        throw new ReglaNegocioException(
                                        "Para compras a crédito se requiere la fecha de vencimiento del crédito");
                }
                if (request.getFechaVencimientoCredito() != null &&
                                !request.getFechaVencimientoCredito().isAfter(request.getFechaFactura())) {
                        throw new ReglaNegocioException(
                                        "La fecha de vencimiento del crédito debe ser posterior a la fecha de factura");
                }

                // ── PASO 3: Construir los detalles y calcular totales ──

                List<DetalleCompra> detalles = new ArrayList<>();
                BigDecimal subtotalCompra = BigDecimal.ZERO;

                for (CompraRequest.DetalleCompraRequest itemReq : request.getDetalles()) {

                        Producto producto = productoRepository.findById(itemReq.getProductoId())
                                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                                        "Producto", itemReq.getProductoId()));

                        // Subtotal de este ítem: cantidad × precio unitario
                        BigDecimal subtotalItem = itemReq.getPrecioUnitario()
                                        .multiply(BigDecimal.valueOf(itemReq.getCantidadPedida()));

                        DetalleCompra detalle = DetalleCompra.builder()
                                        .producto(producto)
                                        .cantidadPedida(itemReq.getCantidadPedida())
                                        .precioUnitario(itemReq.getPrecioUnitario())
                                        .subtotal(subtotalItem)
                                        .build();

                        detalles.add(detalle);
                        subtotalCompra = subtotalCompra.add(subtotalItem);
                }

                // ── PASO 4: Construir y guardar la Compra ──────────────

                Compra compra = Compra.builder()
                                .proveedor(proveedor)
                                .usuario(usuario)
                                .fechaFactura(request.getFechaFactura())
                                .fechaEntregaEsperada(request.getFechaEntregaEsperada())
                                .metodoPago(metodoPago)
                                .tipoPago(tipoPago)
                                .subtotal(subtotalCompra)
                                .total(subtotalCompra) // En v1 sin impuestos adicionales
                                .notas(request.getNotas())
                                .build();

                // Agrega detalles y establece la referencia bidireccional (compra_id en cada
                // detalle)
                for (DetalleCompra detalle : detalles) {
                        compra.agregarDetalle(detalle);
                }

                // Spring Data guarda la Compra y, por CascadeType.PERSIST, también los
                // DetalleCompra
                Compra compraGuardada = compraRepository.save(compra);
                log.info("Compra ID {} guardada. Procesando stock...", compraGuardada.getCompraId());

                // ── PASO 5: Actualizar stock y registrar movimientos ───

                for (DetalleCompra detalle : compraGuardada.getDetalles()) {
                        actualizarStockEntrada(detalle, compraGuardada.getCompraId(), usuario);
                }

                // ── PASO 6: Crear CreditoCompra si aplica ─────────────

                CreditoCompra creditoCreado = null;
                if (tipoPago == Compra.TipoPago.credito) {
                        creditoCreado = crearCreditoCompra(compraGuardada, request.getFechaVencimientoCredito());
                        log.info("Crédito creado para compra ID: {}, vence: {}",
                                        compraGuardada.getCompraId(), request.getFechaVencimientoCredito());
                }

                log.info("Compra ID {} registrada exitosamente. Total: {}",
                                compraGuardada.getCompraId(), compraGuardada.getTotal());

                return mapearACompraResponse(compraGuardada, creditoCreado);
        }

        // ============================================================
        // ACTUALIZAR STOCK — CON PESSIMISTIC LOCKING
        // ============================================================

        /**
         * Actualiza el stock del producto y crea el movimiento de inventario.
         *
         * <p>
         * <strong>¿Por qué Pessimistic Locking aquí?</strong>
         * El repositorio usa {@code @Lock(LockModeType.PESSIMISTIC_WRITE)} que genera
         * {@code SELECT ... FOR UPDATE} en MySQL. Esto bloquea la FILA del producto
         * durante esta transacción, evitando que otra transacción la lea hasta liberar.
         *
         * <p>
         * Para compras es apropiado porque:
         * <ol>
         * <li>Solo el administrador registra compras (baja concurrencia)</li>
         * <li>Necesitamos stock_antes confiable para el movimiento de inventario</li>
         * <li>El bloqueo es corto (unos milisegundos)</li>
         * </ol>
         *
         * @param detalle  Detalle de la compra con la cantidad a ingresar
         * @param compraId ID de la compra origen del movimiento
         * @param usuario  Usuario que realiza la operación
         */
        private void actualizarStockEntrada(DetalleCompra detalle, Long compraId, Usuario usuario) {
                // findByIdWithLock genera: SELECT * FROM producto WHERE producto_id = ? FOR
                // UPDATE
                // La FK del detalle ya tiene el objeto producto cargado, pero necesitamos
                // recargarlo con el lock explícito para garantizar consistencia
                Producto producto = productoRepository.findByIdWithLock(
                                detalle.getProducto().getProductoId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Producto", detalle.getProducto().getProductoId()));

                int stockAntes = producto.getStock();
                producto.incrementarStock(detalle.getCantidadPedida());
                // Hibernate detecta el cambio y genera el UPDATE automáticamente al final de la
                // tx

                // Registro inmutable del movimiento (RF15)
                InventarioMovimiento movimiento = InventarioMovimiento.builder()
                                .producto(producto)
                                .tipo(InventarioMovimiento.TipoMovimiento.entrada)
                                .cantidad(detalle.getCantidadPedida())
                                .stockAntes(stockAntes)
                                .stockDespues(producto.getStock())
                                .referenciaTipo("compra")
                                .referenciaId(compraId)
                                .usuario(usuario)
                                .motivo("Ingreso por compra #" + compraId)
                                .build();

                inventarioMovimientoRepository.save(movimiento);

                log.debug("Stock producto ID {}: {} → {} (diferencia: +{})",
                                producto.getProductoId(), stockAntes,
                                producto.getStock(), detalle.getCantidadPedida());
        }

        // ============================================================
        // CREAR CRÉDITO DE COMPRA
        // ============================================================

        /**
         * Crea el registro de crédito automáticamente cuando la compra es a crédito.
         *
         * <p>
         * Llamado DENTRO de la transacción principal de {@link #registrarCompra}.
         * Si falla, toda la transacción hace rollback (no puede existir una compra
         * a crédito sin su registro de crédito correspondiente).
         */
        private CreditoCompra crearCreditoCompra(Compra compra, LocalDate fechaVencimiento) {
                // Validar que no exista ya un crédito para esta compra (unicidad)
                if (creditoCompraRepository.existsByCompraCompraId(compra.getCompraId())) {
                        throw new ConflictoDatosException(
                                        "La compra " + compra.getCompraId() + " ya tiene un crédito registrado");
                }

                CreditoCompra credito = CreditoCompra.builder()
                                .compra(compra)
                                .montoTotal(compra.getTotal())
                                .saldoPendiente(compra.getTotal()) // Inicia sin abonos
                                .fechaInicio(compra.getFechaFactura())
                                .fechaVencimiento(fechaVencimiento)
                                .estado(CreditoCompra.EstadoCredito.activo)
                                .build();

                return creditoCompraRepository.save(credito);
        }

        // ============================================================
        // CONSULTAS
        // ============================================================

        /**
         * Obtiene una compra por ID con sus detalles y crédito.
         */
        @Transactional(readOnly = true)
        public CompraResponse obtenerCompra(Long compraId) {
                Compra compra = compraRepository.findById(compraId)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Compra", compraId));

                CreditoCompra credito = creditoCompraRepository
                                .findByCompraCompraId(compraId).orElse(null);

                return mapearACompraResponse(compra, credito);
        }

        /**
         * Lista compras con paginación server-side.
         */
        @Transactional(readOnly = true)
        public Page<CompraResponse> listarCompras(Long proveedorId, Pageable pageable) {
                Page<Compra> compras;
                if (proveedorId != null) {
                        compras = compraRepository.findByProveedorProveedorId(proveedorId, pageable);
                } else {
                        compras = compraRepository.findAll(pageable);
                }
                return compras.map(c -> mapearACompraResponse(c, null));
        }

        // ============================================================
        // MAPPER — Entidad → DTO de Respuesta
        // ============================================================

        /**
         * Convierte la entidad Compra a su DTO de respuesta.
         *
         * <p>
         * Los mappers manuales (sin MapStruct) son claros y controlables.
         * Para mapeos más complejos se puede agregar MapStruct en Fase 2.
         */
        private CompraResponse mapearACompraResponse(Compra compra, CreditoCompra credito) {

                List<CompraResponse.DetalleCompraResponse> detallesDto = compra.getDetalles()
                                .stream()
                                .map(d -> CompraResponse.DetalleCompraResponse.builder()
                                                .detalleId(d.getDetalleCompraId())
                                                .productoId(d.getProducto().getProductoId())
                                                .codigoProducto(d.getProducto().getCodigo())
                                                .nombreProducto(d.getProducto().getNombre())
                                                .cantidadPedida(d.getCantidadPedida())
                                                .cantidadRecibida(d.getCantidadRecibida())
                                                .cantidadPendiente(d.getCantidadPendiente())
                                                .precioUnitario(d.getPrecioUnitario())
                                                .subtotal(d.getSubtotal())
                                                .build())
                                .toList();

                CompraResponse.CreditoCompraResponse creditoDto = null;
                if (credito != null) {
                        creditoDto = CompraResponse.CreditoCompraResponse.builder()
                                        .creditoId(credito.getCreditoId())
                                        .montoTotal(credito.getMontoTotal())
                                        .saldoPendiente(credito.getSaldoPendiente())
                                        .fechaVencimiento(credito.getFechaVencimiento())
                                        .estado(credito.getEstado().name())
                                        .build();
                }

                return CompraResponse.builder()
                                .compraId(compra.getCompraId())
                                .nombreProveedor(compra.getProveedor().getNombre())
                                .nombreUsuario(compra.getUsuario().getNombre())
                                .fechaFactura(compra.getFechaFactura())
                                .fechaEntregaEsperada(compra.getFechaEntregaEsperada())
                                .metodoPago(compra.getMetodoPago().getNombre())
                                .tipoPago(compra.getTipoPago().name())
                                .estado(compra.getEstado().name())
                                .subtotal(compra.getSubtotal())
                                .total(compra.getTotal())
                                .notas(compra.getNotas())
                                .creadoEn(compra.getCreatedAt())
                                .detalles(detallesDto)
                                .credito(creditoDto)
                                .build();
        }
}
