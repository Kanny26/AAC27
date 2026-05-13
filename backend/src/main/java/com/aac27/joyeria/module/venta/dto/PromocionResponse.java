package com.aac27.joyeria.module.venta.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para consultas de promociones.
 * Referencia: RF-V02, Sección 3.28.
 */
@Getter
@Builder
public class PromocionResponse {

    private Long promocionId;
    private String nombre;
    private String descripcion;
    private String tipo;
    private BigDecimal valor;
    private String aplicaA;
    private Long entidadId;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private boolean activa;

    /** Indica si la promoción está vigente hoy. */
    private boolean vigente;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
