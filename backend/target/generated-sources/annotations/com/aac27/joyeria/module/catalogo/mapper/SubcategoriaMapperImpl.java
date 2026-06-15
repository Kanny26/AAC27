package com.aac27.joyeria.module.catalogo.mapper;

import com.aac27.joyeria.module.catalogo.dto.SubcategoriaRequest;
import com.aac27.joyeria.module.catalogo.dto.SubcategoriaResponse;
import com.aac27.joyeria.module.catalogo.entity.Subcategoria;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-15T17:51:32-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.3 (Eclipse Adoptium)"
)
@Component
public class SubcategoriaMapperImpl implements SubcategoriaMapper {

    @Override
    public SubcategoriaResponse toResponse(Subcategoria entity) {
        if ( entity == null ) {
            return null;
        }

        SubcategoriaResponse.SubcategoriaResponseBuilder subcategoriaResponse = SubcategoriaResponse.builder();

        subcategoriaResponse.id( entity.getSubcategoriaId() );
        subcategoriaResponse.nombre( entity.getNombre() );

        return subcategoriaResponse.build();
    }

    @Override
    public Subcategoria toEntity(SubcategoriaRequest request) {
        if ( request == null ) {
            return null;
        }

        Subcategoria.SubcategoriaBuilder subcategoria = Subcategoria.builder();

        subcategoria.nombre( request.getNombre() );

        return subcategoria.build();
    }

    @Override
    public void updateEntity(SubcategoriaRequest request, Subcategoria entity) {
        if ( request == null ) {
            return;
        }

        entity.setNombre( request.getNombre() );
    }
}
