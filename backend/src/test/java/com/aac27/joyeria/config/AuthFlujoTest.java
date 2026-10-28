package com.aac27.joyeria.config;

import com.aac27.joyeria.module.seguridad.entity.Rol;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-07: flujo de autenticación con usuarios REALES en la base (casos de éxito y de fallo).
 *
 * <p>Complementa a {@link AuthSecurityTest}, que cubre sobre todo rutas sin token y tokens
 * malformados. Aquí se prueba lo que exige el roadmap: login válido devuelve JWT, login
 * inválido devuelve 401, un token expirado o inválido no autentica, y los permisos por rol.
 *
 * <p>{@code @Transactional}: cada prueba se revierte al terminar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlujoTest extends AbstractAuthTest {

    private static final String OTRA_CLAVE = "otra-clave-distinta-para-pruebas-aac27-0123456789";

    @Autowired
    private JwtProperties jwtProperties;

    /** JwtUtil con otro secreto o con otra expiración, para fabricar tokens "malos". */
    private JwtUtil jwtUtilCon(String secreto, long expiracionMs) {
        JwtProperties props = new JwtProperties();
        props.setSecret(secreto);
        props.setExpirationMs(expiracionMs);
        props.setRefreshExpirationMs(expiracionMs);
        return new JwtUtil(props);
    }

    // ============================================================
    // LOGIN
    // ============================================================

    @Test
    void loginValidoDevuelveAccessYRefreshToken() throws Exception {
        Usuario vendedor = crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.activo);

        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(vendedor.getCorreo(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.correo").value(vendedor.getCorreo()))
                .andExpect(jsonPath("$.data.rol").value("vendedor"))
                .andReturn();

        // Los tokens devueltos son de verdad válidos y del tipo correcto
        JsonNode data = objectMapper.readTree(resultado.getResponse().getContentAsString()).path("data");
        assertThat(jwtUtil.esTokenTecnicamenteValido(
                data.path("accessToken").asText(), JwtUtil.TIPO_ACCESS)).isTrue();
        assertThat(jwtUtil.esTokenTecnicamenteValido(
                data.path("refreshToken").asText(), JwtUtil.TIPO_REFRESH)).isTrue();
    }

    @Test
    void loginConPasswordIncorrectaDeUsuarioExistenteDevuelve401() throws Exception {
        Usuario vendedor = crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.activo);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(vendedor.getCorreo(), "ClaveEquivocada1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("incorrectos")));
    }

    @Test
    void loginDeUsuarioInactivoConPasswordCorrectaDevuelve401() throws Exception {
        Usuario inactivo = crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.inactivo);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(inactivo.getCorreo(), PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Contacte")));
    }

    @Test
    void loginDeUsuarioInactivoConPasswordIncorrectaNoRevelaQueEstaInactivo() throws Exception {
        // Quien no conoce la contraseña no debe poder saber que la cuenta está inactiva
        Usuario inactivo = crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.inactivo);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(inactivo.getCorreo(), "ClaveEquivocada1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("incorrectos")))
                .andExpect(jsonPath("$.message", not(containsString("inactiva"))))
                .andExpect(jsonPath("$.message", not(containsString("bloqueada"))));
    }

    // ============================================================
    // ACCESO CON ACCESS TOKEN (filtro JWT)
    // ============================================================

    @Test
    void meConAccessTokenValidoDevuelve200ConLosDatosDelUsuario() throws Exception {
        Usuario vendedor = crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        String access = accessTokenDe(vendedor);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(access)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.correo").value(vendedor.getCorreo()))
                .andExpect(jsonPath("$.data.rol").value("vendedor"));
    }

    @Test
    void meConTokenExpiradoDevuelve401PeroUnoVigenteDelMismoUsuarioPasa() throws Exception {
        Usuario vendedor = crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        String expirado = jwtUtilCon(jwtProperties.getSecret(), -1_000).generarAccessToken(vendedor);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(expirado)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        // Control: el mismo usuario con un token vigente sí entra (la causa del 401 es la expiración)
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(jwtUtil.generarAccessToken(vendedor))))
                .andExpect(status().isOk());
    }

    @Test
    void meConTokenFirmadoConOtraClaveDevuelve401() throws Exception {
        Usuario vendedor = crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        String falsificado = jwtUtilCon(OTRA_CLAVE, 60_000).generarAccessToken(vendedor);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(falsificado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidoDeUsuarioDesactivadoDespuesDeLoginDevuelve401() throws Exception {
        Usuario vendedor = crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        String access = accessTokenDe(vendedor);

        // El administrador desactiva la cuenta: el token ya emitido deja de servir (RF05)
        vendedor.setEstado(Usuario.EstadoUsuario.inactivo);
        usuarioRepository.saveAndFlush(vendedor);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(access)))
                .andExpect(status().isUnauthorized());
    }

    // ============================================================
    // REFRESH TOKEN
    // ============================================================

    @Test
    void refreshValidoDevuelveUnAccessTokenNuevoYUtilizable() throws Exception {
        Usuario vendedor = crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        String refresh = loginExitoso(vendedor).path("refreshToken").asText();

        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refresh))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").value(refresh))
                .andReturn();

        String nuevoAccess = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // El access token renovado autentica de verdad
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(nuevoAccess)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correo").value(vendedor.getCorreo()));
    }

    @Test
    void refreshDeUsuarioDesactivadoDevuelve401() throws Exception {
        Usuario vendedor = crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        String refresh = loginExitoso(vendedor).path("refreshToken").asText();

        vendedor.setEstado(Usuario.EstadoUsuario.inactivo);
        usuarioRepository.saveAndFlush(vendedor);

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refresh))))
                .andExpect(status().isUnauthorized());
    }

    // ============================================================
    // REGISTRO DE USUARIOS (solo administrador)
    // ============================================================

    @Test
    void registroConVendedorDevuelve403() throws Exception {
        Usuario vendedor = crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        String access = accessTokenDe(vendedor);

        mockMvc.perform(post("/api/v1/auth/registro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("nuevo@aac27-test.com", "5550001234", "vendedor")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // Y el usuario NO se creó
        assertThat(usuarioRepository.existsByCorreo("nuevo@aac27-test.com")).isFalse();
    }

    @Test
    void registroConAdministradorCreaElUsuarioConPasswordHasheada() throws Exception {
        Usuario admin = crearUsuario(Rol.NombreRol.administrador, Usuario.EstadoUsuario.activo);
        String access = accessTokenDe(admin);
        String correoNuevo = "nuevo@aac27-test.com";

        mockMvc.perform(post("/api/v1/auth/registro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(correoNuevo, "5550001234", "vendedor")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.correo").value(correoNuevo))
                .andExpect(jsonPath("$.data.rol").value("vendedor"))
                // La respuesta nunca incluye la contraseña ni su hash (RNF-SEG03)
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        Usuario creado = usuarioRepository.findByCorreoWithRol(correoNuevo).orElseThrow();
        assertThat(creado.getPasswordHash()).startsWith("$2a$12$"); // BCrypt con cost 12
        assertThat(creado.isEsPasswordTemporal()).isTrue();
        assertThat(creado.getEstado()).isEqualTo(Usuario.EstadoUsuario.activo);
    }

    @Test
    void registroConCorreoYaExistenteDevuelve409() throws Exception {
        Usuario admin = crearUsuario(Rol.NombreRol.administrador, Usuario.EstadoUsuario.activo);
        String access = accessTokenDe(admin);

        mockMvc.perform(post("/api/v1/auth/registro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(admin.getCorreo(), "5550009999", "vendedor")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registroNoPermiteCrearUnSuperadministrador() throws Exception {
        Usuario admin = crearUsuario(Rol.NombreRol.administrador, Usuario.EstadoUsuario.activo);
        String access = accessTokenDe(admin);

        mockMvc.perform(post("/api/v1/auth/registro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("super@aac27-test.com", "5550007777", "superadministrador")))
                .andExpect(status().isBadRequest());

        assertThat(usuarioRepository.existsByCorreo("super@aac27-test.com")).isFalse();
    }
}
