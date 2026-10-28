package com.aac27.joyeria.config;

import com.aac27.joyeria.AbstractMySqlTest;
import com.aac27.joyeria.module.seguridad.entity.Rol;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.module.seguridad.repository.RolRepository;
import com.aac27.joyeria.module.seguridad.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base para pruebas del flujo de autenticación contra la base MySQL de pruebas.
 *
 * <p>Las clases hijas deben llevar {@code @SpringBootTest}, {@code @AutoConfigureMockMvc} y
 * {@code @Transactional}. Con {@code @Transactional} todo lo que se cree aquí se revierte al
 * terminar cada prueba, así que no se ensucia la base y no se depende del usuario administrador
 * que siembra la migración V1.
 */
abstract class AbstractAuthTest extends AbstractMySqlTest {

    /** Contraseña en claro de todos los usuarios de prueba (cumple el mínimo de 6 caracteres). */
    protected static final String PASSWORD = "Clave12345";

    /** Garantiza correos y documentos únicos dentro de una misma ejecución. */
    private static final AtomicLong CONTADOR = new AtomicLong(1);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected RolRepository rolRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtUtil jwtUtil;

    /** Crea y persiste un usuario real (con contraseña BCrypt de {@link #PASSWORD}). */
    protected Usuario crearUsuario(Rol.NombreRol nombreRol, Usuario.EstadoUsuario estado) {
        long n = CONTADOR.getAndIncrement();
        Rol rol = rolRepository.findByNombre(nombreRol).orElseThrow();

        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombre("Usuario de prueba " + n)
                .numeroDocumento(String.format("9%09d", n))
                .correo("prueba" + n + "@aac27-test.com")
                .telefono("3001234567")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .esPasswordTemporal(false)
                .estado(estado)
                .rol(rol)
                .build());
    }

    /** Cuerpo JSON de {@code POST /api/v1/auth/login}. */
    protected String cuerpoLogin(String correo, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("correo", correo, "password", password));
    }

    /** Hace login con la contraseña correcta, exige 200 y devuelve el nodo {@code data}. */
    protected JsonNode loginExitoso(Usuario usuario) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(usuario.getCorreo(), PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(resultado.getResponse().getContentAsString()).path("data");
    }

    /** Hace login y devuelve solo el access token. */
    protected String accessTokenDe(Usuario usuario) throws Exception {
        return loginExitoso(usuario).path("accessToken").asText();
    }

    /** Valor del header {@code Authorization} para un token. */
    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Cuerpo JSON de {@code POST /api/v1/auth/registro}. */
    protected String cuerpoRegistro(String correo, String numeroDocumento, String rol) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "nombre", "Maria Garcia",
                "numeroDocumento", numeroDocumento,
                "correo", correo,
                "telefono", "3001234567",
                "rol", rol));
    }
}
