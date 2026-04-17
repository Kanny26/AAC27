/**
 * Paquete de configuración global de Spring Boot.
 *
 * <p>Contiene las clases de configuración de:
 * <ul>
 *   <li>{@code SecurityConfig} — Cadena de filtros de Spring Security, JWT, RBAC</li>
 *   <li>{@code CorsConfig} — Configuración de CORS para permitir el frontend React</li>
 *   <li>{@code JwtConfig} — Propiedades del token JWT leídas del application.yml</li>
 *   <li>{@code JpaConfig} — Configuración adicional de JPA/Hibernate si se necesita</li>
 *   <li>{@code AsyncConfig} — Configuración del pool de hilos para operaciones @Async</li>
 * </ul>
 *
 * <p>Referencia: Sección 4.1 (Seguridad Spring Security), RNF-SEG01 a RNF-SEG08.
 */
package com.aac27.joyeria.config;
