package com.aac27.joyeria.module.venta.entity;

import com.aac27.joyeria.module.compra.entity.MetodoPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA para la tabla {@code pago_venta}.
 *
 * <p>
 * Registro de cada pago recibido en una venta.
 * Una venta puede tener múltiples pagos (contado en partes, anticipo, abonos a
 * crédito).
 *
 * <p>
 * Referencia: Sección 3.18 — RF17, RF-V01.
 */
@Entity
@Table(name = "pago_venta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pago_id", nullable = false, updatable = false)
    private Long pagoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "metodo_pago_id", nullable = false)
    private MetodoPago metodoPago;

    @Column(name = "monto", nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pago", nullable = false, length = 15)
    private TipoPagoVenta tipoPago;

    @Column(name = "fecha_pago", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime fechaPago;

    /** Número de referencia para transferencias/Nequi/Daviplata. */
    @Column(name = "referencia", length = 100)
    private String referenciaTransaccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    private EstadoPago estado = EstadoPago.registrado;

    @PrePersist
    protected void prePersist() {
        if (this.fechaPago == null)
            this.fechaPago = LocalDateTime.now();
    }

    /**
     * Tipo de pago (Sección 3.18):
     * - anticipo: primer pago parcial antes de recibir el bien
     * - abono: pago parcial posterior
     * - pago_completo: pago total de una vez
     * - canje_puntos: descuento aplicado como canje de puntos de fidelidad (RF-V03)
     */
    public enum TipoPagoVenta {
        anticipo, abono, pago_completo, canje_puntos
    }

    /**
     * Estado del pago (Sección 3.18).
     * Alineado con el ENUM definido en V6__ventas.sql.
     * - registrado: pago confirmado y registrado en el sistema
     * - anulado: pago anulado por error o devolución
     */
    public enum EstadoPago {
        registrado, anulado
    }
}
