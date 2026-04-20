package com.aac27.joyeria.module.seguridad.service;

import com.aac27.joyeria.config.JwtProperties;
import com.aac27.joyeria.config.JwtUtil;
import com.aac27.joyeria.module.seguridad.dto.LoginRequest;
import com.aac27.joyeria.module.seguridad.dto.LoginResponse;
import com.aac27.joyeria.module.seguridad.dto.RegistrarUsuarioRequest;
import com.aac27.joyeria.module.seguridad.entity.Rol;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.module.seguridad.repository.RolRepository;
import com.aac27.joyeria.module.seguridad.repository.UsuarioRepository;
import com.aac27.joyeria.shared.exception.ConflictoDatosException;
import com.aac27.joyeria.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio de autenticación y gestión de usuarios del sistema.
 *
 * <p>Contiene toda la lógica de negocio relacionada con:
 * <ul>
 *   <li>Login con validación de credenciales</li>
 *   <li>Generación de tokens JWT (access + refresh)</li>
 *   <li>Registro de nuevos usuarios con contraseña provisional</li>
 *   <li>Renovación de access token con refresh token</li>
 * </ul>
 *
 * <p><strong>Flujo de Login paso a paso:</strong>
 * <pre>
 * 1. Cliente → POST /api/v1/auth/login {correo, password}
 * 2. AuthController → authService.login(request)
 * 3. AuthService → authenticationManager.authenticate(correo, password)
 * 4. AuthenticationManager → UsuarioDetailsService.loadUserByUsername(correo)
 * 5. UsuarioDetailsService → usuarioRepository.findByCorreoWithRol(correo)
 * 6. Spring compara password con BCrypt hash de la BD
 * 7. Si coincide → generamos JWT → actualizamos ultimo_acceso → retornamos LoginResponse
 * </pre>
 *
 * <p>Referencia: RF02, RF06, RNF-SEG01 a RNF-SEG03.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioDetailsService usuarioDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    // ============================================================
    // LOGIN
    // ============================================================

    /**
     * Autentica al usuario y genera tokens JWT.
     *
     * <p>El {@link AuthenticationManager} orquesta todo el proceso:
     * carga el usuario, verifica la contraseña con BCrypt, y valida
     * que la cuenta esté activa ({@code isEnabled()}).
     *
     * <p>Si la autenticación falla (contraseña incorrecta, cuenta inactiva,
     * usuario no encontrado), Spring lanza automáticamente la excepción
     * apropiada que nuestro {@code GlobalExceptionHandler} convierte en
     * una respuesta 401 estandarizada.
     *
     * @param request DTO con correo y contraseña
     * @return DTO con tokens JWT y datos del usuario autenticado
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Intento de login para: {}", request.getCorreo());

        // authenticate() hace todo:
        // 1. Llama a loadUserByUsername(correo) → busca en BD
        // 2. Compara la contraseña con BCrypt
        // 3. Verifica que isEnabled() y isAccountNonLocked() sean true
        // Si algo falla → lanza BadCredentialsException, DisabledException, etc.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getCorreo(),
                        request.getPassword()
                )
        );

        // Si llegamos aquí, la autenticación fue exitosa.
        // Cargamos el usuario completo (con rol) para generar el token.
        Usuario usuario = (Usuario) usuarioDetailsService.loadUserByUsername(request.getCorreo());

        // Generamos ambos tokens
        String accessToken = jwtUtil.generarAccessToken(usuario);
        String refreshToken = jwtUtil.generarRefreshToken(usuario);

        // Actualizamos la fecha de último acceso en BD (RF02)
        usuarioRepository.actualizarUltimoAcceso(usuario.getUsuarioId(), LocalDateTime.now());

        log.info("Login exitoso para usuario ID: {}, rol: {}",
                usuario.getUsuarioId(), usuario.getRol().getNombre());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInMs(jwtProperties.getExpirationMs())
                .usuarioId(usuario.getUsuarioId())
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol().getNombre().name())
                .requiereCambioPassword(usuario.isEsPasswordTemporal())
                .build();
    }

    // ============================================================
    // REFRESH TOKEN
    // ============================================================

    /**
     * Renueva el access token usando un refresh token válido.
     *
     * <p>El frontend envía el refresh token cuando el access token expira.
     * No se requiere re-autenticación con contraseña.
     *
     * @param refreshToken Token de refresco JWT
     * @return Nuevo access token
     * @throws RecursoNoEncontradoException si el correo del token no existe en BD
     * @throws IllegalArgumentException si el refresh token es inválido o expiró
     */
    @Transactional(readOnly = true)
    public LoginResponse refrescarToken(String refreshToken) {
        // Validamos que el refresh token sea técnicamente correcto (firma y expiración)
        if (!jwtUtil.esTokenTecnicamenteValido(refreshToken)) {
            throw new IllegalArgumentException("El refresh token es inválido o ha expirado");
        }

        // Extraemos el correo del token y cargamos el usuario
        String correo = jwtUtil.extraerCorreo(refreshToken);
        Usuario usuario = (Usuario) usuarioDetailsService.loadUserByUsername(correo);

        // Verificamos que el token pertenezca a este usuario y sea válido
        if (!jwtUtil.esTokenValido(refreshToken, usuario)) {
            throw new IllegalArgumentException("El refresh token no es válido para este usuario");
        }

        // Generamos únicamente un nuevo access token (el refresh token no se renueva aquí)
        String nuevoAccessToken = jwtUtil.generarAccessToken(usuario);

        log.info("Access token renovado para usuario: {}", correo);

        return LoginResponse.builder()
                .accessToken(nuevoAccessToken)
                .refreshToken(refreshToken) // Devolvemos el mismo refresh token
                .expiresInMs(jwtProperties.getExpirationMs())
                .usuarioId(usuario.getUsuarioId())
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                .rol(usuario.getRol().getNombre().name())
                .requiereCambioPassword(usuario.isEsPasswordTemporal())
                .build();
    }

    // ============================================================
    // REGISTRO DE USUARIOS (RF06)
    // ============================================================

    /**
     * Registra un nuevo usuario en el sistema con contraseña provisional.
     *
     * <p>La contraseña provisional se genera automáticamente con UUID
     * y se cifra con BCrypt antes de guardar (RNF-SEG03).
     *
     * <p>En producción, la contraseña provisional se enviaría por correo
     * (RF06: "Contraseña provisional generada con UUID + envío por correo").
     * Aquí la retornamos en el log solo con fines de desarrollo.
     *
     * @param request DTO con datos del nuevo usuario
     * @return Usuario creado
     * @throws ConflictoDatosException si el correo o documento ya existen
     * @throws RecursoNoEncontradoException si el rol solicitado no existe en BD
     */
    @Transactional
    public Usuario registrarUsuario(RegistrarUsuarioRequest request) {
        log.info("Registrando nuevo usuario con correo: {}", request.getCorreo());

        // Validación de unicidad: correo
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new ConflictoDatosException(
                    "Ya existe un usuario con el correo: " + request.getCorreo()
            );
        }

        // Validación de unicidad: número de documento
        if (usuarioRepository.existsByNumeroDocumento(request.getNumeroDocumento())) {
            throw new ConflictoDatosException(
                    "Ya existe un usuario con el documento: " + request.getNumeroDocumento()
            );
        }

        // Buscamos el rol en BD
        Rol.NombreRol nombreRol = Rol.NombreRol.valueOf(request.getRol());
        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Rol no encontrado: " + request.getRol()
                ));

        // Generamos contraseña provisional aleatoria (RF06)
        // UUID garantiza unicidad y aleatoriedad suficiente
        String passwordProvisional = UUID.randomUUID().toString().substring(0, 12);
        String passwordHash = passwordEncoder.encode(passwordProvisional);

        // SOLO para desarrollo: mostrar la contraseña en logs
        // En producción esto se enviaría por correo y NUNCA se loggearía
        log.info("🔑 [DEV ONLY] Contraseña provisional para {}: {}", request.getCorreo(), passwordProvisional);

        // Construimos el usuario con el patrón Builder de Lombok
        Usuario nuevoUsuario = Usuario.builder()
                .nombre(request.getNombre())
                .numeroDocumento(request.getNumeroDocumento())
                .correo(request.getCorreo())
                .telefono(request.getTelefono())
                .passwordHash(passwordHash)
                .esPasswordTemporal(true) // RF06: contraseña provisional = true
                .estado(Usuario.EstadoUsuario.activo)
                .rol(rol)
                .build();

        Usuario guardado = usuarioRepository.save(nuevoUsuario);
        log.info("Usuario creado exitosamente con ID: {}", guardado.getUsuarioId());

        return guardado;
    }
}
