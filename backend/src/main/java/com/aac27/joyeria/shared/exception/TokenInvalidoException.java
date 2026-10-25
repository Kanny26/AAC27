package com.aac27.joyeria.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando un token no es aceptable: firma incorrecta, expirado,
 * tipo equivocado, usuario inexistente o inactivo. Responde 401.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenInvalidoException extends RuntimeException {
    public TokenInvalidoException(String mensaje) {
        super(mensaje);
    }
}
