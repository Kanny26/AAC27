package com.aac27.joyeria.module.catalogo.repository;

import com.aac27.joyeria.module.catalogo.entity.Subcategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repositorio para {@link Subcategoria}. Referencia: RF13. */
@Repository
public interface SubcategoriaRepository extends JpaRepository<Subcategoria, Long> {
    boolean existsByNombre(String nombre);
}
