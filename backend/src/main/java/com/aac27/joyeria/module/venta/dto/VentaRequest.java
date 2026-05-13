package com.aac27.joyeria.module.venta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de petición para POST /api/v1/ventas — registrar venta o apartado.
 * Referencia: RF17-RF21, RF-V01, RF-V03.
 */
@Getter
@NoArgsConstructor
public class VentaRequest {

    /** null = cliente ocasional (RF17). */
    private Long clienteId;

    @NotNull(message = "El método de pago es obligatorio")
    @Positive(message = "El ID de método de pago debe ser positivo")
    private Long metodoPagoId;

    @NotBlank(message = "La modalidad de pago es obligatoria")
    @Pattern(regexp = "^(contado|anticipo|credito|layaway)$", message = "Modalidad debe ser: contado, anticipo, credito o layaway")
    private String modalidadPago;

    @NotNull(message = "El monto pagado es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto pagado debe ser mayor a 0")
    private BigDecimal montoPagado;

    /**
     * Puntos de fidelidad a canjear en esta compra (RF-V03).
     * null o 0 = no canjear puntos.
     */
    @Min(value = 0, message = "Los puntos a usar no pueden ser negativos")
    private Integer puntosAUsarFidelidad;

    /** Requerido solo si modalidadPago = 'layaway'. */
    private LocalDate fechaLimiteApartado;

    private String observaciones;

    @NotEmpty(message = "La venta debe tener al menos un ítem")
    @Valid
    private List<ItemVentaRequest> items;

    /**
     * DTO de ítem de venta.
     * El precio lo determina el servidor (producto.precioVenta) — no el cliente.
     */
    @Getter
    @NoArgsConstructor
    public static class ItemVentaRequest {

        @NotNull(message = "El ID del producto es obligatorio")
        @Positive(message = "El ID del producto debe ser positivo")
        private Long productoId;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad mínima es 1")
        @Max(value = 999, message = "La cantidad máxima por ítem es 999")
        private Integer cantidad;

        /**
         * Descuento en pesos para ESTE ítem (no porcentaje).
         * null = sin descuento.
         */
        @DecimalMin(value = "0.0", inclusive = true, message = "El descuento no puede ser negativo")
        private BigDecimal descuentoItem;

        /**
         * Meses de garantía para este ítem (RF-V04).
         * null o 0 = sin garantía.
         */
        @Min(value = 0, message = "Los meses de garantía no pueden ser negativos")
        @Max(value = 60, message = "La garantía máxima es 60 meses (5 años)")
        private Integer garantiaMeses;

        private String notasItem;
    }
}
