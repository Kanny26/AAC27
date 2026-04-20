package com.aac27.joyeria.module.seguridad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de petición para el endpoint POST /api/v1/auth/login.
 *
 * <p>Recibe las credenciales del usuario desde el frontend React.
 * Las anotaciones de Bean Validation validan automáticamente antes
 * de que llegue al Service (con @Valid en el Controller).
 *
 * <p>Los mensajes de error están en español (RNF-MAN04).
 *
 * <p>¿Por qué un DTO y no la entidad directamente?
 * La entidad Usuario implementa UserDetails y tiene campos de BD.
 * El DTO solo contiene lo que el cliente necesita enviar.
 */
@Getter
@NoArgsConstructor
public class LoginRequest {

    /**
     * Correo electrónico del usuario.
     *
     * <p>@NotBlank: rechaza null, "" y " " (espacios).
     * <p>@Email: valida formato usuario@dominio.com
     */
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String correo;

    /**
     * Contraseña en texto plano enviada por el usuario.
     * NUNCA se almacena en BD. Solo se compara contra el BCrypt hash.
     *
     * <p>@Size: rechaza contraseñas demasiado cortas.
     * El mínimo de 6 aquí es solo para validación básica de entrada;
     * la política real (mín 8, mayúscula, número) se valida en el cambio de contraseña.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;
}
