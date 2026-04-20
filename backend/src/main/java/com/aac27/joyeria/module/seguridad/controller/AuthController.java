package com.aac27.joyeria.module.seguridad.controller;

import com.aac27.joyeria.module.seguridad.dto.LoginRequest;
import com.aac27.joyeria.module.seguridad.dto.LoginResponse;
import com.aac27.joyeria.module.seguridad.dto.RegistrarUsuarioRequest;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.module.seguridad.service.AuthService;
import com.aac27.joyeria.shared.constant.RolConstants;
import com.aac27.joyeria.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para autenticación y gestión básica de usuarios.
 *
 * <p>Base URL: {@code /api/v1/auth}
 * <br>Endpoints públicos: login, refresh-token
 * <br>Endpoints protegidos: registro (solo Administrador)
 *
 * <p>Reglas del controlador (Sección 4.1):
 * <ul>
 *   <li>SOLO recibe peticiones HTTP y delega al Service</li>
 *   <li>NO contiene lógica de negocio</li>
 *   <li>Valida DTOs con {@code @Valid} antes de pasarlos al Service</li>
 *   <li>Retorna siempre {@link ApiResponse} envuelto en {@link ResponseEntity}</li>
 * </ul>
 *
 * <p>Referencia: RF02, RF04, RF06, Sección 4.1 y 4.3.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ============================================================
    // POST /api/v1/auth/login
    // ============================================================

    /**
     * Autentica al usuario y devuelve tokens JWT.
     *
     * <p>Endpoint PÚBLICO: no requiere token previo.
     *
     * <p>Flujo:
     * <ol>
     *   <li>{@code @Valid} valida el DTO (correo formato, password no vacío)</li>
     *   <li>Si validación falla → {@code GlobalExceptionHandler} devuelve 400</li>
     *   <li>Si credenciales incorrectas → {@code GlobalExceptionHandler} devuelve 401</li>
     *   <li>Si cuenta inactiva → {@code GlobalExceptionHandler} devuelve 401</li>
     *   <li>Éxito → 200 con access token, refresh token y datos del usuario</li>
     * </ol>
     *
     * <p>Ejemplo de request:
     * <pre>
     * POST /api/v1/auth/login
     * Content-Type: application/json
     *
     * { "correo": "admin@aac27.com", "password": "MiPassword123" }
     * </pre>
     *
     * @param request DTO con correo y contraseña (validado con @Valid)
     * @return 200 OK con LoginResponse envuelto en ApiResponse
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Petición de login recibida para: {}", request.getCorreo());

        LoginResponse respuesta = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.ok(respuesta, "Inicio de sesión exitoso")
        );
    }

    // ============================================================
    // POST /api/v1/auth/refresh-token
    // ============================================================

    /**
     * Renueva el access token usando un refresh token válido.
     *
     * <p>Endpoint PÚBLICO: el access token ya expiró, por eso se llama aquí.
     * El frontend envía el refresh token en el body.
     *
     * <p>Ejemplo de request:
     * <pre>
     * POST /api/v1/auth/refresh-token
     * Content-Type: application/json
     *
     * { "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
     * </pre>
     *
     * @param body Map con el campo "refreshToken"
     * @return 200 OK con nuevo access token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refrescarToken(
            @RequestBody Map<String, String> body) {

        String refreshToken = body.get("refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("El refresh token es requerido"));
        }

        LoginResponse respuesta = authService.refrescarToken(refreshToken);

        return ResponseEntity.ok(
                ApiResponse.ok(respuesta, "Token renovado exitosamente")
        );
    }

    // ============================================================
    // POST /api/v1/auth/registro
    // ============================================================

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * <p>Endpoint PROTEGIDO: solo {@code administrador} puede crear usuarios (RF06).
     *
     * <p>{@code @PreAuthorize}: evaluado ANTES de ejecutar el método.
     * Si el token del usuario no tiene el rol "administrador" → 403 Forbidden.
     * Spring Security lanza {@code AccessDeniedException} que
     * {@code GlobalExceptionHandler} convierte en respuesta 403 estandarizada.
     *
     * <p>Ejemplo de request:
     * <pre>
     * POST /api/v1/auth/registro
     * Authorization: Bearer {token_administrador}
     * Content-Type: application/json
     *
     * {
     *   "nombre": "María García",
     *   "numeroDocumento": "1234567890",
     *   "correo": "maria@aac27.com",
     *   "telefono": "3001234567",
     *   "rol": "vendedor"
     * }
     * </pre>
     *
     * @param request DTO con datos del nuevo usuario
     * @return 201 Created con ID y correo del usuario creado
     */
    @PostMapping("/registro")
    @PreAuthorize("hasRole('" + RolConstants.ADMINISTRADOR + "')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registrarUsuario(
            @Valid @RequestBody RegistrarUsuarioRequest request) {

        log.info("Petición de registro para: {}", request.getCorreo());

        Usuario usuario = authService.registrarUsuario(request);

        // Solo devolvemos los datos no sensibles. Nunca el password. (RNF-SEG03)
        Map<String, Object> datos = Map.of(
                "usuarioId", usuario.getUsuarioId(),
                "nombre", usuario.getNombre(),
                "correo", usuario.getCorreo(),
                "rol", usuario.getRol().getNombre().name(),
                "estado", usuario.getEstado().name()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(datos, "Usuario registrado exitosamente. Se ha generado una contraseña provisional."));
    }

    // ============================================================
    // GET /api/v1/auth/me
    // ============================================================

    /**
     * Devuelve los datos del usuario actualmente autenticado.
     *
     * <p>Endpoint PROTEGIDO: cualquier usuario autenticado puede consultar sus propios datos.
     * Spring Security inyecta el {@code Usuario} autenticado a través del
     * {@code SecurityContextHolder} que nuestro filtro JWT llenó.
     *
     * <p>Útil para que el frontend valide el token al recargar la página
     * y obtenga los datos actualizados del usuario.
     *
     * @param usuarioAutenticado El objeto Usuario autenticado (inyectado por Spring)
     * @return 200 OK con datos básicos del usuario
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerPerfil(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            Usuario usuarioAutenticado) {

        Map<String, Object> datos = Map.of(
                "usuarioId", usuarioAutenticado.getUsuarioId(),
                "nombre", usuarioAutenticado.getNombre(),
                "correo", usuarioAutenticado.getCorreo(),
                "telefono", usuarioAutenticado.getTelefono() != null
                        ? usuarioAutenticado.getTelefono() : "",
                "rol", usuarioAutenticado.getRol().getNombre().name(),
                "estado", usuarioAutenticado.getEstado().name(),
                "fotoPerfilUrl", usuarioAutenticado.getFotoPerfilUrl() != null
                        ? usuarioAutenticado.getFotoPerfilUrl() : "",
                "requiereCambioPassword", usuarioAutenticado.isEsPasswordTemporal()
        );

        return ResponseEntity.ok(ApiResponse.ok(datos, "Perfil obtenido exitosamente"));
    }
}
