package com.aac27.joyeria.module.catalogo.service;

import com.aac27.joyeria.module.catalogo.dto.MaterialRequest;
import com.aac27.joyeria.module.catalogo.dto.MaterialResponse;
import com.aac27.joyeria.module.catalogo.entity.Material;
import com.aac27.joyeria.module.catalogo.entity.Producto;
import com.aac27.joyeria.module.catalogo.mapper.MaterialMapper;
import com.aac27.joyeria.module.catalogo.repository.MaterialRepository;
import com.aac27.joyeria.module.catalogo.repository.ProductoRepository;
import com.aac27.joyeria.shared.exception.ConflictoDatosException;
import com.aac27.joyeria.shared.exception.RecursoNoEncontradoException;
import com.aac27.joyeria.shared.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la gestión de materiales.
 * Referencia: RF14.
 */
@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final ProductoRepository productoRepository;
    private final MaterialMapper materialMapper;

    @Transactional(readOnly = true)
    public Page<MaterialResponse> listar(Pageable pageable) {
        return materialRepository.findAll(pageable)
                .map(materialMapper::toResponse);
    }

    @Transactional
    public MaterialResponse crear(MaterialRequest request) {
        if (materialRepository.existsByNombre(request.getNombre())) {
            throw new ConflictoDatosException("Ya existe un material con el nombre: " + request.getNombre());
        }

        Material material = materialMapper.toEntity(request);
        material.setEstado(Material.EstadoMaterial.activo);
        
        return materialMapper.toResponse(materialRepository.save(material));
    }

    @Transactional
    public MaterialResponse actualizar(Long id, MaterialRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Material", id));

        if (!material.getNombre().equalsIgnoreCase(request.getNombre()) &&
            materialRepository.existsByNombre(request.getNombre())) {
            throw new ConflictoDatosException("Ya existe otro material con el nombre: " + request.getNombre());
        }

        materialMapper.updateEntity(request, material);
        return materialMapper.toResponse(materialRepository.save(material));
    }

    @Transactional
    public void cambiarEstado(Long id, String nuevoEstado) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Material", id));

        Material.EstadoMaterial estado = Material.EstadoMaterial.valueOf(nuevoEstado.toLowerCase());
        
        if (estado == Material.EstadoMaterial.inactivo) {
            long productosActivos = productoRepository.countByMaterialMaterial_idAndEstado(id, Producto.EstadoProducto.activo);
            if (productosActivos > 0) {
                throw new ReglaNegocioException("No se puede desactivar el material porque tiene " + productosActivos + " productos activos asociados.");
            }
        }

        material.setEstado(estado);
        materialRepository.save(material);
    }
}
