package com.aac27.joyeria.config;

import com.aac27.joyeria.module.seguridad.service.UsuarioDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

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
                        // Solo login y renovación son públicos; /me y /registro exigen token
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh-token").permitAll()
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
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))
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
     *   <li>Solo entonces comprueba que la cuenta esté activa y no bloqueada</li>
     * </ol>
     *
     * <p><strong>Por qué el estado de la cuenta se comprueba DESPUÉS de la contraseña:</strong>
     * por defecto Spring Security lo comprueba ANTES. Eso permitiría a cualquiera,
     * conociendo solo un correo, saber si la cuenta está inactiva (el mensaje sería distinto
     * al de "credenciales incorrectas") sin necesitar la contraseña. Así, quien no demuestra
     * la contraseña recibe siempre el mismo mensaje genérico.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Antes de la contraseña: no se comprueba nada
        provider.setPreAuthenticationChecks(user -> { });
        // Después de la contraseña: bloqueada, deshabilitada, expirada o credenciales vencidas
        provider.setPostAuthenticationChecks(new AccountStatusUserDetailsChecker());
        return provider;
    }

    /**
     * Expone el {@link AuthenticationManager} como bean de Spring.
     *
     * <p>El AuthenticationManager es el orquestador del proceso de autenticación.
     * Lo necesitamos en {@code AuthService.login()} para delegar la verificación
     * de credenciales a Spring Security.
     *
     * <p>Se construye explícitamente con nuestro {@link #authenticationProvider()} para que
     * el login use SIEMPRE la política de comprobaciones definida arriba, sin depender de
     * cómo Spring Boot autoconfigure el manager por su cuenta.
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider());
    }
}
