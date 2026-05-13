package com.aac27.joyeria.module.catalogo.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO para la respuesta de Material.
 */
@Getter
@Builder
public class MaterialResponse {
    private Long id;
    private String nombre;
    private boolean esTrazable;
    private String estado;
}
