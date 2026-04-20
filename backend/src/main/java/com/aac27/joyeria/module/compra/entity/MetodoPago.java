package com.aac27.joyeria.module.compra.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad JPA para la tabla {@code metodo_pago}.
 *
 * <p>Métodos de pago disponibles: Efectivo, Transferencia, Nequi, etc.
 * No se elimina si tiene transacciones asociadas — solo se inactiva (RF28).
 *
 * <p>Referencia: Sección 3.23 — RF28.
 */
@Entity
@Table(name = "metodo_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metodo_pago_id", nullable = false, updatable = false)
    private Long metodoPagoId;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    @Builder.Default
    private EstadoMetodo estado = EstadoMetodo.activo;

    public enum EstadoMetodo { activo, inactivo }
}
