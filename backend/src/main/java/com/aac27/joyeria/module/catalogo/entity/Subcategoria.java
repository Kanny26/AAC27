package com.aac27.joyeria.module.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA para la tabla {@code subcategoria}.
 *
 * <p>
 * Subcategorías: 15 años, Matrimonio, Uso Diario, etc.
 * Relacionadas con {@link Producto} mediante tabla N:M
 * ({@code producto_subcategoria}).
 *
 * <p>
 * Referencia: Sección 3.5 — RF13.
 */
@Entity
@Table(name = "subcategoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subcategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subcategoria_id", nullable = false, updatable = false)
    private Long subcategoriaId;

    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    @Builder.Default
    private EstadoSubcategoria estado = EstadoSubcategoria.activo;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public enum EstadoSubcategoria {
        activo, inactivo
    }
}
