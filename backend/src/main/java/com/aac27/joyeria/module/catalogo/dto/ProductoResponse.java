package com.aac27.joyeria.module.catalogo.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para la respuesta de Producto.
 */
@Getter
@Builder
public class ProductoResponse {
    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private Long categoriaId;
    private String nombreCategoria;
    private Long materialId;
    private String nombreMaterial;
    private List<SubcategoriaSimpleResponse> subcategorias;
    private BigDecimal precioCosto;
    private BigDecimal precioVenta;
    private Integer stock;
    private Integer stockMinimo;
    private String imagenUrl;
    private String estado;
    private TrazabilidadResponse trazabilidad;

    @Getter
    @Builder
    public static class SubcategoriaSimpleResponse {
        private Long id;
        private String nombre;
    }

    @Getter
    @Builder
    public static class TrazabilidadResponse {
        private String ley;
        private BigDecimal quilates;
        private String certificadoProcedencia;
        private String piedrasDescripcion;
    }
}
