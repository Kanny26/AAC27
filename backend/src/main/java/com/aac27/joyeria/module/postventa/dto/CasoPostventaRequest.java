package com.aac27.joyeria.module.postventa.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CasoPostventaRequest {

    // venta_id es opcional, puede ser nulo si es una reparación de una joya externa
    // o un reclamo no asociado a una factura en el sistema.
    private Long ventaId;

    @NotNull(message = "El cliente_id es obligatorio")
    private Long clienteId;

    @NotBlank(message = "El tipo de caso es obligatorio")
    private String tipo; // reclamo, devolucion, cambio, reparacion

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    // --- Campos Exclusivos para Reparaciones ---
    private String descripcionTrabajo;
    private BigDecimal presupuesto;
    private LocalDate fechaEntregaEstimada;

    // --- Ítems devueltos/cambiados ---
    @Valid
    private List<DetalleCasoRequest> detalles;

    @Data
    @Builder
    public static class DetalleCasoRequest {
        @NotNull
        private Long productoId;
        @NotNull
        private Integer cantidad;
        @NotBlank
        private String motivo;
        @NotBlank
        private String estadoProducto; // bueno, defectuoso
    }
}
