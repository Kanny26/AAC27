package com.aac27.joyeria.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades de JWT leídas automáticamente desde application.yml.
 *
 * <p>{@code @ConfigurationProperties(prefix = "jwt")}: Spring lee todos los campos
 * bajo la clave "jwt:" en application.yml y los inyecta en esta clase.
 *
 * <p>Ejemplo en application.yml:
 * <pre>
 * jwt:
 *   secret: mi-clave-secreta
 *   expiration-ms: 28800000
 *   refresh-expiration-ms: 604800000
 * </pre>
 *
 * <p>Beneficio: Las propiedades están tipadas (long, String) en lugar de
 * usar @Value("${jwt.secret}") en cada clase. Más limpio y testeable.
 *
 * <p>{@code @Validated}: Spring comprueba estas restricciones al ARRANCAR. Un secreto
 * ausente o demasiado corto detiene la aplicación de inmediato, en lugar de fallar
 * en el primer login con un error difícil de interpretar.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * Clave secreta para firmar los tokens JWT con HMAC-SHA256.
     * Debe tener al menos 256 bits (32 caracteres).
     * Viene de variables de entorno en producción (RNF-INF02).
     */
    @NotBlank(message = "jwt.secret es obligatorio (defínelo con la variable JWT_SECRET)")
    @Size(min = 32, message = "jwt.secret debe tener al menos 32 caracteres (256 bits)")
    private String secret;

    /**
     * Tiempo de expiración del access token en milisegundos.
     * Por defecto: 8 horas = 28,800,000 ms (RF02).
     */
    @Positive(message = "jwt.expiration-ms debe ser mayor que cero")
    private long expirationMs;

    /**
     * Tiempo de expiración del refresh token en milisegundos.
     * Por defecto: 7 días = 604,800,000 ms (RF02).
     */
    @Positive(message = "jwt.refresh-expiration-ms debe ser mayor que cero")
    private long refreshExpirationMs;
}
