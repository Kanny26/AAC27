package com.aac27.joyeria.module.catalogo.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO para la respuesta de Subcategoría.
 */
@Getter
@Builder
public class SubcategoriaResponse {
    private Long id;
    private String nombre;
    private Long categoriaId;
    private String nombreCategoria;
}
