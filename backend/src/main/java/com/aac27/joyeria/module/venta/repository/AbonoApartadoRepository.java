package com.aac27.joyeria.module.venta.repository;

import com.aac27.joyeria.module.venta.entity.AbonoApartado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repositorio para {@link AbonoApartado}. Referencia: RF-V01. */
@Repository
public interface AbonoApartadoRepository extends JpaRepository<AbonoApartado, Long> {

    List<AbonoApartado> findByApartadoApartadoIdOrderByFechaAbonoDesc(Long apartadoId);
}
