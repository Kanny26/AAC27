package com.aac27.joyeria.module.venta.repository;

import com.aac27.joyeria.module.venta.entity.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repositorio para {@link Venta}. Referencia: RF17-RF21. */
@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    /**
     * Cuenta ventas del año actual para generar número de factura consecutivo.
     * El LIKE filtra por prefijo VTA-{anio}-.
     */
    @Query("SELECT COUNT(v) FROM Venta v WHERE v.numeroFactura LIKE CONCAT('VTA-', :anio, '-%')")
    long contarVentasDelAnio(@Param("anio") String anio);

    /** Ventas de un cliente específico, paginadas. */
    Page<Venta> findByClienteClienteIdOrderByFechaVentaDesc(Long clienteId, Pageable pageable);

    /** Ventas de un vendedor, paginadas. */
    Page<Venta> findByUsuarioUsuarioIdOrderByFechaVentaDesc(Long usuarioId, Pageable pageable);

    /**
     * Búsqueda avanzada de ventas (RF17).
     */
    @Query("SELECT v FROM Venta v WHERE " +
           "(:clienteId IS NULL OR v.cliente.clienteId = :clienteId) AND " +
           "(:usuarioId IS NULL OR v.usuario.usuarioId = :usuarioId) AND " +
           "(:fechaInicio IS NULL OR v.fechaVenta >= :fechaInicio) AND " +
           "(:fechaFin IS NULL OR v.fechaVenta <= :fechaFin)")
    Page<Venta> buscarVentas(
            @Param("clienteId") Long clienteId,
            @Param("usuarioId") Long usuarioId,
            @Param("fechaInicio") java.time.LocalDateTime fechaInicio,
            @Param("fechaFin") java.time.LocalDateTime fechaFin,
            Pageable pageable);
}
