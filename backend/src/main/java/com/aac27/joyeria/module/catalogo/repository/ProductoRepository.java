package com.aac27.joyeria.module.catalogo.repository;

import com.aac27.joyeria.module.catalogo.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para {@link Producto}.
 * Referencia: RF-CAT01, RF-B02 — CRUD y búsqueda de productos, Sección 4.4.
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.productoId = :productoId")
    Optional<Producto> findByIdWithLock(@Param("productoId") Long productoId);

    /** SKU único — verificación antes de crear (RF-CAT01). */
    boolean existsByCodigo(String codigo);

    /**
     * Cuenta productos ACTIVOS en una categoría.
     * Usado para validar si se puede desactivar una categoría (RF12).
     */
    long countByCategoriaCategoria_idAndEstado(
            Long categoriaId, Producto.EstadoProducto estado);

    /**
     * Cuenta productos ACTIVOS con un material.
     * Usado para validar si se puede desactivar un material (RF14).
     */
    long countByMaterialMaterial_idAndEstado(
            Long materialId, Producto.EstadoProducto estado);

    /**
     * Listado paginado de productos activos con stock > 0.
     * Usado en la búsqueda de productos para venta (RF-B02):
     * "Solo muestra productos activos con stock > 0."
     */
    @Query("SELECT p FROM Producto p WHERE p.estado = 'activo' AND p.stock > 0 AND (" +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(p.codigo) LIKE LOWER(CONCAT('%', :termino, '%')))")
    Page<Producto> buscarParaVenta(@Param("termino") String termino, Pageable pageable);

    /**
     * Listado paginado con todos los filtros combinables (RF07 análogo para productos).
     * Paginación server-side con filtro de estado.
     */
    Page<Producto> findByEstado(Producto.EstadoProducto estado, Pageable pageable);

    /**
     * Productos con stock bajo (stock < stockMinimo).
     * Para el dashboard de alertas (RF-INV01).
     */
    @Query("SELECT p FROM Producto p WHERE p.estado = 'activo' AND p.stock < p.stockMinimo")
    Page<Producto> findProductosConStockBajo(Pageable pageable);
}
