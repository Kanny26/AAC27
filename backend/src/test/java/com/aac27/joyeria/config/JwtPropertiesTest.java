package com.aac27.joyeria.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-07: la aplicación NO debe arrancar con un secreto JWT ausente o demasiado corto.
 *
 * <p>No necesita base de datos: solo comprueba el enlace y la validación de
 * {@link JwtProperties} con un contexto mínimo.
 */
class JwtPropertiesTest {

    @EnableConfigurationProperties(JwtProperties.class)
    static class Configuracion {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Configuracion.class);

    @Test
    void secretoDe32CaracteresPermiteArrancar() {
        runner.withPropertyValues(
                        "jwt.secret=" + "x".repeat(32),
                        "jwt.expiration-ms=1000",
                        "jwt.refresh-expiration-ms=2000")
                .run(contexto -> {
                    assertThat(contexto).hasNotFailed();
                    assertThat(contexto.getBean(JwtProperties.class).getSecret()).hasSize(32);
                });
    }

    @Test
    void secretoDemasiadoCortoImpideArrancar() {
        runner.withPropertyValues(
                        "jwt.secret=corta",
                        "jwt.expiration-ms=1000",
                        "jwt.refresh-expiration-ms=2000")
                .run(contexto -> assertThat(contexto).hasFailed());
    }

    @Test
    void secretoAusenteImpideArrancar() {
        runner.withPropertyValues(
                        "jwt.expiration-ms=1000",
                        "jwt.refresh-expiration-ms=2000")
                .run(contexto -> assertThat(contexto).hasFailed());
    }

    @Test
    void expiracionNoPositivaImpideArrancar() {
        runner.withPropertyValues(
                        "jwt.secret=" + "x".repeat(32),
                        "jwt.expiration-ms=0",
                        "jwt.refresh-expiration-ms=2000")
                .run(contexto -> assertThat(contexto).hasFailed());
    }
}
