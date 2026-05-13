package com.aac27.joyeria.module.catalogo.service;

import com.aac27.joyeria.module.catalogo.dto.CategoriaRequest;
import com.aac27.joyeria.module.catalogo.dto.CategoriaResponse;
import com.aac27.joyeria.module.catalogo.entity.Categoria;
import com.aac27.joyeria.module.catalogo.mapper.CategoriaMapper;
import com.aac27.joyeria.module.catalogo.repository.CategoriaRepository;
import com.aac27.joyeria.module.catalogo.repository.ProductoRepository;
import com.aac27.joyeria.module.catalogo.entity.Producto;
import com.aac27.joyeria.shared.exception.ConflictoDatosException;
import com.aac27.joyeria.shared.exception.RecursoNoEncontradoException;
import com.aac27.joyeria.shared.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la gestión de categorías.
 * Referencia: RF12.
 */
@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaMapper categoriaMapper;

    @Transactional(readOnly = true)
    public Page<CategoriaResponse> listar(Pageable pageable) {
        return categoriaRepository.findAll(pageable)
                .map(categoriaMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtenerPorId(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", id));
        return categoriaMapper.toResponse(categoria);
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        if (categoriaRepository.existsByNombre(request.getNombre())) {
            throw new ConflictoDatosException("Ya existe una categoría con el nombre: " + request.getNombre());
        }

        Categoria categoria = categoriaMapper.toEntity(request);
        categoria.setEstado(Categoria.EstadoCategoria.activo);
        
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", id));

        // Validar nombre único si cambió
        if (!categoria.getNombre().equalsIgnoreCase(request.getNombre()) &&
            categoriaRepository.existsByNombre(request.getNombre())) {
            throw new ConflictoDatosException("Ya existe otra categoría con el nombre: " + request.getNombre());
        }

        categoriaMapper.updateEntity(request, categoria);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public void cambiarEstado(Long id, String nuevoEstado) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", id));

        Categoria.EstadoCategoria estado = Categoria.EstadoCategoria.valueOf(nuevoEstado.toLowerCase());
        
        // Regla: No se puede desactivar si tiene productos activos (RF12)
        if (estado == Categoria.EstadoCategoria.inactivo) {
            long productosActivos = productoRepository.countByCategoriaCategoria_idAndEstado(id, Producto.EstadoProducto.activo);
            if (productosActivos > 0) {
                throw new ReglaNegocioException("No se puede desactivar la categoría porque tiene " + productosActivos + " productos activos asociados.");
            }
        }

        categoria.setEstado(estado);
        categoriaRepository.save(categoria);
    }
}
