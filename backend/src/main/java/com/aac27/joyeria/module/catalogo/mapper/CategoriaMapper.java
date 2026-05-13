package com.aac27.joyeria.module.catalogo.mapper;

import com.aac27.joyeria.module.catalogo.dto.CategoriaRequest;
import com.aac27.joyeria.module.catalogo.dto.CategoriaResponse;
import com.aac27.joyeria.module.catalogo.entity.Categoria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper para la entidad Categoria.
 * Usa ComponentModel = spring (definido globalmente en pom.xml).
 */
@Mapper
public interface CategoriaMapper {

    @Mapping(target = "id", source = "categoriaId")
    CategoriaResponse toResponse(Categoria entity);

    @Mapping(target = "categoriaId", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Categoria toEntity(CategoriaRequest request);

    @Mapping(target = "categoriaId", ignore = true)
    @Mapping(target = "estado", ignore = true)
    void updateEntity(CategoriaRequest request, @MappingTarget Categoria entity);
}
