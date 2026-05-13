package com.aac27.joyeria.module.venta.entity;

import com.aac27.joyeria.module.compra.entity.MetodoPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA para la tabla {@code abono_apartado}.
 * Historial de pagos realizados al apartado.
 *
 * <p>Referencia: Sección 3.19 — RF-V01.
 */
@Entity
@Table(name = "abono_apartado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonoApartado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "abono_id", nullable = false, updatable = false)
    private Long abonoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartado_id", nullable = false)
    private Apartado apartado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "metodo_pago_id", nullable = false)
    private MetodoPago metodoPago;

    @Column(name = "monto", nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    /** true solo para el primer abono (debe ser ≥ 20% del total). */
    @Column(name = "es_primer_abono", nullable = false)
    @Builder.Default
    private boolean esPrimerAbono = false;

    @Column(name = "referencia", length = 100)
    private String referencia;

    @Column(name = "fecha_abono", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime fechaAbono;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @Builder.Default
    private EstadoAbono estado = EstadoAbono.registrado;

    @PrePersist
    protected void prePersist() {
        if (this.fechaAbono == null) this.fechaAbono = LocalDateTime.now();
    }

    public enum EstadoAbono { registrado, anulado }
}
