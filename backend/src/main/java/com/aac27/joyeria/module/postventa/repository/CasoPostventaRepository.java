package com.aac27.joyeria.module.postventa.repository;

import com.aac27.joyeria.module.postventa.entity.CasoPostventa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CasoPostventaRepository extends JpaRepository<CasoPostventa, Long> {

    // RF26: Buscar por número de factura asociado
    @Query("SELECT c FROM CasoPostventa c WHERE c.venta.numeroFactura = :factura")
    Page<CasoPostventa> findByVentaNumeroFactura(@Param("factura") String factura, Pageable pageable);

    // Casos asignados a un cliente específico
    Page<CasoPostventa> findByClienteClienteId(Long clienteId, Pageable pageable);

    // Casos generados por un vendedor (para que solo vea los suyos)
    Page<CasoPostventa> findByUsuarioUsuarioId(Long usuarioId, Pageable pageable);
}
