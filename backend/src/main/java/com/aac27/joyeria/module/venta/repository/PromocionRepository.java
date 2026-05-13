package com.aac27.joyeria.module.venta.repository;

import com.aac27.joyeria.module.venta.entity.Promocion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio para {@link Promocion}.
 * Referencia: RF-V02, Sección 3.28.
 */
@Repository
public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    /**
     * Busca promociones vigentes en una fecha dada.
     * Una promoción está vigente si: activa=true, fechaInicio <= fecha <= fechaFin.
     *
     * @param fecha Fecha a verificar (normalmente LocalDate.now())
     * @return Lista de promociones vigentes
     */
    @Query("SELECT p FROM Promocion p WHERE p.activa = true " +
           "AND p.fechaInicio <= :fecha AND p.fechaFin >= :fecha " +
           "ORDER BY p.tipo ASC")
    List<Promocion> findVigentes(@Param("fecha") LocalDate fecha);

    /**
     * Busca promociones vigentes que aplican a una venta completa.
     */
    @Query("SELECT p FROM Promocion p WHERE p.activa = true " +
           "AND p.aplicaA = 'venta' " +
           "AND p.fechaInicio <= :fecha AND p.fechaFin >= :fecha")
    List<Promocion> findVigentesParaVenta(@Param("fecha") LocalDate fecha);

    /**
     * Busca promociones vigentes que aplican a un producto específico o a su categoría.
     *
     * @param productoId  ID del producto
     * @param categoriaId ID de la categoría del producto
     * @param fecha       Fecha de la venta
     */
    @Query("SELECT p FROM Promocion p WHERE p.activa = true " +
           "AND p.fechaInicio <= :fecha AND p.fechaFin >= :fecha " +
           "AND ((p.aplicaA = 'producto' AND p.entidadId = :productoId) " +
           "  OR (p.aplicaA = 'categoria' AND p.entidadId = :categoriaId))")
    List<Promocion> findVigentesParaProducto(
            @Param("productoId") Long productoId,
            @Param("categoriaId") Long categoriaId,
            @Param("fecha") LocalDate fecha);

    /** Listado paginado para el panel de administración. */
    Page<Promocion> findAllByOrderByFechaFinDesc(Pageable pageable);

    /** Listado de promociones activas para el panel del vendedor. */
    @Query("SELECT p FROM Promocion p WHERE p.activa = true ORDER BY p.fechaFin ASC")
    List<Promocion> findActivas();
}
