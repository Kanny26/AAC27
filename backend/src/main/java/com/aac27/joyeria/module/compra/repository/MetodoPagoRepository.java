package com.aac27.joyeria.module.compra.repository;

import com.aac27.joyeria.module.compra.entity.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repositorio para {@link MetodoPago}. Referencia: RF28. */
@Repository
public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Long> {
    List<MetodoPago> findByEstado(MetodoPago.EstadoMetodo estado);
}
