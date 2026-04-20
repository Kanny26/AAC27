package com.aac27.joyeria.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utilidad para crear, firmar y validar JSON Web Tokens (JWT).
 *
 * <p><strong>¿Qué es un JWT?</strong>
 * Es una cadena de texto en 3 partes separadas por puntos:
 * <pre>
 *  Header.Payload.Signature
 *  eyJhbGciOiJIUzI1NiJ9  .  eyJzdWIiOiJhZG1pbiJ9  .  abc123xyz
 *       (algoritmo)              (datos del usuario)       (firma)
 * </pre>
 *
 * <p>El servidor firma el token con la clave secreta.
 * Al recibir una petición, verifica la firma para confirmar que el
 * token no fue alterado. Si la firma es válida → usuario autenticado.
 * No se necesita BD en cada petición (stateless). (RNF-SEG01)
 *
 * <p>Referencia: RF02, RNF-SEG01.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    /** Propiedades JWT leídas de application.yml (clave secreta, tiempos de expiración). */
    private final JwtProperties jwtProperties;

    // ============================================================
    // GENERACIÓN DE TOKENS
    // ============================================================

    /**
     * Genera un access token JWT para el usuario autenticado.
     *
     * <p>El token contiene en su payload (claims):
     * <ul>
     *   <li>{@code sub}: correo del usuario (identificador único)</li>
     *   <li>{@code rol}: nombre del rol para autorización en frontend</li>
     *   <li>{@code tipo}: "access" para distinguirlo del refresh token</li>
     *   <li>{@code iat}: fecha de emisión (issued at)</li>
     *   <li>{@code exp}: fecha de expiración (8 horas desde emisión)</li>
     * </ul>
     *
     * @param userDetails Objeto UserDetails del usuario autenticado (nuestra entidad Usuario)
     * @return Token JWT firmado como String
     */
    public String generarAccessToken(UserDetails userDetails) {
        // Claims adicionales que incluimos en el payload del token
        Map<String, Object> claimsExtra = new HashMap<>();
        claimsExtra.put("tipo", "access");

        // Extraemos el rol del authority del usuario para incluirlo en el token
        // El frontend lo usa para controlar la visibilidad de secciones
        userDetails.getAuthorities().stream()
                .findFirst()
                .ifPresent(auth -> {
                    // Quitamos el prefijo "ROLE_" para enviar solo "administrador"
                    String rolSinPrefijo = auth.getAuthority().replace("ROLE_", "");
                    claimsExtra.put("rol", rolSinPrefijo);
                });

        return construirToken(claimsExtra, userDetails.getUsername(), jwtProperties.getExpirationMs());
    }

    /**
     * Genera un refresh token JWT para el usuario.
     *
     * <p>El refresh token contiene menos información que el access token.
     * Solo se usa para obtener un nuevo access token cuando el anterior expira.
     * Expira en 7 días (RF02).
     *
     * @param userDetails Objeto UserDetails del usuario
     * @return Refresh token JWT firmado
     */
    public String generarRefreshToken(UserDetails userDetails) {
        Map<String, Object> claimsExtra = new HashMap<>();
        claimsExtra.put("tipo", "refresh");

        return construirToken(claimsExtra, userDetails.getUsername(), jwtProperties.getRefreshExpirationMs());
    }

    /**
     * Método interno que construye y firma el token JWT.
     *
     * <p>Usa HMAC-SHA256 (HS256) para la firma — el algoritmo más común para JWT.
     * La clave secreta debe tener al menos 256 bits (32 caracteres).
     *
     * @param claimsExtra Claims adicionales a incluir en el payload
     * @param subject     Identificador del usuario (correo electrónico)
     * @param expiracionMs Tiempo en milisegundos hasta la expiración
     * @return Token JWT firmado
     */
    private String construirToken(Map<String, Object> claimsExtra, String subject, long expiracionMs) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiracionMs);

        return Jwts.builder()
                .claims(claimsExtra)        // Claims extra (rol, tipo)
                .subject(subject)           // "sub": correo del usuario
                .issuedAt(ahora)           // "iat": cuándo se emitió
                .expiration(expiracion)    // "exp": cuándo expira
                .signWith(obtenerClaveSecreta()) // Firma con HMAC-SHA256
                .compact();                // Construye el String final
    }

    // ============================================================
    // VALIDACIÓN DE TOKENS
    // ============================================================

    /**
     * Valida que un token JWT sea válido para un usuario específico.
     *
     * <p>Verifica:
     * <ol>
     *   <li>La firma del token (no fue alterado)</li>
     *   <li>El token no ha expirado</li>
     *   <li>El correo del token coincide con el usuario esperado</li>
     * </ol>
     *
     * @param token       Token JWT a validar
     * @param userDetails Datos del usuario contra quien validar
     * @return {@code true} si el token es válido
     */
    public boolean esTokenValido(String token, UserDetails userDetails) {
        try {
            final String correo = extraerCorreo(token);
            return correo.equals(userDetails.getUsername()) && !estaExpirado(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Valida que el token es técnicamente válido (firma y expiración),
     * sin verificar contra un usuario específico.
     * Usado en el filtro JWT para una validación rápida.
     *
     * @param token Token JWT a validar
     * @return {@code true} si la firma es correcta y el token no expiró
     */
    public boolean esTokenTecnicamenteValido(String token) {
        try {
            parsearToken(token); // Lanza excepción si el token es inválido
            return !estaExpirado(token);
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expirado");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT malformado o con firma inválida: {}", e.getMessage());
            return false;
        }
    }

    // ============================================================
    // EXTRACCIÓN DE DATOS DEL TOKEN
    // ============================================================

    /**
     * Extrae el correo del usuario (campo "sub") del token.
     *
     * @param token Token JWT
     * @return Correo electrónico del usuario
     */
    public String extraerCorreo(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    /**
     * Extrae la fecha de expiración del token.
     *
     * @param token Token JWT
     * @return Fecha de expiración
     */
    public Date extraerExpiracion(String token) {
        return extraerClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae el rol del usuario del payload del token.
     *
     * @param token Token JWT
     * @return Nombre del rol (ej: "administrador") o null si no está presente
     */
    public String extraerRol(String token) {
        return extraerClaim(token, claims -> claims.get("rol", String.class));
    }

    /**
     * Extrae un claim específico del token usando una función.
     *
     * <p>Patrón funcional: recibe una función que extrae el claim deseado.
     * Evita repetir el código de parseo en cada método de extracción.
     *
     * @param token          Token JWT
     * @param resolucionClaim Función que extrae el claim deseado del Claims
     * @param <T>             Tipo del claim a extraer
     * @return Valor del claim
     */
    public <T> T extraerClaim(String token, Function<Claims, T> resolucionClaim) {
        final Claims claims = parsearToken(token);
        return resolucionClaim.apply(claims);
    }

    // ============================================================
    // MÉTODOS PRIVADOS DE SOPORTE
    // ============================================================

    /**
     * Parsea el token JWT y extrae todos sus claims.
     *
     * <p>Si el token tiene firma inválida, está malformado o expirado,
     * lanza una {@link JwtException} que el llamador debe manejar.
     *
     * @param token Token JWT a parsear
     * @return Claims extraídos del payload
     */
    private Claims parsearToken(String token) {
        return Jwts.parser()
                .verifyWith(obtenerClaveSecreta()) // Verifica la firma
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Verifica si el token ya expiró comparando la fecha de expiración con la actual.
     *
     * @param token Token JWT
     * @return {@code true} si el token ya expiró
     */
    private boolean estaExpirado(String token) {
        return extraerExpiracion(token).before(new Date());
    }

    /**
     * Convierte la clave secreta (String del application.yml) en un objeto
     * {@link SecretKey} que JJWT puede usar para firmar/verificar.
     *
     * <p>Se usa HMAC-SHA256 (HS256). La clave debe tener al menos 32 caracteres
     * para cumplir con el requisito de 256 bits mínimos del estándar JWT.
     *
     * @return Clave secreta para firmar/verificar tokens
     */
    private SecretKey obtenerClaveSecreta() {
        byte[] claveBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(claveBytes);
    }
}
