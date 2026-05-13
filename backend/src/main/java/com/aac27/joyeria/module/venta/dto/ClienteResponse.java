package com.aac27.joyeria.module.venta.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para consultas de clientes.
 * Referencia: RF-U02, RF-B01.
 */
@Getter
@Builder
public class ClienteResponse {

    private Long clienteId;
    private String nombre;
    private String numeroDocumento;
    private String telefono;
    private String correo;
    private String direccion;
    private LocalDate fechaNacimiento;

    /** Puntos de fidelidad acumulados (RF-V03). */
    private Integer puntosFidelidad;
    /** Equivalente en COP de los puntos acumulados (puntos × 100). */
    private Integer descuentoDisponibleCOP;

    private String notas;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
