package com.aac27.joyeria.module.catalogo.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta de Categoría.
 */
@Getter
@Builder
public class CategoriaResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private String estado;
    private LocalDateTime creadoEn;
}
