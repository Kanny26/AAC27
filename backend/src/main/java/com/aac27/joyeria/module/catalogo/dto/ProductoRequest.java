package com.aac27.joyeria.module.catalogo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

/**
 * DTO para crear o actualizar un Producto.
 * Referencia: RF-CAT01.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductoRequest {

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(min = 3, max = 255, message = "El nombre debe tener entre 3 y 255 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
    private String descripcion;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoriaId;

    @NotNull(message = "El material es obligatorio")
    private Long materialId;

    private Long proveedorId;

    @NotEmpty(message = "Debe seleccionar al menos una subcategoría")
    private Set<Long> subcategoriaIds;

    @NotNull(message = "El precio de costo es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio de costo debe ser mayor a 0")
    private BigDecimal precioCosto;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor a 0")
    private BigDecimal precioVenta;

    @NotNull(message = "El stock inicial es obligatorio")
    @Min(value = 0, message = "El stock inicial no puede ser negativo")
    private Integer stock;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    private Integer stockMinimo;

    private String imagenUrl;
    private String numeroSerie;
    private String numeroLote;

    /** Datos de trazabilidad si el material lo requiere. */
    @Valid
    private TrazabilidadRequest trazabilidad;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TrazabilidadRequest {
        private String ley;
        private BigDecimal quilates;
        private String certificadoProcedencia;
        private String piedrasDescripcion;
    }
}
