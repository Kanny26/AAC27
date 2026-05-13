package com.aac27.joyeria.module.venta.service;

import com.aac27.joyeria.module.catalogo.entity.Producto;
import com.aac27.joyeria.module.compra.entity.InventarioMovimiento;
import com.aac27.joyeria.module.compra.entity.MetodoPago;
import com.aac27.joyeria.module.compra.repository.InventarioMovimientoRepository;
import com.aac27.joyeria.module.compra.repository.MetodoPagoRepository;
import com.aac27.joyeria.module.compra.repository.ProductoLockRepository;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.module.seguridad.repository.UsuarioRepository;
import com.aac27.joyeria.module.venta.dto.AbonoApartadoRequest;
import com.aac27.joyeria.module.venta.dto.ApartadoResponse;
import com.aac27.joyeria.module.venta.entity.*;
import com.aac27.joyeria.module.venta.repository.*;
import com.aac27.joyeria.shared.exception.RecursoNoEncontradoException;
import com.aac27.joyeria.shared.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Servicio de Apartados/Layaway — Ciclo de vida completo.
 *
 * <p><strong>Flujo del apartado (RF-V01):</strong>
 * <pre>
 * ┌──────────────────────────────────────────────────────────────┐
 * │  [ACTIVO]                                                    │
 * │   └─ registrarAbono()  → actualiza saldo                     │
 * │       └─ Si saldo = 0  → completarApartado() automático      │
 * │   └─ cancelarApartado() → stock liberado → [CANCELADO]       │
 * │   └─ vencerApartado()   → si supera fecha_limite → [VENCIDO] │
 * │                                                              │
 * │  [COMPLETADO] → ventaDefinitiva creada, stock descontado     │
 * └──────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>Referencia: RF-V01, Sección 3.19.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApartadoService {

    private final ApartadoRepository              apartadoRepository;
    private final ClienteRepository               clienteRepository;
    private final VentaRepository                 ventaRepository;
    private final MetodoPagoRepository            metodoPagoRepository;
    private final InventarioMovimientoRepository  inventarioMovimientoRepository;
    private final ProductoLockRepository          productoLockRepository;
    private final UsuarioRepository               usuarioRepository;

    // ──────────────────────────────────────────────────────────
    // LISTAR / CONSULTAR
    // ──────────────────────────────────────────────────────────

    /**
     * Lista apartados de un cliente por estado, paginados.
     */
    @Transactional(readOnly = true)
    public Page<ApartadoResponse> listarPorCliente(Long clienteId,
                                                   Apartado.EstadoApartado estado,
                                                   Pageable pageable) {
        return apartadoRepository
                .findByClienteClienteIdAndEstado(clienteId, estado, pageable)
                .map(this::toResponse);
    }

    /**
     * Lista todos los apartados activos paginados (para el panel de administración).
     */
    @Transactional(readOnly = true)
    public Page<ApartadoResponse> listarActivos(Pageable pageable) {
        return apartadoRepository
                .findByEstadoOrderByFechaLimiteAsc(Apartado.EstadoApartado.activo, pageable)
                .map(this::toResponse);
    }

    /**
     * Obtiene el detalle completo de un apartado.
     */
    @Transactional(readOnly = true)
    public ApartadoResponse obtenerApartado(Long apartadoId) {
        Apartado apartado = buscarApartado(apartadoId);
        return toResponse(apartado);
    }

    // ──────────────────────────────────────────────────────────
    // REGISTRAR ABONO PERIÓDICO
    // ──────────────────────────────────────────────────────────

    /**
     * Registra un abono al apartado y actualiza el saldo.
     *
     * <p>Si el saldo llega a 0 al registrar el abono, se invoca
     * {@link #completarApartadoInterno} automáticamente para generar la venta definitiva.
     *
     * @param apartadoId ID del apartado
     * @param request    Datos del abono
     * @param usuario    Usuario que registra el abono
     * @return Respuesta actualizada del apartado
     */
    @Transactional
    public ApartadoResponse registrarAbono(Long apartadoId,
                                           AbonoApartadoRequest request,
                                           Usuario usuario) {
        Apartado apartado = buscarApartado(apartadoId);

        // Validar estado
        if (apartado.getEstado() != Apartado.EstadoApartado.activo) {
            throw new ReglaNegocioException(
                    "Solo se pueden registrar abonos en apartados activos. Estado actual: "
                    + apartado.getEstado());
        }

        // Validar que no venció
        if (LocalDate.now().isAfter(apartado.getFechaLimite())) {
            throw new ReglaNegocioException(
                    "El apartado ha superado su fecha límite (" + apartado.getFechaLimite()
                    + "). Primero debe regularizarse.");
        }

        // Validar que el abono no supera el saldo pendiente
        if (request.getMonto().compareTo(apartado.getSaldoPendiente()) > 0) {
            throw new ReglaNegocioException(
                    "El abono (" + request.getMonto() + " COP) supera el saldo pendiente ("
                    + apartado.getSaldoPendiente() + " COP).");
        }

        MetodoPago metodoPago = metodoPagoRepository.findById(request.getMetodoPagoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Método de pago", request.getMetodoPagoId()));

        // Registrar el abono en el historial
        AbonoApartado abono = AbonoApartado.builder()
                .apartado(apartado)
                .metodoPago(metodoPago)
                .monto(request.getMonto())
                .esPrimerAbono(false)
                .referencia(request.getReferencia())
                .build();
        apartado.getAbonos().add(abono);

        // Actualizar totales del apartado
        apartado.registrarAbono(request.getMonto());
        apartadoRepository.save(apartado);

        log.info("Abono de {} COP registrado en apartado #{}. Saldo pendiente: {}",
                request.getMonto(), apartadoId, apartado.getSaldoPendiente());

        // Si el saldo llegó a 0, completar automáticamente
        if (apartado.getSaldoPendiente().compareTo(BigDecimal.ZERO) == 0) {
            log.info("Apartado #{} completamente pagado. Generando venta definitiva...", apartadoId);
            completarApartadoInterno(apartado, usuario);
        }

        return toResponse(apartado);
    }

    // ──────────────────────────────────────────────────────────
    // COMPLETAR APARTADO (manual o automático)
    // ──────────────────────────────────────────────────────────

    /**
     * Completa manualmente un apartado cuyo saldo ya está en cero.
     * Genera la venta definitiva y descuenta el stock.
     *
     * <p>Normalmente se llama automáticamente desde {@link #registrarAbono}.
     * Este endpoint existe por si el sistema necesita forzar el cierre.
     *
     * @param apartadoId ID del apartado
     * @param usuario    Usuario administrador que ejecuta el cierre
     * @return Respuesta con la venta definitiva generada
     */
    @Transactional
    public ApartadoResponse completarApartado(Long apartadoId, Usuario usuario) {
        Apartado apartado = buscarApartado(apartadoId);

        if (apartado.getEstado() != Apartado.EstadoApartado.activo) {
            throw new ReglaNegocioException(
                    "Solo se puede completar un apartado activo. Estado: " + apartado.getEstado());
        }
        if (apartado.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0) {
            throw new ReglaNegocioException(
                    "El apartado tiene saldo pendiente de "
                    + apartado.getSaldoPendiente() + " COP. No se puede completar.");
        }

        completarApartadoInterno(apartado, usuario);
        return toResponse(apartado);
    }

    // ──────────────────────────────────────────────────────────
    // CANCELAR APARTADO
    // ──────────────────────────────────────────────────────────

    /**
     * Cancela un apartado activo y libera el stock reservado.
     *
     * <p>NOTA: El reembolso de los abonos se rige por la política de la tienda
     * (RF-V01). Este sistema solo registra la cancelación y libera el stock.
     *
     * @param apartadoId ID del apartado
     * @param usuario    Usuario que ejecuta la cancelación
     * @param motivo     Motivo de la cancelación
     * @return Respuesta del apartado cancelado
     */
    @Transactional
    public ApartadoResponse cancelarApartado(Long apartadoId, Usuario usuario, String motivo) {
        Apartado apartado = buscarApartado(apartadoId);

        if (apartado.getEstado() == Apartado.EstadoApartado.completado) {
            throw new ReglaNegocioException("No se puede cancelar un apartado ya completado.");
        }
        if (apartado.getEstado() == Apartado.EstadoApartado.cancelado) {
            throw new ReglaNegocioException("El apartado ya está cancelado.");
        }

        // Liberar stock
        liberarStock(apartado, usuario, motivo);

        // Cambiar estado
        apartado.setEstado(Apartado.EstadoApartado.cancelado);
        apartadoRepository.save(apartado);

        log.info("Apartado #{} cancelado por usuario '{}'. Motivo: {}",
                apartadoId, usuario.getCorreo(), motivo);

        return toResponse(apartado);
    }

    // ──────────────────────────────────────────────────────────
    // VENCER APARTADO (proceso automático — llamado por scheduler)
    // ──────────────────────────────────────────────────────────

    /**
     * Tarea programada que se ejecuta todos los días a medianoche (00:00).
     * Busca apartados cuya fecha límite ha pasado y los marca como vencidos.
     * Referencia: RF-V01, Sección 3.19.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void procesarVencimientosAutomaticos() {
        log.info("Iniciando Job de vencimiento automático de apartados...");
        
        LocalDate hoy = LocalDate.now();
        java.util.List<Apartado> vencidos = apartadoRepository
                .findByEstadoAndFechaLimiteBefore(Apartado.EstadoApartado.activo, hoy);

        if (vencidos.isEmpty()) {
            log.info("No se encontraron apartados vencidos para procesar.");
            return;
        }

        // Buscamos un usuario administrador para registrar el movimiento de liberación
        Usuario sistema = usuarioRepository.findAll().stream()
                .filter(u -> u.getRol().getNombre().name().contains("admin"))
                .findFirst()
                .orElse(null);

        if (sistema == null) {
            log.error("No se pudo encontrar un usuario administrador para ejecutar el vencimiento automático.");
            return;
        }

        for (Apartado a : vencidos) {
            try {
                vencerApartado(a.getApartadoId(), sistema);
            } catch (Exception e) {
                log.error("Error al vencer el apartado #{}: {}", a.getApartadoId(), e.getMessage());
            }
        }
        
        log.info("Job de vencimiento finalizado. Procesados: {}", vencidos.size());
    }

    /**
     * Marca un apartado como vencido cuando supera la fecha límite sin completarse.
     * Libera el stock reservado.
     */
    @Transactional
    public void vencerApartado(Long apartadoId, Usuario usuario) {
        Apartado apartado = buscarApartado(apartadoId);

        if (apartado.getEstado() != Apartado.EstadoApartado.activo) return;
        if (!LocalDate.now().isAfter(apartado.getFechaLimite())) return;

        liberarStock(apartado, usuario, "Vencimiento automático — fecha límite superada");
        apartado.setEstado(Apartado.EstadoApartado.vencido);
        apartadoRepository.save(apartado);

        log.warn("Apartado #{} marcado como VENCIDO (fecha límite: {}).",
                apartadoId, apartado.getFechaLimite());
    }

    // ──────────────────────────────────────────────────────────
    // MÉTODOS INTERNOS
    // ──────────────────────────────────────────────────────────

    /**
     * Genera la venta definitiva al completar el apartado y descuenta el stock.
     *
     * <p><strong>Operaciones dentro de la transacción del llamador:</strong>
     * <ol>
     *   <li>Generar numero_factura consecutivo</li>
     *   <li>Crear Venta con estado 'confirmada', modalidad 'layaway'</li>
     *   <li>Crear DetalleVenta con precio histórico del apartado</li>
     *   <li>Descontar stock físicamente (InventarioMovimiento tipo 'salida')</li>
     *   <li>Actualizar apartado.estado = completado, apartado.venta = venta generada</li>
     * </ol>
     */
    private void completarApartadoInterno(Apartado apartado, Usuario usuario) {
        // Generar número de factura
        String anio = String.valueOf(LocalDate.now().getYear());
        long siguiente = ventaRepository.contarVentasDelAnio(anio) + 1;
        String numeroFactura = String.format("VTA-%s-%05d", anio, siguiente);

        // Calcular totales
        BigDecimal precioUnit = apartado.getPrecioUnitario();
        BigDecimal subtotal   = precioUnit.multiply(BigDecimal.valueOf(apartado.getCantidad()));

        // Crear la venta definitiva
        Venta venta = Venta.builder()
                .numeroFactura(numeroFactura)
                .cliente(apartado.getCliente())
                .usuario(usuario)
                .modalidadPago(Venta.ModalidadPago.layaway)
                .subtotal(subtotal)
                .descuentoTotal(BigDecimal.ZERO)
                .puntosFidelidadUsados(0)
                .descuentoPuntos(BigDecimal.ZERO)
                .total(subtotal)
                .puntosGanados(0) // El apartado no genera puntos hasta completar (simplificado)
                .observaciones("Venta generada al completar apartado #" + apartado.getApartadoId())
                .estadoPago(Venta.EstadoPagoVenta.pagado)
                .build();

        // Crear el detalle con precio histórico del apartado
        DetalleVenta detalle = DetalleVenta.builder()
                .producto(apartado.getProducto())
                .cantidad(apartado.getCantidad())
                .precioUnitario(precioUnit)           // Precio histórico del apartado
                .descuentoItem(BigDecimal.ZERO)
                .precioConDescuento(precioUnit)
                .subtotal(subtotal)
                .garantiaMeses(0)
                .build();

        venta.agregarDetalle(detalle);
        Venta ventaGuardada = ventaRepository.save(venta);

        // Descontar stock físicamente ahora que se entrega el producto
        Producto producto = productoLockRepository
                .findById(apartado.getProducto().getProductoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Producto", apartado.getProducto().getProductoId()));

        int stockAntes = producto.getStock();
        // NOTA: El stock ya fue descontado físicamente en registrarApartado para bloquearlo.
        // Aquí solo registramos el cambio de 'reserva' a 'salida' definitiva.

        InventarioMovimiento movimiento = InventarioMovimiento.builder()
                .producto(producto)
                .tipo(InventarioMovimiento.TipoMovimiento.salida)
                .cantidad(0) // 0 porque el descuento físico ya se hizo en la reserva
                .stockAntes(stockAntes)
                .stockDespues(producto.getStock())
                .referenciaTipo("venta")
                .referenciaId(ventaGuardada.getVentaId())
                .usuario(usuario)
                .motivo("Entrega por apartado #" + apartado.getApartadoId()
                        + " completado → Venta " + numeroFactura)
                .build();
        inventarioMovimientoRepository.save(movimiento);

        // Actualizar el apartado: estado completado y FK a la venta
        apartado.setEstado(Apartado.EstadoApartado.completado);
        apartado.setVenta(ventaGuardada);
        apartadoRepository.save(apartado);

        log.info("Apartado #{} completado. Venta definitiva: {} | Stock {} → {}",
                apartado.getApartadoId(), numeroFactura, stockAntes, producto.getStock());
    }

    /**
     * Registra un movimiento de tipo 'liberacion' en el inventario
     * cuando se cancela o vence un apartado.
     */
    private void liberarStock(Apartado apartado, Usuario usuario, String motivo) {
        Producto producto = productoLockRepository
                .findById(apartado.getProducto().getProductoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Producto", apartado.getProducto().getProductoId()));

        int stockAntes = producto.getStock();
        producto.incrementarStock(apartado.getCantidad()); // Devolvemos el stock al disponible

        InventarioMovimiento liberacion = InventarioMovimiento.builder()
                .producto(producto)
                .tipo(InventarioMovimiento.TipoMovimiento.liberacion)
                .cantidad(apartado.getCantidad()) // Positivo: se libera de vuelta
                .stockAntes(stockAntes)
                .stockDespues(producto.getStock())
                .referenciaTipo("apartado")
                .referenciaId(apartado.getApartadoId())
                .usuario(usuario)
                .motivo(motivo)
                .build();
        inventarioMovimientoRepository.save(liberacion);
        log.info("Stock liberado para producto '{}' por cancelación/vencimiento de apartado #{}",
                producto.getNombre(), apartado.getApartadoId());
    }

    /** Busca un apartado por ID o lanza 404. */
    private Apartado buscarApartado(Long apartadoId) {
        return apartadoRepository.findById(apartadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Apartado", apartadoId));
    }

    /** Mapeo de Apartado a ApartadoResponse. */
    private ApartadoResponse toResponse(Apartado a) {
        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), a.getFechaLimite());
        int porcentaje = 0;
        if (a.getTotalAPagar().compareTo(BigDecimal.ZERO) > 0) {
            porcentaje = a.getTotalAbonado()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(a.getTotalAPagar(), 0, java.math.RoundingMode.HALF_UP)
                    .intValue();
        }

        return ApartadoResponse.builder()
                .apartadoId(a.getApartadoId())
                .clienteNombre(a.getCliente().getNombre())
                .clienteDocumento(a.getCliente().getNumeroDocumento())
                .productoNombre(a.getProducto().getNombre())
                .productoSku(a.getProducto().getCodigo())
                .cantidad(a.getCantidad())
                .precioUnitario(a.getPrecioUnitario())
                .totalAPagar(a.getTotalAPagar())
                .totalAbonado(a.getTotalAbonado())
                .saldoPendiente(a.getSaldoPendiente())
                .porcentajePagado(porcentaje)
                .fechaLimite(a.getFechaLimite())
                .diasRestantes(diasRestantes)
                .estado(a.getEstado().name())
                .createdAt(a.getCreatedAt())
                .ventaDefinitivaId(a.getVenta() != null ? a.getVenta().getVentaId() : null)
                .ventaDefinitivaFactura(a.getVenta() != null ? a.getVenta().getNumeroFactura() : null)
                .build();
    }
}
