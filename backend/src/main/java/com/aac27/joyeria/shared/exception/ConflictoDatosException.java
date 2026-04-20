package com.aac27.joyeria.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando hay un conflicto de datos únicos (duplicados).
 * Equivale al código HTTP 409 Conflict (Sección 4.3).
 *
 * <p>Uso típico: correo o documento ya registrado, SKU duplicado,
 * nombre de categoría ya existe.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictoDatosException extends RuntimeException {

    public ConflictoDatosException(String mensaje) {
        super(mensaje);
    }
}
