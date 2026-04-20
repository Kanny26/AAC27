package com.aac27.joyeria.module.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA para la tabla {@code categoria}.
 *
 * <p>
 * Categorías de productos: Anillos, Collares, Pulseras, etc.
 * No tiene {@code updated_at} en la BD, solo {@code created_at},
 * por eso NO extiende {@link com.aac27.joyeria.shared.audit.BaseAuditEntity}.
 *
 * <p>
 * Referencia: Sección 3.4 — RF12.
 * <br>
 * Regla: "No se puede desactivar categoría con productos activos" (RF12).
 */
@Entity
@Table(name = "categoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "categoria_id", nullable = false, updatable = false)
    private Long categoriaId;

    /**
     * Nombre único de la categoría.
     * Validado en Service: no se repite (RF12: "Nombre único").
     */
    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    /** URL del ícono representativo (emoji o imagen SVG). */
    @Column(name = "icono_url", length = 500)
    private String iconoUrl;

    /**
     * Estado. No se desactiva si tiene productos activos (RF12).
     * El borrado lógico se maneja en Service, no en BD.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    @Builder.Default
    private EstadoCategoria estado = EstadoCategoria.activo;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public enum EstadoCategoria {
        activo, inactivo
    }
}
