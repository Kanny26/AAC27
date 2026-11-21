package com.aac27.joyeria.module.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Cuerpo de {@code POST /api/v1/auth/cambiar-password}.
 *
 * <p>Política de contraseñas (RF-S02): mínimo 8 caracteres, al menos una mayúscula
 * y al menos un número. El máximo de 72 es un límite técnico de BCrypt: lo que pase
 * de 72 bytes se ignora al calcular el hash.
 */
@Getter
@NoArgsConstructor
public class CambiarPasswordRequest {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String passwordActual;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La nueva contraseña debe tener entre 8 y 72 caracteres")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).*$",
            message = "La nueva contraseña debe incluir al menos una mayúscula y un número")
    private String passwordNueva;
}