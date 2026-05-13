package com.aac27.joyeria.module.postventa.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CasoPostventaResponse {
    
    private Long casoId;
    private String numeroFactura;
    private String nombreCliente;
    private String tipo;
    private String estado;
    private String descripcion;
    private String respuestaAdmin;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;

    // Reparaciones
    private String descripcionTrabajo;
    private BigDecimal presupuesto;
    private BigDecimal costoReal;
    private String estadoReparacion;
    private LocalDate fechaEntregaEstimada;

    private List<DetalleCasoResponse> detalles;
    private List<HistorialCasoResponse> historial;

    @Data
    @Builder
    public static class DetalleCasoResponse {
        private Long productoId;
        private String nombreProducto;
        private Integer cantidad;
        private String motivo;
        private String estadoProducto;
    }

    @Data
    @Builder
    public static class HistorialCasoResponse {
        private String usuario;
        private String estadoAnterior;
        private String estadoNuevo;
        private String comentario;
        private LocalDateTime fechaCambio;
    }
}
