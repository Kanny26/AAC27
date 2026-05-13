package com.aac27.joyeria.module.venta.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * DTO de respuesta para el registro de ventas y apartados.
 * Versión simplificada de confirmación — el frontend puede hacer
 * GET /api/v1/ventas/{id} para obtener el detalle completo.
 */
@Getter
@Builder
public class VentaResponse {

    private Long ventaId;
    private String numeroFactura;
    private BigDecimal subtotal;
    private BigDecimal descuentoTotal;
    private BigDecimal descuentoPuntos;
    private BigDecimal total;

    /** Puntos canjeados en esta venta. */
    private Integer puntosFidelidadUsados;
    /** Puntos ganados por esta compra. */
    private Integer puntosGanados;
    /** Saldo de puntos actualizado del cliente tras la venta. */
    private Integer saldoPuntosCliente;
    private String estado;
    private String mensaje;
}
