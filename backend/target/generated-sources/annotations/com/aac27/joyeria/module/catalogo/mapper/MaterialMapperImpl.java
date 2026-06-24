package com.aac27.joyeria.module.catalogo.mapper;

import com.aac27.joyeria.module.catalogo.dto.MaterialRequest;
import com.aac27.joyeria.module.catalogo.dto.MaterialResponse;
import com.aac27.joyeria.module.catalogo.entity.Material;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-24T12:13:52-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23 (Oracle Corporation)"
)
@Component
public class MaterialMapperImpl implements MaterialMapper {

    @Override
    public MaterialResponse toResponse(Material entity) {
        if ( entity == null ) {
            return null;
        }

        MaterialResponse.MaterialResponseBuilder materialResponse = MaterialResponse.builder();

        materialResponse.id( entity.getMaterialId() );
        materialResponse.nombre( entity.getNombre() );
        materialResponse.esTrazable( entity.isEsTrazable() );
        if ( entity.getEstado() != null ) {
            materialResponse.estado( entity.getEstado().name() );
        }

        return materialResponse.build();
    }

    @Override
    public Material toEntity(MaterialRequest request) {
        if ( request == null ) {
            return null;
        }

        Material.MaterialBuilder material = Material.builder();

        material.nombre( request.getNombre() );
        if ( request.getEsTrazable() != null ) {
            material.esTrazable( request.getEsTrazable() );
        }

        return material.build();
    }

    @Override
    public void updateEntity(MaterialRequest request, Material entity) {
        if ( request == null ) {
            return;
        }

        entity.setNombre( request.getNombre() );
        if ( request.getEsTrazable() != null ) {
            entity.setEsTrazable( request.getEsTrazable() );
        }
    }
}
