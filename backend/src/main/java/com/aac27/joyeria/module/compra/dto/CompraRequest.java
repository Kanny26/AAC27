package com.aac27.joyeria.module.compra.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO de petición para POST /api/v1/compras — registrar una orden de compra.
 *
 * <p>
 * Contiene los datos de la cabecera y la lista de ítems.
 * Los subtotales y totales los calcula el Service (no confiar en el cliente).
 *
 * <p>
 * Referencia: RF11 — Sección 3.14, 3.15.
 */
@Getter
@NoArgsConstructor
public class CompraRequest {

    @NotNull(message = "El proveedor es obligatorio")
    @Positive(message = "El ID del proveedor debe ser un número positivo")
    private Long proveedorId;

    @NotNull(message = "La fecha de factura es obligatoria")
    @PastOrPresent(message = "La fecha de factura no puede ser futura")
    private LocalDate fechaFactura;

    /**
     * Fecha comprometida de entrega. Validado en Service: debe ser >= fechaFactura.
     */
    private LocalDate fechaEntregaEsperada;

    @NotNull(message = "El método de pago es obligatorio")
    @Positive(message = "El ID de método de pago debe ser positivo")
    private Long metodoPagoId;

    @NotNull(message = "El tipo de pago es obligatorio")
    @Pattern(regexp = "^(contado|credito)$", message = "El tipo de pago debe ser 'contado' o 'credito'")
    private String tipoPago;

    /** Solo requerido si tipoPago = 'credito'. Validado en Service. */
    private LocalDate fechaVencimientoCredito;

    @Size(max = 1000, message = "Las notas no pueden superar 1000 caracteres")
    private String notas;

    @NotEmpty(message = "La compra debe tener al menos un producto")
    @Valid // Propaga validaciones a cada ítem de la lista
    private List<DetalleCompraRequest> detalles;

    // ── DTO anidado ────────────────────────────────────────────

    /**
     * DTO de un ítem de la orden de compra.
     * Clase estática anidada: vive dentro del contexto de CompraRequest.
     */
    @Getter
    @NoArgsConstructor
    public static class DetalleCompraRequest {

        @NotNull(message = "El producto es obligatorio en cada ítem")
        @Positive(message = "El ID del producto debe ser positivo")
        private Long productoId;

        @NotNull(message = "La cantidad pedida es obligatoria")
        @Min(value = 1, message = "La cantidad mínima de pedido es 1")
        @Max(value = 9999, message = "La cantidad máxima por ítem es 9,999")
        private Integer cantidadPedida;

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio unitario debe ser mayor a 0")
        @Digits(integer = 12, fraction = 2, message = "El precio unitario debe tener máximo 2 decimales")
        private BigDecimal precioUnitario;
    }
}
