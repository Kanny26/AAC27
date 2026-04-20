package com.aac27.joyeria.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de CORS (Cross-Origin Resource Sharing).
 *
 * <p><strong>¿Por qué necesitamos CORS?</strong>
 * El navegador bloquea por seguridad cualquier petición que venga de un
 * origen distinto al del servidor. Como el frontend React corre en
 * {@code localhost:5173} y la API en {@code localhost:8080}, son orígenes
 * distintos → el navegador bloquearía las peticiones sin esta configuración.
 *
 * <p>CORS permite al servidor indicar explícitamente qué orígenes, métodos
 * y headers están permitidos.
 *
 * <p>Los orígenes se leen de application.yml según el perfil activo:
 * <ul>
 *   <li>Dev: http://localhost:5173, http://localhost:3000</li>
 *   <li>Prod: https://aac27.com (configurado por variable de entorno)</li>
 * </ul>
 *
 * <p>Referencia: RNF-SEG04 — "Headers CORS configurados explícitamente por entorno."
 */
@Configuration
public class CorsConfig {

    /**
     * Lista de orígenes permitidos leída de application.yml.
     * La anotación @Value inyecta el valor de la propiedad "cors.allowed-origins"
     * definida en el perfil activo (dev o prod).
     */
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    /**
     * Define la política CORS para todos los endpoints de la API.
     *
     * <p>Esta configuración le dice al navegador:
     * "Estos orígenes pueden hacer peticiones a mi API con estos métodos y headers."
     *
     * @return Bean de configuración CORS usado por Spring Security y el DispatcherServlet
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos (frontend React en dev y prod)
        config.setAllowedOrigins(allowedOrigins);

        // Métodos HTTP permitidos (todos los que usamos en la API REST)
        // Sección 4.3: GET, POST, PUT, PATCH, DELETE
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers que el frontend puede enviar
        // Authorization: el token JWT
        // Content-Type: application/json
        // X-Requested-With: para identificar peticiones AJAX
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        ));

        // Headers que el navegador puede leer de la respuesta
        config.setExposedHeaders(List.of("Authorization"));

        // Permite enviar cookies en peticiones cross-origin
        // Necesario para el refresh token en httpOnly cookie (Sección 4.2)
        config.setAllowCredentials(true);

        // Tiempo en segundos que el navegador cachea la respuesta preflight (OPTIONS)
        // 1 hora = 3600s. Reduce las peticiones OPTIONS repetidas.
        config.setMaxAge(3600L);

        // Aplicamos esta configuración a TODOS los endpoints (/api/v1/**)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
