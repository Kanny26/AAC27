package com.aac27.joyeria.module.catalogo.service;

import com.aac27.joyeria.module.catalogo.dto.SubcategoriaRequest;
import com.aac27.joyeria.module.catalogo.dto.SubcategoriaResponse;
import com.aac27.joyeria.module.catalogo.entity.Categoria;
import com.aac27.joyeria.module.catalogo.entity.Subcategoria;
import com.aac27.joyeria.module.catalogo.mapper.SubcategoriaMapper;
import com.aac27.joyeria.module.catalogo.repository.CategoriaRepository;
import com.aac27.joyeria.module.catalogo.repository.SubcategoriaRepository;
import com.aac27.joyeria.shared.exception.ConflictoDatosException;
import com.aac27.joyeria.shared.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la gestión de subcategorías.
 * Referencia: RF13.
 */
@Service
@RequiredArgsConstructor
public class SubcategoriaService {

    private final SubcategoriaRepository subcategoriaRepository;
    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaMapper subcategoriaMapper;

    @Transactional(readOnly = true)
    public Page<SubcategoriaResponse> listar(Pageable pageable) {
        return subcategoriaRepository.findAll(pageable)
                .map(subcategoriaMapper::toResponse);
    }

    @Transactional
    public SubcategoriaResponse crear(SubcategoriaRequest request) {
        if (subcategoriaRepository.existsByNombre(request.getNombre())) {
            throw new ConflictoDatosException("Ya existe una subcategoría con el nombre: " + request.getNombre());
        }

        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", request.getCategoriaId()));

        Subcategoria subcategoria = subcategoriaMapper.toEntity(request);
        subcategoria.setCategoria(categoria);
        
        return subcategoriaMapper.toResponse(subcategoriaRepository.save(subcategoria));
    }

    @Transactional
    public SubcategoriaResponse actualizar(Long id, SubcategoriaRequest request) {
        Subcategoria subcategoria = subcategoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Subcategoría", id));

        if (!subcategoria.getNombre().equalsIgnoreCase(request.getNombre()) &&
            subcategoriaRepository.existsByNombre(request.getNombre())) {
            throw new ConflictoDatosException("Ya existe otra subcategoría con el nombre: " + request.getNombre());
        }

        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", request.getCategoriaId()));

        subcategoriaMapper.updateEntity(request, subcategoria);
        subcategoria.setCategoria(categoria);
        
        return subcategoriaMapper.toResponse(subcategoriaRepository.save(subcategoria));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!subcategoriaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Subcategoría", id);
        }
        // Nota: El documento no especifica restricciones fuertes para eliminar subcategorías,
        // pero Hibernate lanzará error si hay productos asociados debido a la FK.
        subcategoriaRepository.deleteById(id);
    }
}
