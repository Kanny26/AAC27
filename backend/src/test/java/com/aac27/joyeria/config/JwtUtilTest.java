package com.aac27.joyeria.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "clave-de-pruebas-aac27-con-mas-de-32-caracteres-xx";
    private static final String OTRA_CLAVE = "otra-clave-distinta-para-pruebas-aac27-0123456789";

    private JwtUtil jwtUtil;
    private UserDetails usuarioActivo;

    private static JwtUtil crearJwtUtil(String secret, long accessMs, long refreshMs) {
        JwtProperties props = new JwtProperties();
        props.setSecret(secret);
        props.setExpirationMs(accessMs);
        props.setRefreshExpirationMs(refreshMs);
        return new JwtUtil(props);
    }

    @BeforeEach
    void preparar() {
        jwtUtil = crearJwtUtil(SECRET, 60_000, 120_000);
        usuarioActivo = User.withUsername("ana@aac27.com").password("x").roles("vendedor").build();
    }

    @Test
    void accessTokenEsValidoParaSuUsuario() {
        String token = jwtUtil.generarAccessToken(usuarioActivo);

        assertThat(jwtUtil.esTokenTecnicamenteValido(token, JwtUtil.TIPO_ACCESS)).isTrue();
        assertThat(jwtUtil.esTokenValido(token, usuarioActivo, JwtUtil.TIPO_ACCESS)).isTrue();
    }

    @Test
    void refreshTokenNoSirveComoAccessToken() {
        String refresh = jwtUtil.generarRefreshToken(usuarioActivo);

        assertThat(jwtUtil.esTokenTecnicamenteValido(refresh, JwtUtil.TIPO_ACCESS)).isFalse();
        assertThat(jwtUtil.esTokenValido(refresh, usuarioActivo, JwtUtil.TIPO_ACCESS)).isFalse();
    }

    @Test
    void accessTokenNoSirveComoRefreshToken() {
        String access = jwtUtil.generarAccessToken(usuarioActivo);

        assertThat(jwtUtil.esTokenTecnicamenteValido(access, JwtUtil.TIPO_REFRESH)).isFalse();
        assertThat(jwtUtil.esTokenValido(access, usuarioActivo, JwtUtil.TIPO_REFRESH)).isFalse();
    }

    @Test
    void tokenExpiradoEsRechazado() {
        JwtUtil conExpiracionPasada = crearJwtUtil(SECRET, -1_000, -1_000);
        String token = conExpiracionPasada.generarAccessToken(usuarioActivo);

        assertThat(jwtUtil.esTokenTecnicamenteValido(token, JwtUtil.TIPO_ACCESS)).isFalse();
        assertThat(jwtUtil.esTokenValido(token, usuarioActivo, JwtUtil.TIPO_ACCESS)).isFalse();
    }

    @Test
    void tokenFirmadoConOtraClaveEsRechazado() {
        JwtUtil otroEmisor = crearJwtUtil(OTRA_CLAVE, 60_000, 120_000);
        String tokenFalso = otroEmisor.generarAccessToken(usuarioActivo);

        assertThat(jwtUtil.esTokenTecnicamenteValido(tokenFalso, JwtUtil.TIPO_ACCESS)).isFalse();
    }

    @Test
    void tokenMalformadoOVacioEsRechazado() {
        assertThat(jwtUtil.esTokenTecnicamenteValido("esto.no.es.un.jwt", JwtUtil.TIPO_ACCESS)).isFalse();
        assertThat(jwtUtil.esTokenTecnicamenteValido("", JwtUtil.TIPO_ACCESS)).isFalse();
    }

    @Test
    void usuarioInactivoNoPasaLaValidacionAunqueElTokenSeaCorrecto() {
        String token = jwtUtil.generarAccessToken(usuarioActivo);
        UserDetails inactivo = User.withUsername("ana@aac27.com").password("x")
                .roles("vendedor").disabled(true).build();

        assertThat(jwtUtil.esTokenValido(token, inactivo, JwtUtil.TIPO_ACCESS)).isFalse();
    }

    @Test
    void usuarioBloqueadoNoPasaLaValidacion() {
        String token = jwtUtil.generarAccessToken(usuarioActivo);
        UserDetails bloqueado = User.withUsername("ana@aac27.com").password("x")
                .roles("vendedor").accountLocked(true).build();

        assertThat(jwtUtil.esTokenValido(token, bloqueado, JwtUtil.TIPO_ACCESS)).isFalse();
    }

    @Test
    void tokenDeOtroUsuarioNoEsValido() {
        String token = jwtUtil.generarAccessToken(usuarioActivo);
        UserDetails otro = User.withUsername("luis@aac27.com").password("x").roles("vendedor").build();

        assertThat(jwtUtil.esTokenValido(token, otro, JwtUtil.TIPO_ACCESS)).isFalse();
    }
}