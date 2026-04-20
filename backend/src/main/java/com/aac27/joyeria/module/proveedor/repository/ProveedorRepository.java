package com.aac27.joyeria.module.proveedor.repository;

import com.aac27.joyeria.module.proveedor.entity.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para {@link Proveedor}.
 * Referencia: RF09, RF10 — Listado y registro de proveedores.
 */
@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    /** Documento NIT/cédula único — validación antes de crear (RF10). */
    boolean existsByDocumento(String documento);

    /**
     * Listado paginado con JOIN FETCH de teléfonos y correos.
     * Evita N+1 problema al mostrar la lista (RF09).
     * Carga proveedor + telefonos + correos en 1 query.
     */
    @Query("SELECT DISTINCT p FROM Proveedor p " +
           "LEFT JOIN FETCH p.telefonos " +
           "LEFT JOIN FETCH p.correos " +
           "WHERE p.estado = :estado")
    Page<Proveedor> findByEstadoWithContactos(
            @Param("estado") Proveedor.EstadoProveedor estado,
            Pageable pageable);

    /**
     * Búsqueda por nombre o material suministrado (RF09).
     * Usa JOIN con la tabla intermedia proveedor_material.
     */
    @Query("SELECT DISTINCT p FROM Proveedor p " +
           "JOIN p.materiales m WHERE " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(m.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))")
    Page<Proveedor> buscarPorNombreOMaterial(
            @Param("termino") String termino,
            Pageable pageable);
}
