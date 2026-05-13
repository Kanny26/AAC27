package com.aac27.joyeria.module.venta.service;

import com.aac27.joyeria.module.catalogo.entity.Producto;
import com.aac27.joyeria.module.compra.entity.InventarioMovimiento;
import com.aac27.joyeria.module.compra.entity.MetodoPago;
import com.aac27.joyeria.module.compra.repository.InventarioMovimientoRepository;
import com.aac27.joyeria.module.compra.repository.MetodoPagoRepository;
import com.aac27.joyeria.module.compra.repository.ProductoLockRepository;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.module.seguridad.repository.UsuarioRepository;
import com.aac27.joyeria.module.venta.dto.VentaRequest;
import com.aac27.joyeria.module.venta.dto.VentaResponse;
import com.aac27.joyeria.module.venta.entity.*;
import com.aac27.joyeria.module.venta.repository.*;
import com.aac27.joyeria.shared.exception.RecursoNoEncontradoException;
import com.aac27.joyeria.shared.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Ventas — El más complejo del sistema.
 *
 * <p>
 * <strong>Comparación Optimistic vs Pessimistic Locking:</strong>
 *
 * <table border="1">
 * <tr>
 * <th>Criterio</th>
 * <th>Pessimistic (Compras)</th>
 * <th>Optimistic (Ventas)</th>
 * </tr>
 * <tr>
 * <td>Concurrencia</td>
 * <td>Baja</td>
 * <td>Alta</td>
 * </tr>
 * <tr>
 * <td>Mecanismo</td>
 * <td>SELECT FOR UPDATE</td>
 * <td>@Version field</td>
 * </tr>
 * <tr>
 * <td>Conflicto</td>
 * <td>Bloquea y espera</td>
 * <td>Detecta y reintenta</td>
 * </tr>
 * <tr>
 * <td>Rendimiento</td>
 * <td>Más lento con alta concurrencia</td>
 * <td>Mejor bajo alta carga</td>
 * </tr>
 * <tr>
 * <td>Riesgo</td>
 * <td>Deadlock posible</td>
 * <td>Starvation si muchos reintentos</td>
 * </tr>
 * </table>
 *
 * <p>
 * <strong>¿Por qué Optimistic en Ventas?</strong>
 * Múltiples vendedores pueden intentar vender el MISMO producto al mismo
 * tiempo.
 * Bloquear la fila (pessimistic) haría que todos esperen en cola.
 * Con @Version: si dos vendedores leen stock=3 y ambas intentan guardar
 * stock=2,
 * la SEGUNDA detecta que version cambió → falla → Spring Retry reintenta →
 * funciona.
 * El cliente de la segunda venta ni se entera — es transparente.
 *
 * <p>
 * <strong>Transacciones anidadas:</strong>
 * Todo ocurre en UNA transacción REQUIRED. Si cualquier paso falla → ROLLBACK
 * TOTAL.
 * No hay propagación REQUIRES_NEW aquí — garantizamos atomicidad completa.
 *
 * <p>
 * Referencia: RF17-RF21, RF-V01 a RF-V04, RNF-REN04, Sección 3.16-3.19.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VentaService {

        private final VentaRepository ventaRepository;
        private final ClienteRepository clienteRepository;
        private final ProductoLockRepository productoLockRepository;
        private final InventarioMovimientoRepository inventarioMovimientoRepository;
        private final MetodoPagoRepository metodoPagoRepository;
        private final UsuarioRepository usuarioRepository;
        private final ApartadoRepository apartadoRepository;
        private final AbonoApartadoRepository abonoApartadoRepository;

        // ============================================================
        // CONSTANTE — Puntos de Fidelidad (RF-V03)
        // ============================================================

        /**
         * Progresión de puntos: 1 punto por cada 10,000 COP en compras.
         * Se puede externalizar a application.yml en el futuro.
         */
        private static final int PESOS_POR_PUNTO = 10_000;

        /** Valor de canje: 1 punto = 100 COP de descuento. */
        private static final int PESOS_POR_PUNTO_CANJE = 100;

        // ============================================================
        // REGISTRAR VENTA DIRECTA (Contado / Crédito / Anticipo)
        // ============================================================

        /**
         * Registra una venta con su flujo completo de transacción.
         *
         * <p>
         * {@code @Retryable}: si el Optimistic Lock falla al actualizar stock,
         * Spring reintenta la transacción completa hasta 3 veces.
         * El backoff exponencial (100ms → 200ms → 400ms) reduce la presión.
         *
         * @param request         DTO con datos de la venta
         * @param usuarioVendedor Usuario vendedor autenticado
         * @return VentaResponse con todos los datos de confirmación
         */
        @Transactional
        @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
        public VentaResponse registrarVenta(VentaRequest request, Usuario usuarioVendedor) {
                log.info("Iniciando registro de venta por usuario: {}", usuarioVendedor.getCorreo());

                // ── PASO 1: Cargar entidades referenciadas ─────────────

                Cliente cliente = null;
                if (request.getClienteId() != null) {
                        cliente = clienteRepository.findById(request.getClienteId())
                                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                                        "Cliente", request.getClienteId()));
                }

                MetodoPago metodoPago = metodoPagoRepository.findById(request.getMetodoPagoId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Método de pago", request.getMetodoPagoId()));

                // ── PASO 2: Validar stock ANTES de cualquier operación ─

                // IMPORTANTE: Validamos ANTES de reservar para evitar rollbacks innecesarios.
                // Si el stock es insuficiente, fallamos rápido con error claro.
                for (VentaRequest.ItemVentaRequest item : request.getItems()) {
                        Producto producto = productoLockRepository
                                        .findById(item.getProductoId())
                                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                                        "Producto", item.getProductoId()));

                        if (producto.getEstado() == Producto.EstadoProducto.inactivo) {
                                throw new ReglaNegocioException(
                                                "El producto '" + producto.getNombre() + "' está inactivo");
                        }
                        if (producto.getStock() < item.getCantidad()) {
                                throw new ReglaNegocioException(
                                                "Stock insuficiente para '" + producto.getNombre() +
                                                                "'. Disponible: " + producto.getStock() +
                                                                ", solicitado: " + item.getCantidad());
                        }
                }

                // ── PASO 3: Construir detalles y calcular totales ──────

                List<DetalleVenta> detalles = new ArrayList<>();
                BigDecimal subtotalVenta = BigDecimal.ZERO;
                BigDecimal descuentoTotalVenta = BigDecimal.ZERO;

                for (VentaRequest.ItemVentaRequest item : request.getItems()) {

                        // Cargamos el producto (ya validado en paso 2)
                        Producto producto = productoLockRepository.findById(item.getProductoId())
                                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                                        "Producto", item.getProductoId()));

                        // PRECIO HISTÓRICO: usamos el precio actual del producto
                        // pero lo incluimos en el detalle como campo inmutable.
                        // Si el producto sube de precio en el futuro, esta venta conserva el original.
                        BigDecimal precioUnitario = producto.getPrecioVenta();
                        BigDecimal descuentoItem = item.getDescuentoItem() != null
                                        ? item.getDescuentoItem()
                                        : BigDecimal.ZERO;

                        // Validar que el descuento no exceda el precio
                        if (descuentoItem.compareTo(precioUnitario) > 0) {
                                throw new ReglaNegocioException(
                                                "El descuento del ítem supera el precio del producto: "
                                                                + producto.getNombre());
                        }

                        BigDecimal precioConDescuento = precioUnitario.subtract(descuentoItem);
                        BigDecimal subtotalItem = precioConDescuento
                                        .multiply(BigDecimal.valueOf(item.getCantidad()));

                        // Garantía (RF-V04): calcular fecha de vencimiento
                        LocalDate garantiaVence = null;
                        int garantiaMeses = item.getGarantiaMeses() != null ? item.getGarantiaMeses() : 0;
                        if (garantiaMeses > 0) {
                                garantiaVence = LocalDate.now().plusMonths(garantiaMeses);
                        }

                        DetalleVenta detalle = DetalleVenta.builder()
                                        .producto(producto)
                                        .cantidad(item.getCantidad())
                                        .precioUnitario(precioUnitario) // Precio histórico — inmutable
                                        .descuentoItem(descuentoItem)
                                        .precioConDescuento(precioConDescuento)
                                        .subtotal(subtotalItem)
                                        .garantiaMeses(garantiaMeses)
                                        .garantiaVence(garantiaVence)
                                        .notasItem(item.getNotasItem())
                                        .build();

                        detalles.add(detalle);
                        subtotalVenta = subtotalVenta.add(precioUnitario.multiply(
                                        BigDecimal.valueOf(item.getCantidad())));
                        descuentoTotalVenta = descuentoTotalVenta.add(descuentoItem
                                        .multiply(BigDecimal.valueOf(item.getCantidad())));
                }

                // ── PASO 4: Calcular descuento por puntos (RF-V03) ─────

                int puntosFidelidadUsados = 0;
                BigDecimal descuentoPuntos = BigDecimal.ZERO;

                if (cliente != null && request.getPuntosAUsarFidelidad() != null
                                && request.getPuntosAUsarFidelidad() > 0) {

                        // Validar que el cliente tenga suficientes puntos
                        if (request.getPuntosAUsarFidelidad() > cliente.getPuntosFidelidad()) {
                                throw new ReglaNegocioException(
                                                "El cliente no tiene suficientes puntos. Disponible: "
                                                                + cliente.getPuntosFidelidad()
                                                                + ", solicitado: " + request.getPuntosAUsarFidelidad());
                        }

                        puntosFidelidadUsados = request.getPuntosAUsarFidelidad();
                        // 1 punto = PESOS_POR_PUNTO_CANJE (100 COP)
                        descuentoPuntos = BigDecimal.valueOf(
                                        (long) puntosFidelidadUsados * PESOS_POR_PUNTO_CANJE);
                }

                // Total final = subtotal - descuentos - descuento por puntos
                BigDecimal totalVenta = subtotalVenta
                                .subtract(descuentoTotalVenta)
                                .subtract(descuentoPuntos)
                                .max(BigDecimal.ZERO); // Nunca negativo

                // ── PASO 5: Generar número de factura único ────────────

                String numeroFactura = generarNumeroFactura();

                // ── PASO 6: Calcular puntos ganados (RF-V03) ───────────

                // 1 punto por cada PESOS_POR_PUNTO (10,000) COP del total
                int puntosGanados = totalVenta
                                .divide(BigDecimal.valueOf(PESOS_POR_PUNTO), 0, RoundingMode.DOWN)
                                .intValue();

                // ── PASO 7: Crear y persistir la Venta ────────────────

                Venta venta = Venta.builder()
                                .numeroFactura(numeroFactura)
                                .cliente(cliente)
                                .usuario(usuarioVendedor)
                                .modalidadPago(Venta.ModalidadPago.valueOf(request.getModalidadPago()))
                                .subtotal(subtotalVenta)
                                .descuentoTotal(descuentoTotalVenta)
                                .puntosFidelidadUsados(puntosFidelidadUsados)
                                .descuentoPuntos(descuentoPuntos)
                                .total(totalVenta)
                                .puntosGanados(puntosGanados)
                                .observaciones(request.getObservaciones())
                                .build();

                for (DetalleVenta detalle : detalles) {
                        venta.agregarDetalle(detalle);
                }

                // Registrar el pago inicial
                PagoVenta pagoInicial = PagoVenta.builder()
                                .metodoPago(metodoPago)
                                .monto(request.getMontoPagado())
                                .tipoPago(request.getModalidadPago().equals("contado")
                                                ? PagoVenta.TipoPagoVenta.pago_completo
                                                : PagoVenta.TipoPagoVenta.anticipo)
                                .estado(PagoVenta.EstadoPago.registrado)
                                .build();
                venta.agregarPago(pagoInicial);

                Venta ventaGuardada = ventaRepository.save(venta);
                log.info("Venta {} creada. Procesando stock...", numeroFactura);

                // ── PASO 8: Actualizar stock y crear movimientos ──────

                // Optimistic Locking con @Version:
                // Si otra transacción modificó el producto →
                // ObjectOptimisticLockingFailureException
                // → @Retryable lo captura y reintenta la transacción COMPLETA
                for (DetalleVenta detalle : ventaGuardada.getDetalles()) {
                        descontarStockConLock(detalle, ventaGuardada.getVentaId(), usuarioVendedor);
                }

                // ── PASO 9: Actualizar puntos del cliente ─────────────

                if (cliente != null) {
                        // Primero restamos los canjeados, luego sumamos los ganados
                        if (puntosFidelidadUsados > 0) {
                                cliente.canjearPuntos(puntosFidelidadUsados);
                        }
                        if (puntosGanados > 0) {
                                cliente.sumarPuntos(puntosGanados);
                        }
                        clienteRepository.save(cliente);
                        log.info("Cliente {}: -{}pts canjeados, +{}pts ganados. Saldo: {}",
                                        cliente.getNombre(), puntosFidelidadUsados,
                                        puntosGanados, cliente.getPuntosFidelidad());
                }

                log.info("Venta {} registrada. Total: {} | Puntos ganados: {}",
                                numeroFactura, totalVenta, puntosGanados);

                return VentaResponse.builder()
                                .ventaId(ventaGuardada.getVentaId())
                                .numeroFactura(numeroFactura)
                                .total(totalVenta)
                                .puntosFidelidadUsados(puntosFidelidadUsados)
                                .puntosGanados(puntosGanados)
                                .saldoPuntosCliente(cliente != null ? cliente.getPuntosFidelidad() : null)
                                .estado(ventaGuardada.getEstado().name())
                                .build();
        }

        // ============================================================
        // REGISTRAR APARTADO / LAYAWAY (RF-V01)
        // ============================================================

        /**
         * Registra un apartado: reserva el stock sin descontarlo.
         *
         * <p>
         * <strong>Diferencia clave con venta directa:</strong>
         * Aquí el stock se RESERVA (inventario tipo 'reserva'), NO se descuenta.
         * El producto permanece físicamente en tienda.
         * Solo al completar el pago se descuenta (genera venta definitiva).
         */
        @Transactional
        public VentaResponse registrarApartado(VentaRequest request, Usuario usuarioVendedor) {
                if (request.getClienteId() == null) {
                        throw new ReglaNegocioException("El apartado requiere un cliente identificado");
                }

                Cliente cliente = clienteRepository.findById(request.getClienteId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Cliente", request.getClienteId()));

                if (request.getFechaLimiteApartado() == null) {
                        throw new ReglaNegocioException("El apartado requiere una fecha límite de pago");
                }
                if (!request.getFechaLimiteApartado().isAfter(LocalDate.now())) {
                        throw new ReglaNegocioException(
                                        "La fecha límite del apartado debe ser posterior a hoy");
                }

                // Solo permite 1 producto por apartado (puede extenderse en Fase 2)
                if (request.getItems().size() != 1) {
                        throw new ReglaNegocioException(
                                        "El apartado solo permite un producto por operación");
                }

                VentaRequest.ItemVentaRequest item = request.getItems().get(0);
                Producto producto = productoLockRepository.findById(item.getProductoId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Producto", item.getProductoId()));

                // Validar stock
                if (producto.getStock() < item.getCantidad()) {
                        throw new ReglaNegocioException("Stock insuficiente para apartar el producto");
                }

                MetodoPago metodoPago = metodoPagoRepository.findById(request.getMetodoPagoId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Método de pago", request.getMetodoPagoId()));

                BigDecimal totalAPagar = producto.getPrecioVenta()
                                .multiply(BigDecimal.valueOf(item.getCantidad()));

                // Validar primer abono mínimo 20%
                BigDecimal primerAbono = request.getMontoPagado();
                BigDecimal minimoRequerido = totalAPagar.multiply(BigDecimal.valueOf(0.20));
                if (primerAbono.compareTo(minimoRequerido) < 0) {
                        throw new ReglaNegocioException(
                                        "El primer abono debe ser al menos el 20% del total ("
                                                        + minimoRequerido.setScale(0, RoundingMode.HALF_UP) + " COP)");
                }

                // ── PASO 3: Descontar stock físicamente (Reserva) ──────────
                // Aunque es un apartado, debemos restar el stock para que no sea
                // vendido por otros. Se marca como 'reserva' en el historial.
                int stockAntes = producto.getStock();
                producto.decrementarStock(item.getCantidad());

                // ── PASO 4: Registrar el Apartado ───────────────────────
                Apartado apartado = Apartado.builder()
                                .cliente(cliente)
                                .usuario(usuarioVendedor)
                                .producto(producto)
                                .cantidad(item.getCantidad())
                                .precioUnitario(producto.getPrecioVenta())
                                .totalAPagar(totalAPagar)
                                .totalAbonado(primerAbono)
                                .saldoPendiente(totalAPagar.subtract(primerAbono))
                                .fechaLimite(request.getFechaLimiteApartado())
                                .observaciones(request.getObservaciones())
                                .primerAbonoOk(true)
                                .estado(Apartado.EstadoApartado.activo)
                                .build();

                Apartado apartadoGuardado = apartadoRepository.save(apartado);

                // ── PASO 5: Registrar Movimiento e Historial ────────────
                InventarioMovimiento movimiento = InventarioMovimiento.builder()
                                .producto(producto)
                                .tipo(InventarioMovimiento.TipoMovimiento.reserva)
                                .cantidad(-item.getCantidad())
                                .stockAntes(stockAntes)
                                .stockDespues(producto.getStock())
                                .referenciaTipo("apartado")
                                .referenciaId(apartadoGuardado.getApartadoId())
                                .usuario(usuarioVendedor)
                                .motivo("Reserva por nuevo apartado #" + apartadoGuardado.getApartadoId())
                                .build();
                inventarioMovimientoRepository.save(movimiento);

                // Agregar el primer abono
                AbonoApartado primerAbonoEntity = AbonoApartado.builder()
                                .apartado(apartadoGuardado)
                                .metodoPago(metodoPago)
                                .monto(primerAbono)
                                .esPrimerAbono(true)
                                .build();
                abonoApartadoRepository.save(primerAbonoEntity);

                log.info("Apartado #{} creado. Primer abono: {} / Total: {}",
                                apartadoGuardado.getApartadoId(), primerAbono, totalAPagar);

                return VentaResponse.builder()
                                .ventaId(apartadoGuardado.getApartadoId()) // Reutilizamos el campo para el ID
                                .numeroFactura("APT-" + apartadoGuardado.getApartadoId())
                                .total(totalAPagar)
                                .estado(apartadoGuardado.getEstado().name())
                                .build();
        }

        // ============================================================
        // MÉTODOS PRIVADOS
        // ============================================================

        /**
         * Descuenta stock usando Optimistic Locking (@Version en Producto).
         *
         * <p>
         * El producto tiene un campo {@code version} (integer).
         * Hibernate genera:
         * {@code UPDATE producto SET stock=?, version=version+1 WHERE producto_id=? AND version=?}
         * Si {@code version} en BD ya cambió → 0 filas actualizadas → Hibernate
         * lanza {@code ObjectOptimisticLockingFailureException} → @Retryable reintenta.
         */
        private void descontarStockConLock(DetalleVenta detalle, Long ventaId, Usuario usuario) {
                Producto producto = productoLockRepository.findById(
                                detalle.getProducto().getProductoId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Producto", detalle.getProducto().getProductoId()));

                int stockAntes = producto.getStock();
                // decrementarStock valida que no quede negativo y lanza IllegalStateException
                // si falla
                producto.decrementarStock(detalle.getCantidad());

                InventarioMovimiento movimiento = InventarioMovimiento.builder()
                                .producto(producto)
                                .tipo(InventarioMovimiento.TipoMovimiento.salida)
                                .cantidad(-detalle.getCantidad()) // Negativo: salida
                                .stockAntes(stockAntes)
                                .stockDespues(producto.getStock())
                                .referenciaTipo("venta")
                                .referenciaId(ventaId)
                                .usuario(usuario)
                                .motivo("Venta #" + ventaId)
                                .build();

                inventarioMovimientoRepository.save(movimiento);

                log.debug("Stock producto ID {}: {} → {} (-{})",
                                producto.getProductoId(), stockAntes,
                                producto.getStock(), detalle.getCantidad());
        }

        /**
         * Genera el número de factura consecutivo por año.
         * Formato: VTA-2026-00001
         *
         * <p>
         * Usa una consulta que busca el último número del año actual
         * y genera el siguiente. La operación es atómica dentro de la transacción.
         */
        private String generarNumeroFactura() {
                String anio = String.valueOf(LocalDate.now().getYear());
                long siguiente = ventaRepository.contarVentasDelAnio(anio) + 1;
                return String.format("VTA-%s-%05d", anio, siguiente);
        }
}
