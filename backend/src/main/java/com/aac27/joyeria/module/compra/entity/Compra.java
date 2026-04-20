package com.aac27.joyeria.module.compra.entity;

import com.aac27.joyeria.module.proveedor.entity.Proveedor;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA para la tabla {@code compra}.
 *
 * <p>Cabecera de la orden de compra a un proveedor.
 *
 * <p><strong>Regla crítica (RF11):</strong>
 * "Una vez guardada la compra no se puede modificar (integridad contable)."
 * Esta regla se hace cumplir en {@code CompraService}, que verifica el estado
 * antes de cualquier operación de edición.
 *
 * <p><strong>Flujo al guardar una compra:</strong>
 * <ol>
 *   <li>Se crea la {@code Compra} con sus {@code DetalleCompra}</li>
 *   <li>Se actualiza el stock de cada producto (optimistic locking)</li>
 *   <li>Se registra un {@code InventarioMovimiento} por cada producto</li>
 *   <li>Si tipo_pago = 'credito', se crea un {@code CreditoCompra}</li>
 * </ol>
 *
 * <p>Todo en UNA transacción {@code @Transactional} (atomicidad).
 *
 * <p>Referencia: Sección 3.14 — RF11, RF-C01, RF-C03.
 */
@Entity
@Table(
        name = "compra",
        indexes = {
                @Index(name = "idx_compra_proveedor", columnList = "proveedor_id"),
                @Index(name = "idx_compra_fecha",     columnList = "fecha_factura")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "compra_id", nullable = false, updatable = false)
    private Long compraId;

    /** Proveedor al que se le compra. Debe estar ACTIVO (RF11). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    /** Administrador que registró la compra. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Fecha de la factura del proveedor. No puede ser futura. */
    @Column(name = "fecha_factura", nullable = false)
    private LocalDate fechaFactura;

    /** Fecha comprometida de entrega por el proveedor. */
    @Column(name = "fecha_entrega_esperada")
    private LocalDate fechaEntregaEsperada;

    /** Fecha real en que se recibió el pedido completo. */
    @Column(name = "fecha_recepcion_real")
    private LocalDate fechaRecepcionReal;

    /**
     * Método de pago utilizado en esta compra.
     * Referencia a la tabla {@code metodo_pago}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "metodo_pago_id", nullable = false)
    private MetodoPago metodoPago;

    /**
     * Tipo de pago: contado (pago inmediato) o crédito (pago a futuro).
     * Si es 'credito', se crea automáticamente un {@link CreditoCompra}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false, length = 10)
    private TipoPago tipoPago;

    /** Suma de los subtotales de cada ítem. Calculado en Service. */
    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** Total de la orden (subtotal + impuestos si aplica). Calculado en Service. */
    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 25)
    @Builder.Default
    private EstadoCompra estado = EstadoCompra.pendiente;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    /**
     * Líneas de detalle de esta compra.
     *
     * <p>{@code CascadeType.PERSIST, MERGE}: al guardar la compra,
     * se guardan automáticamente los detalles.
     * NO usamos CascadeType.REMOVE porque las compras son inmutables (RF11).
     */
    @OneToMany(mappedBy = "compra", cascade = {CascadeType.PERSIST, CascadeType.MERGE},
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetalleCompra> detalles = new ArrayList<>();

    @PrePersist
    protected void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public enum TipoPago { contado, credito }

    public enum EstadoCompra {
        pendiente,
        recibido_parcial,
        recibido_completo,
        cancelada
    }

    /** Agrega un detalle y calcula en tiempo real los totales de la compra. */
    public void agregarDetalle(DetalleCompra detalle) {
        detalle.setCompra(this);
        this.detalles.add(detalle);
        recalcularTotales();
    }

    /** Recalcula subtotal y total sumando todos los detalles. */
    public void recalcularTotales() {
        this.subtotal = detalles.stream()
                .map(DetalleCompra::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = this.subtotal; // En v1 total = subtotal (sin impuestos adicionales)
    }
}
