package com.aac27.joyeria.shared.exception;

/**
 * Se lanza cuando no se pudo enviar un correo obligatorio. Es RuntimeException a propósito: así la transacción
 * del servicio se revierte y no queda un usuario creado que nadie puede usar.
 */
public class CorreoNoEnviadoException extends RuntimeException {
    public CorreoNoEnviadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

