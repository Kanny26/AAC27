package com.aac27.joyeria.config;

import com.aac27.joyeria.module.seguridad.entity.PasswordHistorial;
import com.aac27.joyeria.module.seguridad.entity.Rol;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.module.seguridad.repository.PasswordHistorialRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** T-07 / RF-S02: no se pueden reutilizar las últimas 5 contraseñas (la vigente cuenta como una). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HistorialPasswordTest extends AbstractAuthTest {

    private static final String URL = "/api/v1/auth/cambiar-password";

    /** De la más antigua a la más reciente; la última es la contraseña vigente tras sembrar. */
    private static final String[] ANTERIORES = {
            "Anterior1Clave", "Anterior2Clave", "Anterior3Clave", "Anterior4Clave", "Anterior5Clave"
    };
    private static final String ACTUAL = ANTERIORES[4];

    @Autowired
    private PasswordHistorialRepository historialRepository;

    private String cuerpoCambio(String actual, String nueva) throws Exception {
        return objectMapper.writeValueAsString(Map.of("passwordActual", actual, "passwordNueva", nueva));
    }

    /**
     * Deja al usuario con 6 contraseñas en el historial: {@code PASSWORD} (la más vieja)
     * y las 5 de {@link #ANTERIORES}, siendo la última la vigente.
     * Hay que pedir el token ANTES de sembrar, porque después la clave de login cambia.
     */
    private void sembrarHistorial(Usuario usuario) {
        historialRepository.save(PasswordHistorial.builder()
                .usuarioId(usuario.getUsuarioId())
                .passwordHash(usuario.getPassword())
                .build());

        String hashVigente = usuario.getPassword();
        for (String clave : ANTERIORES) {
            hashVigente = passwordEncoder.encode(clave);
            historialRepository.save(PasswordHistorial.builder()
                    .usuarioId(usuario.getUsuarioId())
                    .passwordHash(hashVigente)
                    .build());
        }
        usuario.setPasswordHash(hashVigente);
        usuarioRepository.saveAndFlush(usuario);
    }

    @Test
    void rechazaUnaContrasenaQueSigueEntreLasUltimas5() throws Exception {
        Usuario usuario = crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        String access = accessTokenDe(usuario);
        sembrarHistorial(usuario);

        // "Anterior1Clave" es la 5.ª más reciente: sigue dentro de la ventana
        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCambio(ACTUAL, ANTERIORES[0])))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("últimas 5")));
    }

    @Test
    void permiteUnaContrasenaQueYaSalioDeLasUltimas5YLaAgregaAlHistorial() throws Exception {
        Usuario usuario = crearUsuario(Rol.NombreRol.vendedor, Usuario.EstadoUsuario.activo);
        String access = accessTokenDe(usuario);
        sembrarHistorial(usuario);

        // PASSWORD es la 6.ª más reciente: ya salió de la ventana
        mockMvc.perform(post(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCambio(ACTUAL, PASSWORD)))
                .andExpect(status().isOk());

        List<PasswordHistorial> recientes =
                historialRepository.findTop5ByUsuarioIdOrderByHistorialIdDesc(usuario.getUsuarioId());
        assertThat(recientes).hasSize(5);
        // La más reciente del historial es la nueva y coincide con la que quedó en el usuario
        assertThat(passwordEncoder.matches(PASSWORD, recientes.get(0).getPasswordHash())).isTrue();
        assertThat(recientes.get(0).getPasswordHash())
                .isEqualTo(usuarioRepository.findById(usuario.getUsuarioId()).orElseThrow().getPassword());
    }

    @Test
    void registrarUnUsuarioGuardaSuContrasenaProvisionalEnElHistorial() throws Exception {
        String access = accessTokenDe(crearUsuario(Rol.NombreRol.administrador, Usuario.EstadoUsuario.activo));

        mockMvc.perform(post("/api/v1/auth/registro")
                        .header(HttpHeaders.AUTHORIZATION, bearer(access))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro("historial@aac27-test.com", "5550006666", "vendedor")))
                .andExpect(status().isCreated());

        Usuario creado = usuarioRepository.findByCorreoWithRol("historial@aac27-test.com").orElseThrow();
        List<PasswordHistorial> historial =
                historialRepository.findTop5ByUsuarioIdOrderByHistorialIdDesc(creado.getUsuarioId());

        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).getPasswordHash()).isEqualTo(creado.getPassword());
    }
}