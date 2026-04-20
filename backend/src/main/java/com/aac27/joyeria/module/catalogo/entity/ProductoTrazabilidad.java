package com.aac27.joyeria.module.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA para la tabla {@code producto_trazabilidad}.
 *
 * <p>Datos de trazabilidad para metales preciosos. Solo aplica a productos
 * con {@code material.esTrazable == true} (RF-CAT02).
 *
 * <p>Relación: ONE producto : ONE trazabilidad.
 * La FK {@code producto_id} está en ESTA tabla (lado "many" de la relación 1:1).
 *
 * <p>Referencia: Sección 3.8 — RF-CAT02.
 */
@Entity
@Table(name = "producto_trazabilidad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoTrazabilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trazabilidad_id", nullable = false, updatable = false)
    private Long trazabilidadId;

    /**
     * Relación inversa con Producto.
     * {@code @OneToOne}: esta entidad es el lado "dueño" de la relación
     * porque tiene la FK {@code producto_id} en su tabla.
     * {@code @JoinColumn}: define la columna FK.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false, unique = true)
    private Producto producto;

    /**
     * Ley del metal (para plata y oro).
     * Ejemplos: "925" (plata), "750" (oro 18K), "585" (oro 14K).
     */
    @Column(name = "ley", length = 20)
    private String ley;

    /**
     * Quilates del oro.
     * Ejemplos: 18.00 (18K), 14.00 (14K), 10.00 (10K).
     * NULL para plata y otros materiales.
     */
    @Column(name = "quilates", precision = 5, scale = 2)
    private BigDecimal quilates;

    /** Número de certificado de procedencia del metal o piedra. */
    @Column(name = "certificado_procedencia", length = 255)
    private String certificadoProcedencia;

    /**
     * Descripción de piedras preciosas.
     * Formato libre: tipo + quilates + certificado por cada piedra.
     * Ejemplo: "Diamante 0.5ct GIA-123456; Esmeralda 0.3ct COL-789"
     */
    @Column(name = "piedras_descripcion", columnDefinition = "TEXT")
    private String piedrasDescripcion;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
