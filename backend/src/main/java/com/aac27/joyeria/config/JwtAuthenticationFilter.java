package com.aac27.joyeria.config;

import com.aac27.joyeria.module.seguridad.service.UsuarioDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT que intercepta CADA petición HTTP para verificar autenticación.
 *
 * <p>Extiende {@link OncePerRequestFilter}: garantiza que este filtro
 * se ejecuta exactamente UNA vez por petición (evita doble ejecución en redirects).
 *
 * <p><strong>Flujo de cada petición autenticada:</strong>
 * <pre>
 * 1. Request entra → JwtAuthenticationFilter.doFilterInternal()
 * 2. ¿Tiene header "Authorization: Bearer {token}"? → No → continúa sin autenticar
 * 3. ¿Tiene? → Extraemos el token del header
 * 4. ¿Token válido? (firma + expiración) → No → continúa sin autenticar (Spring devolverá 401)
 * 5. ¿Válido? → Extraemos correo del token → cargamos usuario de BD
 * 6. Creamos Authentication y lo guardamos en SecurityContextHolder
 * 7. Spring Security ahora "sabe" quién hizo la petición → permite o deniega por rol
 * </pre>
 *
 * <p>Referencia: Sección 4.1 — "JwtAuthenticationFilter como OncePerRequestFilter".
 * RNF-SEG01 — Autenticación JWT stateless.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioDetailsService usuarioDetailsService;

    /** Prefijo estándar del header de autorización JWT. */
    private static final String BEARER_PREFIX = "Bearer ";
    /** Nombre del header HTTP de autorización. */
    private static final String AUTH_HEADER = "Authorization";

    /**
     * Lógica principal del filtro. Ejecutada UNA vez por cada petición HTTP.
     *
     * @param request     Petición HTTP entrante
     * @param response    Respuesta HTTP saliente
     * @param filterChain Cadena de filtros de Spring Security
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Paso 1: Extraer el header Authorization de la petición
        final String authHeader = request.getHeader(AUTH_HEADER);

        // Paso 2: Si no tiene token o no empieza con "Bearer ", dejamos pasar sin autenticar.
        // Los endpoints públicos (/api/v1/auth/**) no necesitan token.
        // Los privados serán rechazados por SecurityFilterChain más adelante.
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Paso 3: Extraer solo el token (quitamos el prefijo "Bearer ")
        final String token = authHeader.substring(BEARER_PREFIX.length());

        // Paso 4: Validar técnicamente el token (firma y expiración)
        if (!jwtUtil.esTokenTecnicamenteValido(token)) {
            log.debug("Token inválido o expirado para la petición: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Paso 5: Extraer el correo del payload del token
        final String correo = jwtUtil.extraerCorreo(token);

        // Paso 6: Solo procesamos si hay correo Y el usuario NO está ya autenticado en el contexto.
        // La segunda condición evita re-autenticar en cada filtro de la cadena.
        if (correo != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Cargamos el usuario de BD para obtener sus authorities (roles)
            UserDetails userDetails = usuarioDetailsService.loadUserByUsername(correo);

            // Verificamos que el token pertenezca a este usuario
            if (jwtUtil.esTokenValido(token, userDetails)) {

                // Paso 7: Creamos el objeto Authentication de Spring Security.
                // UsernamePasswordAuthenticationToken(usuario, credenciales, authorities)
                // credenciales = null porque ya estamos autenticados con el token
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,   // Principal: el objeto Usuario
                                null,          // Credenciales: null (ya autenticado)
                                userDetails.getAuthorities() // Roles del usuario
                        );

                // Añadimos detalles de la petición (IP, session) al authentication
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Paso 8: Guardamos la autenticación en el SecurityContextHolder.
                // A partir de aquí, Spring sabe que la petición es del usuario `correo`.
                // @PreAuthorize y hasRole() consultan este contexto para autorizar.
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Usuario autenticado via JWT: {} para URI: {}",
                        correo, request.getRequestURI());
            }
        }

        // Paso 9: Pasamos la petición al siguiente filtro de la cadena
        filterChain.doFilter(request, response);
    }
}
