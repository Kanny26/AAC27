package com.aac27.joyeria.module.catalogo.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para crear o actualizar un Material.
 * Referencia: RF14.
 */
@Getter
@Setter
@NoArgsConstructor
public class MaterialRequest {

    @NotBlank(message = "El nombre del material es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;

    @NotNull(message = "Debe especificar si el material es trazable")
    private Boolean esTrazable;
}
