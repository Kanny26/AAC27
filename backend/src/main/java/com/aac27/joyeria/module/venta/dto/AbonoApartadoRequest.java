package com.aac27.joyeria.module.venta.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para registrar un abono a un apartado existente.
 * Referencia: RF-V01, Sección 3.19.
 *
 * <p>Usado en: POST /api/v1/ventas/apartados/{id}/abonos
 */
@Getter
@NoArgsConstructor
public class AbonoApartadoRequest {

    @NotNull(message = "El método de pago es obligatorio")
    @Positive(message = "El ID del método de pago debe ser positivo")
    private Long metodoPagoId;

    @NotNull(message = "El monto del abono es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto del abono debe ser mayor a 0")
    private BigDecimal monto;

    /** Número de referencia para transferencias/Nequi. Opcional. */
    @Size(max = 100, message = "La referencia no puede exceder 100 caracteres")
    private String referencia;
}
