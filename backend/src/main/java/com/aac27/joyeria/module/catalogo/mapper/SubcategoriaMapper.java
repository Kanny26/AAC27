package com.aac27.joyeria.module.catalogo.mapper;

import com.aac27.joyeria.module.catalogo.dto.SubcategoriaRequest;
import com.aac27.joyeria.module.catalogo.dto.SubcategoriaResponse;
import com.aac27.joyeria.module.catalogo.entity.Subcategoria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper para la entidad Subcategoria.
 */
@Mapper
public interface SubcategoriaMapper {

    @Mapping(target = "id", source = "subcategoriaId")
    SubcategoriaResponse toResponse(Subcategoria entity);

    @Mapping(target = "subcategoriaId", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Subcategoria toEntity(SubcategoriaRequest request);

    @Mapping(target = "subcategoriaId", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(SubcategoriaRequest request, @MappingTarget Subcategoria entity);
}
