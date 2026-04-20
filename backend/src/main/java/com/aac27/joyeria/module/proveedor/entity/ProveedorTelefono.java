package com.aac27.joyeria.module.proveedor.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad JPA para la tabla {@code proveedor_telefono}.
 * Un proveedor puede tener múltiples teléfonos (Sección 3.11).
 * Al menos uno requerido (RF10 — validado en Service).
 */
@Entity
@Table(name = "proveedor_telefono")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorTelefono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "telefono_id", nullable = false, updatable = false)
    private Long telefonoId;

    /**
     * Relación inversa con Proveedor.
     * La FK {@code proveedor_id} está en ESTA tabla.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    /** true si es el contacto principal del proveedor. */
    @Column(name = "es_principal", nullable = false)
    @Builder.Default
    private boolean esPrincipal = false;
}
