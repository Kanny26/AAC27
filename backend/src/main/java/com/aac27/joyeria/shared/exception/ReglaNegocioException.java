package com.aac27.joyeria.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando se viola una regla de negocio.
 * Equivale al código HTTP 422 Unprocessable Entity (Sección 4.3).
 *
 * <p>Ejemplos de uso:
 * <ul>
 *   <li>Precio de venta menor o igual al precio de costo (RF-CAT01)</li>
 *   <li>Stock resultante negativo (RF15)</li>
 *   <li>Monto de abono mayor al saldo pendiente (RF-C02)</li>
 *   <li>Correo duplicado al crear usuario (RF06)</li>
 * </ul>
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
