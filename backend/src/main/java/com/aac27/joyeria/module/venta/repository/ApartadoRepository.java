package com.aac27.joyeria.module.venta.repository;

import com.aac27.joyeria.module.venta.entity.Apartado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositorio para {@link Apartado}. Referencia: RF-V01. */
@Repository
public interface ApartadoRepository extends JpaRepository<Apartado, Long> {
        Page<Apartado> findByClienteClienteIdAndEstado(
                        Long clienteId, Apartado.EstadoApartado estado, Pageable pageable);

        /**
         * Para el panel de administración: todos los activos ordenados por urgencia.
         */
        Page<Apartado> findByEstadoOrderByFechaLimiteAsc(
                        Apartado.EstadoApartado estado, Pageable pageable);

        /**
         * Busca apartados activos cuya fecha límite ha pasado.
         * Usado por el Job programado de vencimiento automático.
         */
        java.util.List<Apartado> findByEstadoAndFechaLimiteBefore(
                        Apartado.EstadoApartado estado, java.time.LocalDate fecha);
}
