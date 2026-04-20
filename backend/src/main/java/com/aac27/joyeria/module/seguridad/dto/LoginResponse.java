package com.aac27.joyeria.module.seguridad.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO de respuesta para el endpoint POST /api/v1/auth/login.
 *
 * <p>Contiene los tokens JWT y datos básicos del usuario autenticado
 * para que el frontend React los almacene y use en peticiones subsiguientes.
 *
 * <p>Flujo después del login exitoso:
 * <ol>
 *   <li>Frontend recibe este DTO</li>
 *   <li>Almacena {@code accessToken} en memoria (variable de módulo, NO localStorage)</li>
 *   <li>Almacena {@code refreshToken} en httpOnly cookie (Sección 4.2)</li>
 *   <li>Adjunta {@code "Authorization: Bearer {accessToken}"} en cada petición API</li>
 * </ol>
 *
 * <p>Referencia: RF02 — "Token JWT (exp: 8h). Refresh token (exp: 7d)."
 */
@Getter
@Builder
public class LoginResponse {

    /**
     * Token de acceso JWT (exp: 8h).
     * El frontend lo adjunta en el header Authorization de cada request.
     * Formato: "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ..."
     */
    private final String accessToken;

    /**
     * Tipo de token. Siempre "Bearer" según el estándar OAuth2/JWT.
     * El frontend construye el header: "Authorization: Bearer {accessToken}"
     */
    @Builder.Default
    private final String tokenType = "Bearer";

    /**
     * Token de refresco (exp: 7d).
     * Se usa para obtener un nuevo access token sin re-autenticarse.
     * Se almacena en httpOnly cookie para mayor seguridad.
     */
    private final String refreshToken;

    /** Milisegundos hasta que expira el access token (para que el frontend calcule cuándo refrescar). */
    private final long expiresInMs;

    /** ID del usuario autenticado. */
    private final Long usuarioId;

    /** Nombre completo del usuario (para mostrar en la UI). */
    private final String nombre;

    /** Correo del usuario. */
    private final String correo;

    /**
     * Nombre del rol asignado al usuario.
     * El frontend lo usa para mostrar/ocultar secciones del menú.
     * Valores: "superadministrador", "administrador", "vendedor"
     */
    private final String rol;

    /**
     * Indica si el usuario debe cambiar su contraseña al iniciar sesión.
     * Si es {@code true}, el frontend redirige al formulario de cambio de contraseña.
     * Referencia: RF02 — "Detectar primer acceso con clave provisional y forzar cambio."
     */
    private final boolean requiereCambioPassword;
}
