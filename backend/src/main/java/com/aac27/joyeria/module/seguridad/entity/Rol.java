package com.aac27.joyeria.module.seguridad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA que representa la tabla {@code rol} de la base de datos.
 *
 * <p>Los roles son inmutables: no se crean ni eliminan en runtime,
 * solo se asignan a usuarios. Los 3 roles se insertan con Flyway en datos semilla.
 *
 * <p>Referencia: Sección 3.2 del documento de arquitectura y RF01.
 * <br>Regla de negocio: "Los roles no pueden eliminarse para garantizar trazabilidad" (RF01).
 */
@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
public class Rol {

    /**
     * Clave primaria autoincremental.
     * {@code GenerationType.IDENTITY}: usa el AUTO_INCREMENT de MySQL.
     * Tipo Long equivale a BIGINT UNSIGNED en MySQL (Sección 3.2).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rol_id", nullable = false, updatable = false)
    private Long rolId;

    /**
     * Nombre del rol.
     * Se mapea como ENUM en MySQL y String en Java.
     * {@code @Enumerated(EnumType.STRING)}: guarda el nombre del enum en texto,
     * no el ordinal (0, 1, 2), para que sea legible en la BD.
     *
     * <p>Valores posibles: superadministrador, administrador, vendedor.
     * Referencia: Sección 3.2
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private NombreRol nombre;

    /**
     * Descripción opcional del rol para documentación interna.
     */
    @Column(name = "descripcion", length = 255)
    private String descripcion;

    /**
     * Enum interno que representa los posibles valores del campo nombre.
     *
     * <p>Usar un enum interno de la entidad es una práctica recomendada
     * porque acopla el enum directamente a la entidad donde se usa.
     * Los valores coinciden EXACTAMENTE con los del ENUM en MySQL.
     */
    public enum NombreRol {
        /** Acceso técnico total. No se asigna a usuarios normales. */
        superadministrador,
        /** Acceso completo a todas las funcionalidades del negocio. */
        administrador,
        /** Acceso limitado a ventas y consulta de datos propios. */
        vendedor
    }
}
