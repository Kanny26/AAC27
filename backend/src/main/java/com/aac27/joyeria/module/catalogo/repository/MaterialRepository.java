package com.aac27.joyeria.module.catalogo.repository;

import com.aac27.joyeria.module.catalogo.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repositorio para {@link Material}. Referencia: RF14. */
@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {
    boolean existsByNombre(String nombre);
    List<Material> findByEstado(Material.EstadoMaterial estado);
}
