package com.aac27.joyeria.config;

import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.shared.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * RF02: mientras el usuario tenga la contraseña provisional, solo puede consultar su
 * perfil y cambiarla. Todo lo demás responde 403.
 *
 * <p>Va justo después de {@link JwtAuthenticationFilter}, que ya cargó al usuario desde la
 * BD en esta misma petición: por eso, apenas cambia la contraseña, el mismo token deja de
 * estar restringido sin necesidad de volver a iniciar sesión.
 *
 * <p>Es una lista blanca (método + ruta exacta): lo que no esté aquí queda bloqueado, así
 * que cualquier variante extraña de ruta o método falla cerrada.
 *
 * <p>No es un {@code @Component} a propósito: se instancia en {@link SecurityConfig} para
 * que Spring Boot no lo registre además como filtro global del contenedor.
 */
@Slf4j
public class CambioPasswordObligatorioFilter extends OncePerRequestFilter {

    /** Código estable para que el frontend distinga este 403 de uno por falta de rol. */
    public static final String CODIGO = "PASSWORD_CHANGE_REQUIRED";

    private static final Set<String> PERMITIDAS = Set.of(
            "GET /api/v1/auth/me",
            "POST /api/v1/auth/cambiar-password",
            "POST /api/v1/auth/login",
            "POST /api/v1/auth/refresh-token"
    );

    private final ObjectMapper objectMapper;

    public CambioPasswordObligatorioFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean debeCambiarla = auth != null
                && auth.getPrincipal() instanceof Usuario usuario
                && usuario.isEsPasswordTemporal();

        String peticion = request.getMethod() + " " + request.getRequestURI();

        if (debeCambiarla && !PERMITIDAS.contains(peticion)) {
            log.warn("Petición bloqueada: el usuario aún no cambia su contraseña provisional ({})", peticion);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(
                    "Debes cambiar tu contraseña provisional antes de continuar",
                    List.of(CODIGO)));
            return;
        }

        filterChain.doFilter(request, response);
    }
}