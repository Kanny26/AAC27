package com.aac27.joyeria.module.venta.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de petición para crear o actualizar un cliente.
 * Referencia: RF-U02, Sección 3.3.
 */
@Getter
@NoArgsConstructor
public class ClienteRequest {

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(min = 2, max = 150, message = "El nombre debe tener entre 2 y 150 caracteres")
    private String nombre;

    @Size(max = 20, message = "El número de documento no puede exceder 20 caracteres")
    private String numeroDocumento;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String telefono;

    @Email(message = "El formato del correo no es válido")
    @Size(max = 255, message = "El correo no puede exceder 255 caracteres")
    private String correo;

    @Size(max = 500, message = "La dirección no puede exceder 500 caracteres")
    private String direccion;

    /** Fecha de nacimiento para campañas de fidelidad. Opcional. */
    private LocalDate fechaNacimiento;

    @Size(max = 1000, message = "Las notas no pueden exceder 1000 caracteres")
    private String notas;
}
