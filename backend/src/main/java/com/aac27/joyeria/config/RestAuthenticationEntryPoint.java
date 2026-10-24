package com.aac27.joyeria.config;

import com.aac27.joyeria.shared.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Se ejecuta cuando una petición SIN autenticación válida llega a un
 * endpoint protegido. Responde 401 con el mismo formato JSON (ApiResponse)
 * que usa el resto de la API, para que el frontend lo trate siempre igual.
 */
@Component
@RequiredArgsConstructor

public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Indica al cliente qué esquema de autenticación se espera
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");

        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.error("Autenticación requerida: token ausente, inválido o expirado"));
    }
}