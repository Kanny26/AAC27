package com.aac27.joyeria.config;

import com.aac27.joyeria.module.seguridad.entity.Rol;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** T-07 / RF02 y RF-S02: cambio de contraseña y política de contraseñas. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CambioPasswordTest extends AbstractAuthTest {

    private static final String NUEVA = "NuevaClave2026";
    private static final String URL = "/api/v1/auth/cambiar-password";

    private String cuerpoCambio(String actual, String nueva) throws Exception {
        return objectMapper.writeValueAsString(Map.of("passwordActual", actual, "passwordNueva", nueva));
    }

    /** Usuario con contraseña provisional (el estado en que nace tras RF06). */
    private Usuario usuarioConClaveProvisional() {
        Usuario u = crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        u.setEsPasswordTemporal(true);
        return usuarioRepository.saveAndFlush(u);
    }

    @Test
    void cambioValidoActualizaLaClaveYQuitaLaMarcaDeProvisional() throws Exception {
        Usuario usuario = usuarioConClaveProvisional();
        String access = accessTokenDe(usuario);

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCambio(PASSWORD, NUEVA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Con la nueva clave se entra y ya no pide cambio
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(usuario.getCorreo(), NUEVA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requiereCambioPassword").value(false));

        // Con la clave vieja ya no
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(usuario.getCorreo(), PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCambio(PASSWORD, NUEVA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conLaClaveActualIncorrectaDevuelve422() throws Exception {
        String access = accessTokenDe(usuarioConClaveProvisional());

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCambio("OtraClave999", NUEVA)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("La contraseña actual es incorrecta"));
    }

    @Test
    void laNuevaClaveNoPuedeSerIgualALaActual() throws Exception {
        // PASSWORD ("Clave12345") cumple la política, así que falla por ser igual y no por formato
        String access = accessTokenDe(usuarioConClaveProvisional());

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCambio(PASSWORD, PASSWORD)))
                .andExpect(status().isUnprocessableEntity());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Corta1",            // menos de 8 caracteres
            "sinmayuscula123",   // sin mayúscula
            "SinNumeroAquiYa",   // sin número
            // 73 caracteres (el máximo permitido es 72): "A1" + 71 letras "a"
            "A1aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    })
    void unaNuevaClaveQueNoCumpleLaPoliticaDevuelve400(String claveInvalida) throws Exception {
        String access = accessTokenDe(usuarioConClaveProvisional());

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCambio(PASSWORD, claveInvalida)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}