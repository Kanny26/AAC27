/**
 * Paquete compartido (cross-cutting concerns).
 *
 * <p>Contiene componentes reutilizables por TODOS los módulos:
 * <ul>
 *   <li>{@code exception/} — Excepciones de dominio y @ControllerAdvice global</li>
 *   <li>{@code response/} — Wrapper ApiResponse&lt;T&gt; para respuestas estandarizadas</li>
 *   <li>{@code util/} — Utilidades de fechas, texto, generación de códigos</li>
 *   <li>{@code audit/} — Entidad base con campos created_at, updated_at, @EntityListeners</li>
 *   <li>{@code constant/} — Constantes del sistema (roles, estados, mensajes)</li>
 * </ul>
 *
 * <p>Referencia: Sección 4.1 (Manejo de Excepciones, ApiResponse), RNF-MAN05.
 */
package com.aac27.joyeria.shared;
