package com.aac27.joyeria.module.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad JPA para la tabla {@code material}.
 *
 * <p>Materiales de joyería: Plata Ley 950, Oro Amarillo, Covergold, etc.
 * El campo {@code esTrazable} determina si el producto debe tener datos
 * de trazabilidad (ley, quilates, certificado de procedencia) — RF-CAT02.
 *
 * <p>Referencia: Sección 3.6 — RF14.
 */
@Entity
@Table(name = "material")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id", nullable = false, updatable = false)
    private Long materialId;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    /**
     * Indica si el material requiere datos de trazabilidad.
     * {@code true} para metales preciosos (Plata, Oro).
     * {@code false} para materiales sin valor intrínseco (Covergold, Fantasía).
     * Activa el flujo de certificación (RF-CAT02).
     */
    @Column(name = "es_trazable", nullable = false)
    @Builder.Default
    private boolean esTrazable = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    @Builder.Default
    private EstadoMaterial estado = EstadoMaterial.activo;

    public enum EstadoMaterial { activo, inactivo }
}
