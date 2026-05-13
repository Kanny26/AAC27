package com.aac27.joyeria.module.catalogo.mapper;

import com.aac27.joyeria.module.catalogo.dto.MaterialRequest;
import com.aac27.joyeria.module.catalogo.dto.MaterialResponse;
import com.aac27.joyeria.module.catalogo.entity.Material;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper para la entidad Material.
 */
@Mapper
public interface MaterialMapper {

    @Mapping(target = "id", source = "materialId")
    MaterialResponse toResponse(Material entity);

    @Mapping(target = "materialId", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Material toEntity(MaterialRequest request);

    @Mapping(target = "materialId", ignore = true)
    @Mapping(target = "estado", ignore = true)
    void updateEntity(MaterialRequest request, @MappingTarget Material entity);
}
