package com.aac27.joyeria.module.catalogo.mapper;

import com.aac27.joyeria.module.catalogo.dto.ProductoRequest;
import com.aac27.joyeria.module.catalogo.dto.ProductoResponse;
import com.aac27.joyeria.module.catalogo.entity.Categoria;
import com.aac27.joyeria.module.catalogo.entity.Material;
import com.aac27.joyeria.module.catalogo.entity.Producto;
import com.aac27.joyeria.module.catalogo.entity.ProductoTrazabilidad;
import com.aac27.joyeria.module.catalogo.entity.Subcategoria;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-15T17:51:34-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.3 (Eclipse Adoptium)"
)
@Component
public class ProductoMapperImpl implements ProductoMapper {

    @Override
    public ProductoResponse toResponse(Producto entity) {
        if ( entity == null ) {
            return null;
        }

        ProductoResponse.ProductoResponseBuilder productoResponse = ProductoResponse.builder();

        productoResponse.id( entity.getProductoId() );
        productoResponse.categoriaId( entityCategoriaCategoriaId( entity ) );
        productoResponse.nombreCategoria( entityCategoriaNombre( entity ) );
        productoResponse.materialId( entityMaterialMaterialId( entity ) );
        productoResponse.nombreMaterial( entityMaterialNombre( entity ) );
        productoResponse.subcategorias( subcategoriaSetToSubcategoriaSimpleResponseList( entity.getSubcategorias() ) );
        productoResponse.trazabilidad( toTrazabilidadDto( entity.getTrazabilidad() ) );
        productoResponse.codigo( entity.getCodigo() );
        productoResponse.nombre( entity.getNombre() );
        productoResponse.descripcion( entity.getDescripcion() );
        productoResponse.precioCosto( entity.getPrecioCosto() );
        productoResponse.precioVenta( entity.getPrecioVenta() );
        productoResponse.stock( entity.getStock() );
        productoResponse.stockMinimo( entity.getStockMinimo() );
        productoResponse.imagenUrl( entity.getImagenUrl() );
        if ( entity.getEstado() != null ) {
            productoResponse.estado( entity.getEstado().name() );
        }

        return productoResponse.build();
    }

    @Override
    public ProductoResponse.SubcategoriaSimpleResponse toSubcategoriaDto(Subcategoria entity) {
        if ( entity == null ) {
            return null;
        }

        ProductoResponse.SubcategoriaSimpleResponse.SubcategoriaSimpleResponseBuilder subcategoriaSimpleResponse = ProductoResponse.SubcategoriaSimpleResponse.builder();

        subcategoriaSimpleResponse.nombre( entity.getNombre() );

        return subcategoriaSimpleResponse.build();
    }

    @Override
    public ProductoResponse.TrazabilidadResponse toTrazabilidadDto(ProductoTrazabilidad entity) {
        if ( entity == null ) {
            return null;
        }

        ProductoResponse.TrazabilidadResponse.TrazabilidadResponseBuilder trazabilidadResponse = ProductoResponse.TrazabilidadResponse.builder();

        trazabilidadResponse.ley( entity.getLey() );
        trazabilidadResponse.quilates( entity.getQuilates() );
        trazabilidadResponse.certificadoProcedencia( entity.getCertificadoProcedencia() );
        trazabilidadResponse.piedrasDescripcion( entity.getPiedrasDescripcion() );

        return trazabilidadResponse.build();
    }

    @Override
    public Producto toEntity(ProductoRequest request) {
        if ( request == null ) {
            return null;
        }

        Producto.ProductoBuilder<?, ?> producto = Producto.builder();

        producto.nombre( request.getNombre() );
        producto.descripcion( request.getDescripcion() );
        producto.precioCosto( request.getPrecioCosto() );
        producto.precioVenta( request.getPrecioVenta() );
        producto.stock( request.getStock() );
        producto.stockMinimo( request.getStockMinimo() );
        producto.imagenUrl( request.getImagenUrl() );
        producto.numeroSerie( request.getNumeroSerie() );
        producto.numeroLote( request.getNumeroLote() );

        return producto.build();
    }

    @Override
    public ProductoTrazabilidad toTrazabilidadEntity(ProductoRequest.TrazabilidadRequest request) {
        if ( request == null ) {
            return null;
        }

        ProductoTrazabilidad.ProductoTrazabilidadBuilder productoTrazabilidad = ProductoTrazabilidad.builder();

        productoTrazabilidad.ley( request.getLey() );
        productoTrazabilidad.quilates( request.getQuilates() );
        productoTrazabilidad.certificadoProcedencia( request.getCertificadoProcedencia() );
        productoTrazabilidad.piedrasDescripcion( request.getPiedrasDescripcion() );

        return productoTrazabilidad.build();
    }

    @Override
    public void updateEntity(ProductoRequest request, Producto entity) {
        if ( request == null ) {
            return;
        }

        entity.setNombre( request.getNombre() );
        entity.setDescripcion( request.getDescripcion() );
        entity.setPrecioCosto( request.getPrecioCosto() );
        entity.setPrecioVenta( request.getPrecioVenta() );
        entity.setStock( request.getStock() );
        entity.setStockMinimo( request.getStockMinimo() );
        entity.setImagenUrl( request.getImagenUrl() );
        entity.setNumeroSerie( request.getNumeroSerie() );
        entity.setNumeroLote( request.getNumeroLote() );
    }

    private Long entityCategoriaCategoriaId(Producto producto) {
        if ( producto == null ) {
            return null;
        }
        Categoria categoria = producto.getCategoria();
        if ( categoria == null ) {
            return null;
        }
        Long categoriaId = categoria.getCategoriaId();
        if ( categoriaId == null ) {
            return null;
        }
        return categoriaId;
    }

    private String entityCategoriaNombre(Producto producto) {
        if ( producto == null ) {
            return null;
        }
        Categoria categoria = producto.getCategoria();
        if ( categoria == null ) {
            return null;
        }
        String nombre = categoria.getNombre();
        if ( nombre == null ) {
            return null;
        }
        return nombre;
    }

    private Long entityMaterialMaterialId(Producto producto) {
        if ( producto == null ) {
            return null;
        }
        Material material = producto.getMaterial();
        if ( material == null ) {
            return null;
        }
        Long materialId = material.getMaterialId();
        if ( materialId == null ) {
            return null;
        }
        return materialId;
    }

    private String entityMaterialNombre(Producto producto) {
        if ( producto == null ) {
            return null;
        }
        Material material = producto.getMaterial();
        if ( material == null ) {
            return null;
        }
        String nombre = material.getNombre();
        if ( nombre == null ) {
            return null;
        }
        return nombre;
    }

    protected List<ProductoResponse.SubcategoriaSimpleResponse> subcategoriaSetToSubcategoriaSimpleResponseList(Set<Subcategoria> set) {
        if ( set == null ) {
            return null;
        }

        List<ProductoResponse.SubcategoriaSimpleResponse> list = new ArrayList<ProductoResponse.SubcategoriaSimpleResponse>( set.size() );
        for ( Subcategoria subcategoria : set ) {
            list.add( toSubcategoriaDto( subcategoria ) );
        }

        return list;
    }
}
