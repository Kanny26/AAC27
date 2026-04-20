package com.aac27.joyeria.module.compra.repository;

import com.aac27.joyeria.module.compra.entity.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * Repositorio para {@link Compra}.
 * Referencia: RF11 — Gestión de órdenes de compra.
 */
@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    /**
     * Compras de un proveedor específico, paginadas.
     * Usado en la vista de resumen histórico del proveedor (RF11).
     */
    Page<Compra> findByProveedorProveedorId(Long proveedorId, Pageable pageable);

    /**
     * Compras en un rango de fechas con filtro de estado.
     * Usado para reportes de compras por período.
     */
    @Query("SELECT c FROM Compra c WHERE " +
           "(:proveedorId IS NULL OR c.proveedor.proveedorId = :proveedorId) AND " +
           "c.fechaFactura BETWEEN :desde AND :hasta")
    Page<Compra> filtrarCompras(
            @Param("proveedorId") Long proveedorId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            Pageable pageable);

    /** Verifica si un proveedor tiene compras asociadas (para validar borrado lógico). */
    boolean existsByProveedorProveedorId(Long proveedorId);
}
