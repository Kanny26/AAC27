package com.aac27.joyeria.module.proveedor.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad JPA para la tabla {@code proveedor_correo}.
 * Un proveedor puede tener múltiples correos (Sección 3.12).
 * Al menos uno requerido (RF10 — validado en Service).
 * El correo es ÚNICO globalmente en la tabla.
 */
@Entity
@Table(name = "proveedor_correo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorCorreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "correo_id", nullable = false, updatable = false)
    private Long correoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(name = "correo", nullable = false, unique = true, length = 255)
    private String correo;

    @Column(name = "es_principal", nullable = false)
    @Builder.Default
    private boolean esPrincipal = false;
}
