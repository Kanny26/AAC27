package com.aac27.joyeria.module.catalogo.mapper;

import com.aac27.joyeria.module.catalogo.dto.CategoriaRequest;
import com.aac27.joyeria.module.catalogo.dto.CategoriaResponse;
import com.aac27.joyeria.module.catalogo.entity.Categoria;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-24T12:13:55-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23 (Oracle Corporation)"
)
@Component
public class CategoriaMapperImpl implements CategoriaMapper {

    @Override
    public CategoriaResponse toResponse(Categoria entity) {
        if ( entity == null ) {
            return null;
        }

        CategoriaResponse.CategoriaResponseBuilder categoriaResponse = CategoriaResponse.builder();

        categoriaResponse.id( entity.getCategoriaId() );
        categoriaResponse.nombre( entity.getNombre() );
        if ( entity.getEstado() != null ) {
            categoriaResponse.estado( entity.getEstado().name() );
        }

        return categoriaResponse.build();
    }

    @Override
    public Categoria toEntity(CategoriaRequest request) {
        if ( request == null ) {
            return null;
        }

        Categoria.CategoriaBuilder categoria = Categoria.builder();

        categoria.nombre( request.getNombre() );

        return categoria.build();
    }

    @Override
    public void updateEntity(CategoriaRequest request, Categoria entity) {
        if ( request == null ) {
            return;
        }

        entity.setNombre( request.getNombre() );
    }
}
