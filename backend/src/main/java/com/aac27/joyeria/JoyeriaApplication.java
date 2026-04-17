package com.aac27.joyeria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Clase principal del Sistema de Gestión de Joyería AAC27.
 *
 * <p>Anotaciones clave:
 * <ul>
 *   <li>{@code @SpringBootApplication}: combina @Configuration + @EnableAutoConfiguration + @ComponentScan</li>
 *   <li>{@code @EnableJpaAuditing}: activa @CreatedDate y @LastModifiedDate en entidades JPA (Sección 3.29)</li>
 *   <li>{@code @EnableAsync}: permite métodos @Async para generar PDFs en segundo plano (RNF-REN05)</li>
 * </ul>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class JoyeriaApplication {

    public static void main(String[] args) {
        SpringApplication.run(JoyeriaApplication.class, args);
    }
}
