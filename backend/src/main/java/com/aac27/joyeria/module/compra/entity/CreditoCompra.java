package com.aac27.joyeria.module.compra.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidad JPA para la tabla {@code credito_compra}.
 *
 * <p>Creado automáticamente cuando {@code compra.tipoPago == CREDITO}.
 * Una compra tiene máximo un crédito activo simultáneo (UQ en BD).
 *
 * <p><strong>Transiciones de estado:</strong>
 * <ul>
 *   <li>{@code activo} → {@code pagado}: cuando {@code saldoPendiente == 0}</li>
 *   <li>{@code activo} → {@code vencido}: cuando se supera {@code fechaVencimiento}</li>
 * </ul>
 * Las transiciones se hacen en {@code CreditoCompraService} mediante un job
 * programado (Fase 4) o al registrar un abono.
 *
 * <p>Referencia: Sección 3.24 — RF-C01, RF29-A.
 */
@Entity
@Table(name = "credito_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credito_id", nullable = false, updatable = false)
    private Long creditoId;

    /**
     * Compra asociada a este crédito.
     * Relación OneToOne: una compra → un crédito máximo.
     * La UQ en BD garantiza esta restricción a nivel de datos.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false, unique = true)
    private Compra compra;

    /** Monto total del crédito (igual al total de la compra). */
    @Column(name = "monto_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal montoTotal;

    /**
     * Saldo pendiente por pagar.
     * Decrementado en cada abono. Cuando llega a 0 → estado PAGADO.
     */
    @Column(name = "saldo_pendiente", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoPendiente;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /** Fecha límite de pago. Validado en Service: debe ser > fechaInicio. */
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    @Builder.Default
    private EstadoCredito estado = EstadoCredito.activo;

    public enum EstadoCredito { activo, pagado, vencido }

    /**
     * Registra un abono al saldo pendiente.
     * Si el saldo llega a 0, cambia el estado a PAGADO automáticamente.
     *
     * @param montoAbono Monto del pago (debe ser > 0 y ≤ saldoPendiente)
     * @throws IllegalArgumentException si el abono supera el saldo pendiente
     */
    public void registrarAbono(BigDecimal montoAbono) {
        if (montoAbono.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del abono debe ser positivo");
        }
        if (montoAbono.compareTo(this.saldoPendiente) > 0) {
            throw new IllegalArgumentException(
                    "El abono (" + montoAbono + ") supera el saldo pendiente (" + saldoPendiente + ")"
            );
        }
        this.saldoPendiente = this.saldoPendiente.subtract(montoAbono);
        if (this.saldoPendiente.compareTo(BigDecimal.ZERO) == 0) {
            this.estado = EstadoCredito.pagado;
        }
    }
}
