package com.aac27.joyeria.module.seguridad.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de petición para POST /api/v1/usuarios (crear usuario).
 * Solo el Administrador puede crear usuarios (RF06).
 *
 * <p>Validaciones alineadas con RF06:
 * "Campos obligatorios: nombre, documento, correo, teléfono, rol"
 * "Correo y número de documento únicos."
 * "Contraseña provisional generada con UUID aleatorio + hash BCrypt."
 *
 * <p>NOTA: La contraseña NO viene en este DTO porque el sistema
 * genera automáticamente una contraseña provisional (RF06).
 */
@Getter
@NoArgsConstructor
public class RegistrarUsuarioRequest {

    /**
     * Nombre completo del nuevo empleado.
     * @NotBlank: no puede ser vacío o solo espacios.
     * @Size: entre 3 y 150 caracteres (alineado con VARCHAR(150) en BD).
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String nombre;

    /**
     * Número de cédula o documento del empleado.
     * Debe ser único en el sistema (RF06).
     * Solo dígitos, entre 6 y 20 caracteres.
     */
    @NotBlank(message = "El número de documento es obligatorio")
    @Size(min = 6, max = 20, message = "El documento debe tener entre 6 y 20 caracteres")
    @Pattern(regexp = "^[0-9]+$", message = "El documento solo debe contener dígitos")
    private String numeroDocumento;

    /**
     * Correo electrónico del nuevo usuario.
     * Debe ser único (RF06). Se usará como nombre de usuario para el login.
     */
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 255, message = "El correo no puede superar 255 caracteres")
    private String correo;

    /**
     * Teléfono de contacto del empleado.
     * Acepta formato colombiano: +57 3001234567, 3001234567, etc.
     */
    @NotBlank(message = "El teléfono es obligatorio")
    @Size(min = 7, max = 20, message = "El teléfono debe tener entre 7 y 20 caracteres")
    private String telefono;

    /**
     * Nombre del rol a asignar al nuevo usuario.
     * Solo puede ser "administrador" o "vendedor" (no "superadministrador").
     * Validación: @Pattern limita los valores aceptados.
     */
    @NotBlank(message = "El rol es obligatorio")
    @Pattern(
            regexp = "^(administrador|vendedor)$",
            message = "El rol debe ser 'administrador' o 'vendedor'"
    )
    private String rol;
}
