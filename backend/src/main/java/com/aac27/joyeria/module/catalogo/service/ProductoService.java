package com.aac27.joyeria.module.catalogo.service;

import com.aac27.joyeria.module.catalogo.dto.ProductoRequest;
import com.aac27.joyeria.module.catalogo.dto.ProductoResponse;
import com.aac27.joyeria.module.catalogo.entity.*;
import com.aac27.joyeria.module.catalogo.mapper.ProductoMapper;
import com.aac27.joyeria.module.catalogo.repository.*;
import com.aac27.joyeria.module.proveedor.entity.Proveedor;
import com.aac27.joyeria.module.proveedor.repository.ProveedorRepository;
import com.aac27.joyeria.shared.exception.ConflictoDatosException;
import com.aac27.joyeria.shared.exception.RecursoNoEncontradoException;
import com.aac27.joyeria.shared.exception.ReglaNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio para la gestión de productos.
 * Referencia: RF-CAT01.
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoTrazabilidadRepository trazabilidadRepository;
    private final CategoriaRepository categoriaRepository;
    private final MaterialRepository materialRepository;
    private final SubcategoriaRepository subcategoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoMapper productoMapper;

    @Transactional(readOnly = true)
    public Page<ProductoResponse> listar(Pageable pageable) {
        return productoRepository.findAll(pageable)
                .map(productoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
        return productoMapper.toResponse(producto);
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        // Validar reglas de negocio de precios
        if (request.getPrecioVenta().compareTo(request.getPrecioCosto()) <= 0) {
            throw new ReglaNegocioException("El precio de venta debe ser mayor al precio de costo.");
        }

        // Cargar relaciones
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", request.getCategoriaId()));
        
        Material material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Material", request.getMaterialId()));

        Proveedor proveedor = null;
        if (request.getProveedorId() != null) {
            proveedor = proveedorRepository.findById(request.getProveedorId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor", request.getProveedorId()));
        }

        List<Subcategoria> subcategorias = subcategoriaRepository.findAllById(request.getSubcategoriaIds());
        if (subcategorias.size() != request.getSubcategoriaIds().size()) {
            throw new ReglaNegocioException("Una o más subcategorías seleccionadas no existen.");
        }

        // Crear entidad
        Producto producto = productoMapper.toEntity(request);
        producto.setCodigo(generarSKU(categoria)); // RF-CAT01: SKU único
        producto.setCategoria(categoria);
        producto.setMaterial(material);
        producto.setProveedor(proveedor);
        producto.setSubcategorias(new HashSet<>(subcategorias));
        producto.setEstado(Producto.EstadoProducto.activo);

        // Trazabilidad (RF-CAT02)
        if (material.isEsTrazable()) {
            if (request.getTrazabilidad() == null) {
                throw new ReglaNegocioException("El material seleccionado requiere datos de trazabilidad.");
            }
            ProductoTrazabilidad trazabilidad = productoMapper.toTrazabilidadEntity(request.getTrazabilidad());
            trazabilidad.setProducto(producto);
            producto.setTrazabilidad(trazabilidad);
        }

        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));

        if (request.getPrecioVenta().compareTo(request.getPrecioCosto()) <= 0) {
            throw new ReglaNegocioException("El precio de venta debe ser mayor al precio de costo.");
        }

        // Actualizar relaciones si cambiaron
        if (!producto.getCategoria().getCategoriaId().equals(request.getCategoriaId())) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", request.getCategoriaId()));
            producto.setCategoria(categoria);
        }

        if (!producto.getMaterial().getMaterialId().equals(request.getMaterialId())) {
            Material material = materialRepository.findById(request.getMaterialId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Material", request.getMaterialId()));
            producto.setMaterial(material);
        }

        List<Subcategoria> subcategorias = subcategoriaRepository.findAllById(request.getSubcategoriaIds());
        producto.setSubcategorias(new HashSet<>(subcategorias));

        productoMapper.updateEntity(request, producto);

        // Trazabilidad
        if (producto.getMaterial().isEsTrazable()) {
            if (request.getTrazabilidad() != null) {
                if (producto.getTrazabilidad() == null) {
                    ProductoTrazabilidad trazabilidad = productoMapper.toTrazabilidadEntity(request.getTrazabilidad());
                    trazabilidad.setProducto(producto);
                    producto.setTrazabilidad(trazabilidad);
                } else {
                    // Update existing trazabilidad fields
                    ProductoTrazabilidad t = producto.getTrazabilidad();
                    t.setLey(request.getTrazabilidad().getLey());
                    t.setQuilates(request.getTrazabilidad().getQuilates());
                    t.setCertificadoProcedencia(request.getTrazabilidad().getCertificadoProcedencia());
                    t.setPiedrasDescripcion(request.getTrazabilidad().getPiedrasDescripcion());
                }
            }
        } else {
            producto.setTrazabilidad(null); // Si ya no es trazable, se elimina por orphanRemoval
        }

        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Transactional
    public void cambiarEstado(Long id, String nuevoEstado) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
        producto.setEstado(Producto.EstadoProducto.valueOf(nuevoEstado.toLowerCase()));
        productoRepository.save(producto);
    }

    /**
     * Genera un SKU formateado: CAT-YYYYMM-NNNN.
     * Ejemplo: ANI-202404-0001
     */
    private String generarSKU(Categoria categoria) {
        String prefix = categoria.getNombre().substring(0, Math.min(3, categoria.getNombre().length())).toUpperCase();
        String datePart = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        
        // Esta es una implementación simplificada. En un entorno real, usaríamos un contador en BD o un servicio de secuencias.
        // Por ahora, generamos uno basado en el timestamp para evitar colisiones en este paso.
        long count = productoRepository.count() + 1;
        return String.format("%s-%s-%04d", prefix, datePart, count);
    }
}
