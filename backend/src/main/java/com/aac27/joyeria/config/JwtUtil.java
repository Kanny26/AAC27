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
 * Crea, firma y valida JSON Web Tokens (JWT).
 *
 * <p>Cada token lleva un claim "tipo" ("access" o "refresh") que indica para qué
 * sirve. Quien lo use DEBE exigir el tipo correcto; si no, un refresh token
 * (7 días) podría usarse como access token (8 horas).
 *
 * <p>Referencia: RF02, RF05, RNF-SEG01.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    /** Nombre del claim que indica el propósito del token. */
    public static final String CLAIM_TIPO = "tipo";
    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    // ============================================================
    // GENERACIÓN DE TOKENS
    // ============================================================

    public String generarAccessToken(UserDetails userDetails) {
        Map<String, Object> claimsExtra = new HashMap<>();
        claimsExtra.put(CLAIM_TIPO, TIPO_ACCESS);
        userDetails.getAuthorities().stream()
                .findFirst()
                .ifPresent(auth -> claimsExtra.put("rol", auth.getAuthority().replace("ROLE_", "")));
        return construirToken(claimsExtra, userDetails.getUsername(), jwtProperties.getExpirationMs());
    }

    public String generarRefreshToken(UserDetails userDetails) {
        Map<String, Object> claimsExtra = new HashMap<>();
        claimsExtra.put(CLAIM_TIPO, TIPO_REFRESH);
        return construirToken(claimsExtra, userDetails.getUsername(), jwtProperties.getRefreshExpirationMs());
    }

    private String construirToken(Map<String, Object> claimsExtra, String subject, long expiracionMs) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiracionMs);
        return Jwts.builder()
                .claims(claimsExtra)
                .subject(subject)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(obtenerClaveSecreta())
                .compact();
    }

    // ============================================================
    // VALIDACIÓN DE TOKENS
    // ============================================================

    /**
     * Validación técnica: firma correcta, no expirado y del tipo esperado.
     * No consulta la base de datos.
     */
    public boolean esTokenTecnicamenteValido(String token, String tipoEsperado) {
        try {
            Claims claims = parsearToken(token); // lanza excepción si firma o expiración fallan
            return tipoEsperado.equals(claims.get(CLAIM_TIPO, String.class));
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expirado");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT malformado o con firma inválida: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validación completa contra el usuario cargado de la BD (RF05):
     * tipo esperado, mismo correo, cuenta activa y no bloqueada.
     * Así, desactivar a un usuario invalida sus tokens en la siguiente petición.
     */
    public boolean esTokenValido(String token, UserDetails userDetails, String tipoEsperado) {
        try {
            Claims claims = parsearToken(token);
            return tipoEsperado.equals(claims.get(CLAIM_TIPO, String.class))
                    && userDetails.getUsername().equals(claims.getSubject())
                    && userDetails.isEnabled()
                    && userDetails.isAccountNonLocked();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return false;
        }
    }

    // ============================================================
    // EXTRACCIÓN DE DATOS
    // ============================================================

    public String extraerCorreo(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public Date extraerExpiracion(String token) {
        return extraerClaim(token, Claims::getExpiration);
    }

    public String extraerRol(String token) {
        return extraerClaim(token, claims -> claims.get("rol", String.class));
    }

    public <T> T extraerClaim(String token, Function<Claims, T> resolucionClaim) {
        return resolucionClaim.apply(parsearToken(token));
    }

    /** Verifica la firma Y la expiración; lanza excepción si algo falla. */
    private Claims parsearToken(String token) {
        return Jwts.parser()
                .verifyWith(obtenerClaveSecreta())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey obtenerClaveSecreta() {
        byte[] claveBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(claveBytes);
    }
}