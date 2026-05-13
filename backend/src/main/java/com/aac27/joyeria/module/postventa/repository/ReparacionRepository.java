package com.aac27.joyeria.module.postventa.repository;

import com.aac27.joyeria.module.postventa.entity.Reparacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReparacionRepository extends JpaRepository<Reparacion, Long> {

    // Filtrar por estado de la reparación (en proceso, listo, etc)
    Page<Reparacion> findByEstadoReparacion(Reparacion.EstadoReparacion estado, Pageable pageable);
}
