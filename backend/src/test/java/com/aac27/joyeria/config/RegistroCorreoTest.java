package com.aac27.joyeria.config;

import com.aac27.joyeria.module.seguridad.entity.Rol;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.shared.exception.CorreoNoEnviadoException;
import com.aac27.joyeria.shared.mail.CorreoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** T-07 / RF06: al registrar un usuario se envía su contraseña provisional por correo. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistroCorreoTest extends AbstractAuthTest {

    @MockBean
    private CorreoService correoService;

    @Test
    void registroEnviaLaContrasenaProvisionalYEstaCoincideConElHashGuardado() throws Exception {
        String access = accessTokenDe(crearUsuario(Rol.NombreRol.administrador, Usuario.EstadoUsuario.activo));

        mockMvc.perform(post("/api/v1/auth/registro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("nuevo-correo@aac27-test.com", "5550003333", "vendedor")))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> clave = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviarPasswordProvisional(
                eq("nuevo-correo@aac27-test.com"), eq("Maria Garcia"), clave.capture());

        Usuario creado = usuarioRepository.findByCorreoWithRol("nuevo-correo@aac27-test.com").orElseThrow();
        assertThat(creado.isEsPasswordTemporal()).isTrue();
        // La clave enviada por correo es la que quedó guardada (como hash BCrypt)
        assertThat(passwordEncoder.matches(clave.getValue(), creado.getPassword())).isTrue();
        assertThat(creado.getPassword()).isNotEqualTo(clave.getValue());
    }

    @Test
    void siFallaElCorreoLaRespuestaEs500SinExponerElDetalleTecnico() throws Exception {
        String access = accessTokenDe(crearUsuario(Rol.NombreRol.administrador, Usuario.EstadoUsuario.activo));
        doThrow(new CorreoNoEnviadoException("smtp caido", new RuntimeException("detalle-interno")))
                .when(correoService).enviarPasswordProvisional(any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/registro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("falla-correo@aac27-test.com", "5550004444", "vendedor")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("La cuenta no fue creada")))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("detalle-interno"))));
    }
}