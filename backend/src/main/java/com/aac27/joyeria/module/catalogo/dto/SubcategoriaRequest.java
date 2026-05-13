package com.aac27.joyeria.module.catalogo.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para crear o actualizar una Subcategoría.
 * Referencia: RF13.
 */
@Getter
@Setter
@NoArgsConstructor
public class SubcategoriaRequest {

    @NotBlank(message = "El nombre de la subcategoría es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;

    @NotNull(message = "La categoría padre es obligatoria")
    private Long categoriaId;
}
