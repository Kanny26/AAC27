package com.aac27.joyeria.config;

import com.aac27.joyeria.module.seguridad.entity.Rol;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** T-07 / RF02: con contraseña provisional el backend bloquea todo salvo ver el perfil y cambiarla. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordProvisionalBloqueoTest extends AbstractAuthTest {

    /** Ruta real y de lectura, abierta a cualquier usuario autenticado (sirve de control). */
    private static final String RUTA_DE_NEGOCIO = "/api/v1/categorias";

    private Usuario usuarioProvisional(Rol.NombreRol rol) {
        Usuario u = crearUsuario(rol, Usuario.EstadoUsuario.activo);
        u.setEsPasswordTemporal(true);
        return usuarioRepository.saveAndFlush(u);
    }

    private int estadoDe(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion)
            throws Exception {
        return mockMvc.perform(peticion).andReturn().getResponse().getStatus();
    }

    @Test
    void conClaveProvisionalUnaRutaDeNegocioDevuelve403ConCodigoEstable() throws Exception {
        String access = accessTokenDe(usuarioProvisional(Rol.NombreRol.vendedor));

        mockMvc.perform(get(RUTA_DE_NEGOCIO).header(HttpHeaders.AUTHORIZATION, bearer(access)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0]").value("PASSWORD_CHANGE_REQUIRED"));
    }

    @Test
    void conClaveProvisionalNiUnAdministradorPuedeRegistrarUsuarios() throws Exception {
        String access = accessTokenDe(usuarioProvisional(Rol.NombreRol.administrador));

        mockMvc.perform(post("/api/v1/auth/registro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("bloqueado@aac27-test.com", "5550005555", "vendedor")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors[0]").value("PASSWORD_CHANGE_REQUIRED"));
    }

    @Test
    void conClaveProvisionalSePuedeVerElPerfilYSeIndicaQueDebeCambiarla() throws Exception {
        String access = accessTokenDe(usuarioProvisional(Rol.NombreRol.vendedor));

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(access)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiereCambioPassword").value(true));
    }

    @Test
    void conClaveProvisionalSePuedeRenovarElToken() throws Exception {
        Usuario usuario = usuarioProvisional(Rol.NombreRol.vendedor);
        String refresh = loginExitoso(usuario).path("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refresh))))
                .andExpect(status().isOk());
    }

    @Test
    void elMismoTokenFuncionaApenasSeCambiaLaClave() throws Exception {
        String access = accessTokenDe(usuarioProvisional(Rol.NombreRol.vendedor));

        // Antes: bloqueado
        assertThat(estadoDe(get(RUTA_DE_NEGOCIO).header(HttpHeaders.AUTHORIZATION, bearer(access))))
                .isEqualTo(403);

        // Cambia la contraseña con ese mismo token
        mockMvc.perform(post("/api/v1/auth/cambiar-password")
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("passwordActual", PASSWORD, "passwordNueva", "NuevaClave2026"))))
                .andExpect(status().isOk());

        // Después: ya no es el bloqueo por clave provisional, con el mismo token y sin volver a entrar
        assertThat(estadoDe(get(RUTA_DE_NEGOCIO).header(HttpHeaders.AUTHORIZATION, bearer(access))))
                .isNotEqualTo(403);
    }

    @Test
    void unUsuarioConClavePropiaNoEstaRestringido() throws Exception {
        // Control: demuestra que el 403 de las pruebas anteriores se debe a la clave provisional
        // y no al rol ni a la ruta
        String access = accessTokenDe(crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.activo));

        assertThat(estadoDe(get(RUTA_DE_NEGOCIO).header(HttpHeaders.AUTHORIZATION, bearer(access))))
                .isNotEqualTo(403);
    }

    @Test
    void laListaBlancaFallaCerradaConOtroMetodoOUnaVarianteDeRuta() throws Exception {
        String access = accessTokenDe(usuarioProvisional(Rol.NombreRol.vendedor));

        // Mismo path permitido pero con otro método
        assertThat(estadoDe(get("/api/v1/auth/cambiar-password").header(HttpHeaders.AUTHORIZATION, bearer(access))))
                .isEqualTo(403);
        // Variante con barra final de una ruta permitida
        assertThat(estadoDe(get("/api/v1/auth/me/").header(HttpHeaders.AUTHORIZATION, bearer(access))))
                .isEqualTo(403);
    }
}