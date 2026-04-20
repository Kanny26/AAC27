package com.aac27.joyeria.module.compra.repository;

import com.aac27.joyeria.module.compra.entity.InventarioMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repositorio para {@link InventarioMovimiento}.
 * Referencia: RF15, RF16 — Control e historial de inventario.
 */
@Repository
public interface InventarioMovimientoRepository
        extends JpaRepository<InventarioMovimiento, Long> {

    /**
     * Historial de movimientos de un producto, ordenado por fecha DESC.
     * Paginado server-side (RF16).
     * El índice {@code idx_inventario_movimiento_producto_fecha} en BD
     * hace esta query muy eficiente (Sección 4.4).
     */
    Page<InventarioMovimiento> findByProductoProductoIdOrderByCreatedAtDesc(
            Long productoId, Pageable pageable);

    /**
     * Historial filtrado por tipo de movimiento y rango de fechas (RF16).
     */
    Page<InventarioMovimiento> findByProductoProductoIdAndTipoAndCreatedAtBetween(
            Long productoId,
            InventarioMovimiento.TipoMovimiento tipo,
            LocalDateTime desde,
            LocalDateTime hasta,
            Pageable pageable);
}
