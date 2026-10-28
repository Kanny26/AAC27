package com.aac27.joyeria.config;

import com.aac27.joyeria.module.seguridad.entity.Rol;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T-07: la contraseña provisional NO debe escribirse en el log fuera del perfil {@code dev}.
 *
 * <p>El perfil de pruebas deja {@code com.aac27.joyeria} en WARN, así que aquí se sube a INFO;
 * si no, el log de la contraseña (que es INFO) no saldría nunca y la prueba pasaría sin
 * demostrar nada. Por eso hay además una comprobación de control.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "logging.level.com.aac27.joyeria=INFO")
class ContrasenaProvisionalLogTest extends AbstractAuthTest {

    @Test
    void registrarUsuarioFueraDelPerfilDevNoEscribeLaContrasenaEnElLog(CapturedOutput salida) throws Exception {
        Usuario admin = crearUsuario(Rol.NombreRol.administrador, Usuario.EstadoUsuario.activo);
        String access = accessTokenDe(admin);

        mockMvc.perform(post("/api/v1/auth/registro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("log@aac27-test.com", "5550002222", "vendedor")))
                .andExpect(status().isCreated());

        // Control: los logs INFO sí se están capturando (si no, esta prueba no probaría nada)
        assertThat(salida.getAll()).contains("Registrando nuevo usuario con correo");

        // La contraseña provisional no aparece en el log del perfil de pruebas
        assertThat(salida.getAll())
                .doesNotContain("provisional")
                .doesNotContain("[DEV]");
    }
}
