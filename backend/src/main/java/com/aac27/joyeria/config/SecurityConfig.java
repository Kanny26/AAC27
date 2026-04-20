package com.aac27.joyeria.config;

import com.aac27.joyeria.module.seguridad.service.UsuarioDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración central de Spring Security.
 *
 * <p>Define:
 * <ul>
 *   <li>Qué endpoints son públicos (sin token) y cuáles requieren autenticación</li>
 *   <li>Política de sesión: STATELESS (JWT, sin HttpSession en servidor)</li>
 *   <li>BCrypt como encoder de contraseñas (cost factor 12)</li>
 *   <li>AuthenticationProvider: conecta el UserDetailsService con el encoder</li>
 *   <li>Posición del JwtAuthenticationFilter en la cadena de filtros</li>
 * </ul>
 *
 * <p>{@code @EnableWebSecurity}: activa la configuración de seguridad web de Spring.
 * <p>{@code @EnableMethodSecurity}: activa {@code @PreAuthorize} en servicios y controllers
 *    para control de acceso basado en roles (RBAC), requerido por RNF-SEG02.
 *
 * <p>Referencia: Sección 4.1, RNF-SEG01 a RNF-SEG04.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Activa @PreAuthorize y @PostAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UsuarioDetailsService usuarioDetailsService;

    // ============================================================
    // SECURITY FILTER CHAIN
    // ============================================================

    /**
     * Define las reglas de acceso a los endpoints de la API.
     *
     * <p>Leer como: "Para las peticiones HTTP..."
     * <ul>
     *   <li>CSRF desactivado: usamos JWT (stateless), no cookies de sesión</li>
     *   <li>CORS configurado en {@link CorsConfig}</li>
     *   <li>Sesión STATELESS: Spring no crea ni usa HttpSession (JWT maneja el estado)</li>
     *   <li>/api/v1/auth/**: permitido sin token (login, registro)</li>
     *   <li>/actuator/health: permitido sin token (para healthcheck de Docker)</li>
     *   <li>Cualquier otra ruta: requiere token válido</li>
     * </ul>
     *
     * @param http Builder de configuración HTTP de Spring Security
     * @return Cadena de filtros configurada
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desactivar CSRF: con JWT stateless no hay vulnerabilidad CSRF
                // porque no usamos cookies de sesión (RNF-SEG04)
                .csrf(AbstractHttpConfigurer::disable)

                // CORS: usa el bean CorsConfigurationSource definido en CorsConfig
                .cors(cors -> cors.configure(http))

                // Reglas de autorización por endpoint
                .authorizeHttpRequests(auth -> auth

                        // ── ENDPOINTS PÚBLICOS (no requieren token) ──────────────
                        // Login y refresh token: el usuario no tiene token aún
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // Swagger UI: documentación de la API (en desarrollo)
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Actuator health: para Docker healthcheck sin autenticación
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Preflight CORS: el navegador envía OPTIONS antes de la petición real
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ── TODOS LOS DEMÁS ENDPOINTS REQUIEREN TOKEN ─────────────
                        // La autorización granular (por rol) se hace con @PreAuthorize
                        // en los métodos de servicio o controlador
                        .anyRequest().authenticated()
                )

                // Política STATELESS: Spring NO crea ni mantiene HttpSession
                // Cada petición debe venir con su propio token JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Registramos nuestro AuthenticationProvider personalizado
                .authenticationProvider(authenticationProvider())

                // Insertamos el filtro JWT ANTES del filtro de autenticación por usuario/contraseña
                // Spring Security evalúa los filtros en orden; el nuestro va primero
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ============================================================
    // BEANS DE SEGURIDAD
    // ============================================================

    /**
     * Codificador de contraseñas BCrypt con cost factor 12.
     *
     * <p>BCrypt genera automáticamente un salt aleatorio y lo incluye
     * en el hash resultante. El mismo password cifrado dos veces produce
     * hashes DISTINTOS (por el salt), lo que imposibilita ataques con tablas rainbow.
     *
     * <p>Cost factor 12: cada incremento duplica el tiempo de cómputo.
     * 12 es el mínimo recomendado para producción (RNF-SEG03).
     * En un servidor moderno, cifrar una contraseña tarda ~300ms con cost 12.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Proveedor de autenticación que conecta:
     * - {@code UsuarioDetailsService}: cómo cargar el usuario desde BD
     * - {@code PasswordEncoder}: cómo comparar la contraseña enviada con el BCrypt hash
     *
     * <p>El {@link DaoAuthenticationProvider} es la implementación estándar de Spring
     * para autenticación con base de datos (DAO = Data Access Object).
     *
     * <p>Cuando llamamos a {@code authenticationManager.authenticate(correo, password)},
     * este provider:
     * <ol>
     *   <li>Llama a {@code usuarioDetailsService.loadUserByUsername(correo)}</li>
     *   <li>Llama a {@code passwordEncoder.matches(password, passwordHash)}</li>
     *   <li>Si coincide → autenticado. Si no → BadCredentialsException</li>
     * </ol>
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expone el {@link AuthenticationManager} como bean de Spring.
     *
     * <p>El AuthenticationManager es el orquestador del proceso de autenticación.
     * Lo necesitamos en {@code AuthService.login()} para delegar la verificación
     * de credenciales a Spring Security.
     *
     * <p>Spring Boot lo configura automáticamente; aquí solo lo exponemos
     * para poder inyectarlo donde lo necesitemos.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
