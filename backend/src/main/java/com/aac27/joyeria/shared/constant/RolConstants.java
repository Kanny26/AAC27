package com.aac27.joyeria.shared.constant;

/**
 * Constantes de nombres de rol del sistema.
 *
 * <p>Centraliza los nombres exactos de los roles tal como están
 * definidos en la tabla {@code rol} de la base de datos.
 * Usar estas constantes en lugar de Strings literales evita errores
 * de tipeo en las anotaciones @PreAuthorize.
 *
 * <p>Ejemplo de uso en un controlador:
 * <pre>
 * {@code @PreAuthorize("hasRole('" + RolConstants.ADMINISTRADOR + "')")}
 * </pre>
 *
 * <p>Referencia: Sección 3.2 — Tabla rol (RF01).
 */
public final class RolConstants {

    // Constructor privado: clase de utilidad, no se instancia.
    private RolConstants() {}

    /** Rol técnico con acceso total al sistema. No se asigna a usuarios normales. */
    public static final String SUPERADMINISTRADOR = "superadministrador";

    /** Rol con acceso completo a todas las funcionalidades del negocio (RF01). */
    public static final String ADMINISTRADOR = "administrador";

    /** Rol con acceso limitado: creación de ventas y consulta de sus propios datos (RF01). */
    public static final String VENDEDOR = "vendedor";

    /**
     * Prefijo requerido por Spring Security para los roles.
     * Spring Security espera el formato "ROLE_nombre" en los authorities.
     * Ejemplo: "ROLE_administrador"
     */
    public static final String ROLE_PREFIX = "ROLE_";
}
