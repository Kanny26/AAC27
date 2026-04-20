package com.aac27.joyeria.module.compra.repository;

import com.aac27.joyeria.module.compra.entity.CreditoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para {@link CreditoCompra}.
 * Referencia: RF-C01, RF29-A.
 */
@Repository
public interface CreditoCompraRepository extends JpaRepository<CreditoCompra, Long> {

    /** Busca el crédito asociado a una compra. */
    Optional<CreditoCompra> findByCompraCompraId(Long compraId);

    /** Verifica si una compra ya tiene crédito (para evitar duplicados). */
    boolean existsByCompraCompraId(Long compraId);
}
