package com.aac27.joyeria.module.venta.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para apartados/layaway.
 * Referencia: RF-V01, Sección 3.19.
 */
@Getter
@Builder
public class ApartadoResponse {

    private Long apartadoId;

    /** Nombre del cliente. */
    private String clienteNombre;
    /** Documento del cliente. */
    private String clienteDocumento;

    /** Nombre del producto reservado. */
    private String productoNombre;
    /** Código SKU del producto. */
    private String productoSku;

    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal totalAPagar;
    private BigDecimal totalAbonado;
    private BigDecimal saldoPendiente;

    /** Porcentaje de avance del pago: (totalAbonado / totalAPagar) × 100 */
    private Integer porcentajePagado;

    private LocalDate fechaLimite;

    /** Días restantes hasta la fecha límite. Negativo si ya venció. */
    private Long diasRestantes;

    private String estado;
    private LocalDateTime createdAt;

    /** ID de la venta definitiva generada al completar (null si aún activo). */
    private Long ventaDefinitivaId;
    /** Número de factura de la venta definitiva (null si aún activo). */
    private String ventaDefinitivaFactura;
}
