package com.aac27.joyeria.module.venta.entity;

import com.aac27.joyeria.module.compra.entity.MetodoPago;
import com.aac27.joyeria.module.catalogo.entity.Producto;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA para la tabla {@code apartado}.
 *
 * <p>
 * <strong>Flujo del Layaway/Apartado:</strong>
 * <ol>
 * <li>Cliente paga primer abono ≥ 20% del total</li>
 * <li>Stock se RESERVA (InventarioMovimiento tipo 'reserva') — NO se
 * descuenta</li>
 * <li>Cliente realiza abonos periódicos hasta completar</li>
 * <li>Al completar → se crea la {@link Venta} definitiva
 * → stock se descuenta (InventarioMovimiento tipo 'salida')</li>
 * <li>Si cancela → stock se libera (InventarioMovimiento tipo
 * 'liberacion')</li>
 * </ol>
 *
 * <p>
 * Referencia: Sección 3.19 — RF-V01.
 */
@Entity
@Table(name = "apartado", indexes = {
        @Index(name = "idx_apartado_cliente", columnList = "cliente_id"),
        @Index(name = "idx_apartado_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Apartado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "apartado_id", nullable = false, updatable = false)
    private Long apartadoId;

    /**
     * Siempre requerido — no se puede hacer apartado a cliente anónimo (RF-V01).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    /**
     * Precio fijado en el momento del apartado. INMUTABLE.
     * El cliente paga este precio aunque el producto suba de precio.
     */
    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "total_a_pagar", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAPagar;

    @Column(name = "total_abonado", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalAbonado = BigDecimal.ZERO;

    @Column(name = "saldo_pendiente", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoPendiente;

    /** Fecha máxima para completar el pago. Validado: debe ser > hoy. */
    @Column(name = "fecha_limite", nullable = false)
    private LocalDate fechaLimite;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    private EstadoApartado estado = EstadoApartado.activo;

    /**
     * La venta definitiva generada al completar el apartado.
     * NULL mientras el apartado está activo.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @Column(name = "primer_abono_ok", nullable = false)
    @Builder.Default
    private boolean primerAbonoOk = false;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "apartado", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<AbonoApartado> abonos = new ArrayList<>();

    @PrePersist
    protected void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.createdAt == null)
            this.createdAt = ahora;
        this.updatedAt = ahora;
        if (this.saldoPendiente == null)
            this.saldoPendiente = this.totalAPagar;
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Estado del apartado. 'vencido' cuando se supera fecha_limite sin completar el
     * pago.
     */
    public enum EstadoApartado {
        activo, completado, cancelado, vencido
    }

    // ── MÉTODOS DE NEGOCIO ─────────────────────────────────────

    /**
     * Registra un abono y actualiza saldo pendiente.
     * Si saldo llega a 0 → cambia estado a COMPLETADO.
     *
     * @param montoAbono Monto del abono (> 0 y ≤ saldo_pendiente)
     */
    public void registrarAbono(BigDecimal montoAbono) {
        if (montoAbono.compareTo(this.saldoPendiente) > 0) {
            throw new IllegalArgumentException(
                    "El abono (" + montoAbono + ") supera el saldo pendiente (" + saldoPendiente + ")");
        }
        this.totalAbonado = this.totalAbonado.add(montoAbono);
        this.saldoPendiente = this.saldoPendiente.subtract(montoAbono);

        if (this.saldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
            this.estado = EstadoApartado.completado;
        }
    }

    /** Calcula si el primer abono cumple el mínimo del 20% (RF-V01). */
    public boolean cumpleMinimoPrimerAbono(BigDecimal primerAbono) {
        BigDecimal minimoRequerido = totalAPagar.multiply(BigDecimal.valueOf(0.20));
        return primerAbono.compareTo(minimoRequerido) >= 0;
    }
}
