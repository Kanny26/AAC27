package com.aac27.joyeria.module.seguridad.service;

import com.aac27.joyeria.module.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación personalizada de {@link UserDetailsService} de Spring
 * Security.
 *
 * <p>
 * <strong>¿Para qué sirve?</strong>
 * Spring Security, al recibir una petición de login, necesita cargar el usuario
 * desde algún origen de datos. Ese origen se define implementando esta
 * interfaz.
 * Al registrar este bean, Spring Security lo usará automáticamente.
 *
 * <p>
 * <strong>Flujo de autenticación:</strong>
 * <ol>
 * <li>POST /api/v1/auth/login recibe {correo, password}</li>
 * <li>Spring Security llama a {@code loadUserByUsername(correo)}</li>
 * <li>Este método carga el {@code Usuario} desde MySQL</li>
 * <li>Spring compara el password con BCrypt automáticamente</li>
 * <li>Si coincide → autenticado → generamos JWT</li>
 * </ol>
 *
 * <p>
 * Por qué {@code @Transactional}: la query {@code findByCorreoWithRol}
 * hace un JOIN FETCH con el rol. JPA necesita una transacción activa.
 *
 * <p>
 * Referencia: Sección 4.1 — Seguridad Spring Security.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Carga un usuario por su correo electrónico desde la base de datos.
     *
     * <p>
     * IMPORTANTE: En nuestro sistema, el "username" de Spring Security
     * es el CORREO ELECTRÓNICO (no un nombre de usuario), porque el correo
     * es único e inmutable (RF08).
     *
     * <p>
     * El {@code JOIN FETCH u.rol} en la query asegura que el rol se cargue
     * en la misma consulta SQL, evitando {@code LazyInitializationException}
     * cuando Spring Security llame a {@code getAuthorities()}.
     *
     * @param correo Correo electrónico del usuario (usado como username)
     * @return Objeto {@code UserDetails} (nuestra entidad {@code Usuario})
     * @throws UsernameNotFoundException si no existe un usuario con ese correo
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        log.debug("Cargando usuario por correo: {}", correo);

        return usuarioRepository.findByCorreoWithRol(correo)
                .orElseThrow(() -> {
                    // IMPORTANTE: No revelar si el correo existe o no (seguridad)
                    // El mensaje genérico evita que un atacante enumere correos válidos
                    log.warn("Intento de acceso con correo no registrado: {}", correo);
                    return new UsernameNotFoundException(
                            "No existe un usuario con el correo: " + correo);
                });
    }
}
