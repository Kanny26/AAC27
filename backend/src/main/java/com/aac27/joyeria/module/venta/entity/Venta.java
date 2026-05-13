package com.aac27.joyeria.module.venta.entity;

import com.aac27.joyeria.module.compra.entity.MetodoPago;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA para la tabla {@code venta}.
 *
 * <p><strong>Flujo de registro de venta (VentaService):</strong>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  @Transactional — TODO en UNA transacción                          │
 * │                                                                     │
 * │  1. Validar stock disponible (no permitir venta sin stock)          │
 * │  2. Aplicar descuentos y promociones → calcular totales             │
 * │  3. Aplicar puntos de fidelidad (canje) si el cliente los tiene     │
 * │  4. Generar numero_factura consecutivo (bloqueado con LOCK)         │
 * │  5. Guardar Venta + DetalleVenta en cascade                         │
 * │  6. Por cada ítem: Producto.decrementarStock() con @Version         │
 * │     → Si OptimisticLockException → Spring Retry (3 intentos)       │
 * │  7. Crear InventarioMovimiento tipo 'salida' por ítem               │
 * │  8. Registrar PagoVenta                                             │
 * │  9. Actualizar puntos_fidelidad del cliente (ganados - usados)      │
 * │ 10. Si crédito → crear CreditoVenta                                 │
 * └─────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>Referencia: Sección 3.16 — RF17-RF21, RF-V03.
 */
@Entity
@Table(
        name = "venta",
        indexes = {
                @Index(name = "idx_venta_cliente",       columnList = "cliente_id"),
                @Index(name = "idx_venta_fecha",         columnList = "fecha_venta"),
                @Index(name = "idx_venta_usuario_fecha", columnList = "usuario_id, fecha_venta")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "venta_id", nullable = false, updatable = false)
    private Long ventaId;

    /**
     * Número de factura único y consecutivo por año.
     * Formato: VTA-2026-00001 → VTA-2026-00002 → ...
     * Generado en VentaService con SELECT para obtener el último número y bloqueo.
     * Inmutable tras creación.
     */
    @Column(name = "numero_factura", nullable = false, unique = true,
            length = 20, updatable = false)
    private String numeroFactura;

    /**
     * Cliente de la venta. Puede ser NULL para ventas a cliente ocasional (RF17).
     * Si es NULL, los puntos de fidelidad no aplican.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Vendedor que registra la venta. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_venta", nullable = false,
            columnDefinition = "DATETIME")
    private LocalDateTime fechaVenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_pago", nullable = false, length = 10)
    private ModalidadPago modalidadPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    private EstadoVenta estado = EstadoVenta.confirmada;

    /**
     * Estado del pago (Sección 3.16): si la venta está pagada, pendiente o vencida.
     * Se actualiza al registrar pagos en pago_venta.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 10)
    @Builder.Default
    private EstadoPagoVenta estadoPago = EstadoPagoVenta.pendiente;

    // ── MONTOS ─────────────────────────────────────────────────

    /** Suma de (cantidad × precio_unitario) sin descuentos. */
    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Suma de descuentos por promociones. */
    @Column(name = "descuento_total", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal descuentoTotal = BigDecimal.ZERO;

    /**
     * Puntos canjeados en ESTA venta.
     * 1 punto = 100 COP de descuento (RF-V03).
     * Se descuentan del saldo del cliente al confirmar.
     */
    @Column(name = "puntos_fidelidad_usados", nullable = false)
    @Builder.Default
    private Integer puntosFidelidadUsados = 0;

    /** Valor en pesos del descuento por puntos. */
    @Column(name = "descuento_puntos", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal descuentoPuntos = BigDecimal.ZERO;

    /**
     * Total final: subtotal - descuentoTotal - descuentoPuntos.
     * Nunca negativo (validado en Service).
     */
    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    /**
     * Puntos ganados en esta compra.
     * 1 punto por cada 10,000 COP en total (RF-V03).
     * Se suman al saldo de puntos del cliente.
     */
    @Column(name = "puntos_ganados", nullable = false)
    @Builder.Default
    private Integer puntosGanados = 0;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    // ── COLECCIONES ────────────────────────────────────────────

    @OneToMany(mappedBy = "venta", cascade = {CascadeType.PERSIST, CascadeType.MERGE},
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetalleVenta> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "venta", cascade = {CascadeType.PERSIST, CascadeType.MERGE},
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<PagoVenta> pagos = new ArrayList<>();

    @PrePersist
    protected void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.fechaVenta == null) this.fechaVenta = LocalDateTime.now();
    }

    public enum ModalidadPago { contado, anticipo, credito, layaway }
    public enum EstadoVenta   { pendiente, confirmada, entregada, cancelada }
    /** Estado del pago de la venta: pagado completo, pendiente de pago o vencido. */
    public enum EstadoPagoVenta { pagado, pendiente, vencido }

    // ── MÉTODOS DE NEGOCIO ─────────────────────────────────────

    public void agregarDetalle(DetalleVenta detalle) {
        detalle.setVenta(this);
        this.detalles.add(detalle);
    }

    public void agregarPago(PagoVenta pago) {
        pago.setVenta(this);
        this.pagos.add(pago);
    }

    /**
     * Recalcula los totales de la venta a partir de sus detalles.
     * Llamado después de agregar todos los ítems.
     */
    public void recalcularTotales() {
        this.subtotal = detalles.stream()
                .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.descuentoTotal = detalles.stream()
                .map(DetalleVenta::getDescuentoItem)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSinPuntos = subtotal.subtract(descuentoTotal);
        this.total = totalSinPuntos.subtract(descuentoPuntos).max(BigDecimal.ZERO);

        // RF-V03: 1 punto por cada 10,000 COP
        this.puntosGanados = this.total.divide(BigDecimal.valueOf(10_000),
                0, java.math.RoundingMode.DOWN).intValue();
    }
}
