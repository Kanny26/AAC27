package com.aac27.joyeria.module.venta.repository;

import com.aac27.joyeria.module.venta.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repositorio para {@link Cliente}. Referencia: RF-U02. */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByNumeroDocumento(String numeroDocumento);
    boolean existsByCorreo(String correo);

    /** Para validar unicidad al EDITAR — excluye el propio cliente. */
    boolean existsByNumeroDocumentoAndClienteIdNot(String numeroDocumento, Long clienteId);
    boolean existsByCorreoAndClienteIdNot(String correo, Long clienteId);

    @Query("SELECT c FROM Cliente c WHERE " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "c.numeroDocumento LIKE CONCAT('%', :q, '%') OR " +
           "c.telefono LIKE CONCAT('%', :q, '%')")
    Page<Cliente> buscar(@Param("q") String termino, Pageable pageable);
}
