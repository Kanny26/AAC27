package com.aac27.joyeria.module.catalogo.repository;

import com.aac27.joyeria.module.catalogo.entity.ProductoTrazabilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio para la trazabilidad de productos.
 */
@Repository
public interface ProductoTrazabilidadRepository extends JpaRepository<ProductoTrazabilidad, Long> {
}
