package com.aac27.joyeria.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Wrapper estándar para TODAS las respuestas de la API REST.
 *
 * <p>Formato definido en Sección 4.3 del documento de arquitectura:
 * <pre>
 * {
 *   "success": true/false,
 *   "message": "Descripción del resultado",
 *   "data": { ... } o null,
 *   "errors": [ ... ] o null,
 *   "timestamp": "2026-04-17T10:30:00",
 *   "pagination": { "page": 0, "size": 20, "totalElements": 150, "totalPages": 8 }
 * }
 * </pre>
 *
 * <p>El parámetro genérico {@code <T>} permite usar este wrapper con
 * cualquier tipo de dato: {@code ApiResponse<LoginResponse>},
 * {@code ApiResponse<List<CategoriaResponse>>}, etc.
 *
 * <p>Referencia: Sección 4.1 (Manejo de Excepciones) y RNF-MAN05.
 *
 * @param <T> Tipo del campo {@code data}
 */
@Getter
@Builder
// @JsonInclude: omite campos null en el JSON de respuesta (respuestas más limpias)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** true si la operación fue exitosa, false si hubo un error. */
    private final boolean success;

    /** Mensaje descriptivo del resultado para el usuario o el frontend. */
    private final String message;

    /** Datos del resultado. null en respuestas de error. */
    private final T data;

    /**
     * Lista de errores de validación. null en respuestas exitosas.
     * Cada elemento describe un campo y su error (Sección 4.3).
     */
    private final List<String> errors;

    /** Timestamp automático de cuándo se generó la respuesta. */
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    /** Información de paginación. Solo presente en listados paginados. */
    private final PaginationInfo pagination;

    // ============================================================
    // MÉTODOS FACTORY — Simplifican la creación de respuestas comunes
    // ============================================================

    /**
     * Respuesta exitosa con datos.
     *
     * @param data    Objeto de datos a devolver
     * @param message Mensaje descriptivo del éxito
     * @param <T>     Tipo de los datos
     * @return ApiResponse con success=true
     */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Respuesta exitosa sin datos (para DELETE o acciones que no retornan datos).
     *
     * @param message Mensaje del resultado
     * @param <T>     Tipo genérico (usualmente Void)
     * @return ApiResponse con success=true y data=null
     */
    public static <T> ApiResponse<T> ok(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Respuesta exitosa con paginación.
     *
     * @param data       Lista de datos de la página actual
     * @param message    Mensaje descriptivo
     * @param pagination Información de paginación
     * @param <T>        Tipo de los datos
     * @return ApiResponse paginado
     */
    public static <T> ApiResponse<T> paginado(T data, String message, PaginationInfo pagination) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .pagination(pagination)
                .build();
    }

    /**
     * Respuesta de error con mensaje.
     *
     * @param message Mensaje de error descriptivo
     * @param <T>     Tipo genérico
     * @return ApiResponse con success=false
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    /**
     * Respuesta de error con lista de errores de validación.
     *
     * @param message Mensaje principal del error
     * @param errors  Lista de errores específicos de campos
     * @param <T>     Tipo genérico
     * @return ApiResponse con success=false y lista de errores
     */
    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errors(errors)
                .build();
    }

    // ============================================================
    // CLASE ANIDADA: INFORMACIÓN DE PAGINACIÓN
    // ============================================================

    /**
     * Metadatos de paginación incluidos en respuestas de listado.
     * Sección 4.3: "page, size, totalElements, totalPages"
     */
    @Getter
    @Builder
    public static class PaginationInfo {
        /** Número de página actual (base 0). */
        private final int page;
        /** Cantidad de elementos por página. */
        private final int size;
        /** Total de elementos en todas las páginas. */
        private final long totalElements;
        /** Total de páginas disponibles. */
        private final int totalPages;

        /**
         * Crea un PaginationInfo a partir de un Page de Spring Data.
         *
         * @param page Objeto Page de Spring Data JPA
         * @return PaginationInfo con los metadatos de la página
         */
        public static PaginationInfo of(org.springframework.data.domain.Page<?> page) {
            return PaginationInfo.builder()
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .build();
        }
    }
}
