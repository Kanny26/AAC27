package com.aac27.joyeria.module.compra.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para el registro y consulta de compras.
 * Nunca expone las entidades JPA directamente al cliente.
 *
 * <p>
 * Referencia: Sección 4.3 — Formato de respuesta estandarizado.
 */
@Getter
@Builder
public class CompraResponse {

    private Long compraId;
    private String nombreProveedor;
    private String nombreUsuario;
    private LocalDate fechaFactura;
    private LocalDate fechaEntregaEsperada;
    private String metodoPago;
    private String tipoPago;
    private String estado;
    private BigDecimal subtotal;
    private BigDecimal total;
    private String notas;
    private LocalDateTime creadoEn;

    /** Detalles de la compra. */
    private List<DetalleCompraResponse> detalles;

    /** Presente solo si tipoPago = 'credito'. */
    private CreditoCompraResponse credito;

    // ── DTOs anidados ──────────────────────────────────────────

    @Getter
    @Builder
    public static class DetalleCompraResponse {
        private Long detalleId;
        private Long productoId;
        private String codigoProducto;
        private String nombreProducto;
        private Integer cantidadPedida;
        private Integer cantidadRecibida;
        private Integer cantidadPendiente;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
    }

    @Getter
    @Builder
    public static class CreditoCompraResponse {
        private Long creditoId;
        private BigDecimal montoTotal;
        private BigDecimal saldoPendiente;
        private LocalDate fechaVencimiento;
        private String estado;
    }
}
