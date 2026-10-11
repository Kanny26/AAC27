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
import org.springframework.security.core.userdetails.UsernameNotFoundException;

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

        final String authHeader = request.getHeader(AUTH_HEADER);

        // Sin cabecera "Bearer ...": se sigue como anónimo
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(BEARER_PREFIX.length());

        // Solo se aceptan tokens de ACCESO: un refresh token no autentica peticiones
        if (!jwtUtil.esTokenTecnicamenteValido(token, JwtUtil.TIPO_ACCESS)) {
            log.debug("Token inválido, expirado o de tipo incorrecto: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        final String correo = jwtUtil.extraerCorreo(token);

        if (correo != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = usuarioDetailsService.loadUserByUsername(correo);

                // Revalida contra la BD: usuario activo y no bloqueado (RF05)
                if (jwtUtil.esTokenValido(token, userDetails, JwtUtil.TIPO_ACCESS)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Usuario autenticado via JWT: {} para URI: {}",
                            correo, request.getRequestURI());
                } else {
                    log.warn("Token rechazado: usuario inactivo, bloqueado o token no corresponde");
                }
            } catch (UsernameNotFoundException e) {
                // El usuario del token ya no existe: se trata como anónimo, no como error 500
                log.warn("Token válido de un usuario que ya no existe");
            }
        }

        filterChain.doFilter(request, response);
    }
}
