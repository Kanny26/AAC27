package com.aac27.joyeria.module.catalogo.mapper;

import com.aac27.joyeria.module.catalogo.dto.ProductoRequest;
import com.aac27.joyeria.module.catalogo.dto.ProductoResponse;
import com.aac27.joyeria.module.catalogo.entity.Producto;
import com.aac27.joyeria.module.catalogo.entity.ProductoTrazabilidad;
import com.aac27.joyeria.module.catalogo.entity.Subcategoria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper para la entidad Producto.
 */
@Mapper
public interface ProductoMapper {

    @Mapping(target = "id", source = "productoId")
    @Mapping(target = "categoriaId", source = "categoria.categoriaId")
    @Mapping(target = "nombreCategoria", source = "categoria.nombre")
    @Mapping(target = "materialId", source = "material.materialId")
    @Mapping(target = "nombreMaterial", source = "material.nombre")
    @Mapping(target = "subcategorias", source = "subcategorias")
    @Mapping(target = "trazabilidad", source = "trazabilidad")
    ProductoResponse toResponse(Producto entity);

    ProductoResponse.SubcategoriaSimpleResponse toSubcategoriaDto(Subcategoria entity);

    ProductoResponse.TrazabilidadResponse toTrazabilidadDto(ProductoTrazabilidad entity);

    @Mapping(target = "productoId", ignore = true)
    @Mapping(target = "codigo", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "material", ignore = true)
    @Mapping(target = "proveedor", ignore = true)
    @Mapping(target = "subcategorias", ignore = true)
    @Mapping(target = "trazabilidad", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "version", ignore = true)
    Producto toEntity(ProductoRequest request);

    @Mapping(target = "trazabilidadId", ignore = true)
    @Mapping(target = "producto", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ProductoTrazabilidad toTrazabilidadEntity(ProductoRequest.TrazabilidadRequest request);

    @Mapping(target = "productoId", ignore = true)
    @Mapping(target = "codigo", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "material", ignore = true)
    @Mapping(target = "proveedor", ignore = true)
    @Mapping(target = "subcategorias", ignore = true)
    @Mapping(target = "trazabilidad", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(ProductoRequest request, @MappingTarget Producto entity);
}
