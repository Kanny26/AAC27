package com.aac27.joyeria.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando no se encuentra un recurso en la base de datos.
 * Equivale al código HTTP 404 Not Found (Sección 4.3).
 *
 * <p>Uso: lanzar cuando un findById() no encuentre resultados.
 * <pre>
 * usuario = repo.findById(id)
 *     .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
 * </pre>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecursoNoEncontradoException extends RuntimeException {

    /**
     * @param recurso Nombre del recurso (ej: "Usuario", "Producto")
     * @param id      ID que no se encontró
     */
    public RecursoNoEncontradoException(String recurso, Long id) {
        super("No se encontró " + recurso + " con ID: " + id);
    }

    /** Constructor con mensaje personalizado. */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
