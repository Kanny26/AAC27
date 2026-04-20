package com.aac27.joyeria.shared.exception;

import com.aac27.joyeria.shared.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Manejador global de excepciones para toda la API REST.
 *
 * <p>{@code @RestControllerAdvice}: intercepta las excepciones lanzadas en
 * cualquier {@code @RestController} antes de que lleguen al cliente.
 * Convierte todas las excepciones en respuestas {@link ApiResponse} estandarizadas
 * con el código HTTP apropiado.
 *
 * <p>Beneficio: El cliente siempre recibe el mismo formato JSON sin importar
 * el tipo de error. Nunca expone stacktraces al exterior (RNF-MAN04).
 *
 * <p>Referencia: Sección 4.1 — Manejo de Excepciones, RNF-MAN05.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============================================================
    // ERRORES DE VALIDACIÓN (400 Bad Request)
    // ============================================================

    /**
     * Captura errores de validación de Bean Validation (@Valid en DTOs).
     *
     * <p>Cuando un campo del DTO no pasa la validación (@NotBlank, @Email, etc.),
     * Spring lanza {@link MethodArgumentNotValidException}.
     * Aquí la convertimos en una lista de errores legible.
     *
     * <p>Ejemplo de respuesta:
     * <pre>
     * {
     *   "success": false,
     *   "message": "Error de validación en los datos enviados",
     *   "errors": ["El correo no tiene un formato válido", "La contraseña es obligatoria"]
     * }
     * </pre>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> manejarValidacion(
            MethodArgumentNotValidException ex) {

        // Extrae los mensajes de error de cada campo que falló la validación
        List<String> errores = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage) // El mensaje viene de @NotBlank(message="...")
                .toList();

        log.warn("Error de validación: {}", errores);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error de validación en los datos enviados", errores));
    }

    // ============================================================
    // ERRORES DE NEGOCIO
    // ============================================================

    /**
     * Captura cuando no se encuentra un recurso en la BD (404).
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> manejarNoEncontrado(
            RecursoNoEncontradoException ex) {

        log.warn("Recurso no encontrado: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Captura violaciones de reglas de negocio (422).
     */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiResponse<Void>> manejarReglaNegocio(
            ReglaNegocioException ex) {

        log.warn("Regla de negocio violada: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Captura datos duplicados / conflictos (409).
     */
    @ExceptionHandler(ConflictoDatosException.class)
    public ResponseEntity<ApiResponse<Void>> manejarConflicto(
            ConflictoDatosException ex) {

        log.warn("Conflicto de datos: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ============================================================
    // ERRORES DE AUTENTICACIÓN / AUTORIZACIÓN
    // ============================================================

    /**
     * Usuario o contraseña incorrectos (401).
     * Spring Security lanza esto cuando el login falla.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> manejarCredencialesInvalidas(
            BadCredentialsException ex) {

        log.warn("Intento de login fallido: credenciales inválidas");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Correo o contraseña incorrectos"));
    }

    /**
     * Cuenta deshabilitada (usuario inactivo) → 401 (RF05).
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> manejarCuentaDeshabilitada(
            DisabledException ex) {

        log.warn("Intento de login con cuenta inactiva");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("La cuenta está inactiva. Contacte al administrador."));
    }

    /**
     * Cuenta bloqueada (403 en términos de negocio, devolvemos 401 para no exponer info).
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> manejarCuentaBloqueada(
            LockedException ex) {

        log.warn("Intento de login con cuenta bloqueada");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("La cuenta está bloqueada. Contacte al administrador."));
    }

    /**
     * Acceso denegado: el usuario está autenticado pero no tiene permiso (403).
     * Ocurre cuando @PreAuthorize rechaza la petición.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> manejarAccesoDenegado(
            AccessDeniedException ex) {

        log.warn("Acceso denegado: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tienes permisos para realizar esta acción"));
    }

    // ============================================================
    // ERROR GENÉRICO (500)
    // ============================================================

    /**
     * Captura cualquier excepción no manejada específicamente.
     * NUNCA exponemos el detalle del error interno al cliente (RNF-MAN04).
     * El detalle completo queda en los logs del servidor.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> manejarErrorGenerico(Exception ex) {

        // Loggea el error completo internamente para debugging
        log.error("Error interno no esperado: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Ocurrió un error interno. Por favor intenta más tarde."));
    }
}
