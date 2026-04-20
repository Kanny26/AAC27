package com.aac27.joyeria.module.catalogo.repository;

import com.aac27.joyeria.module.catalogo.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para {@link Categoria}.
 * Referencia: RF12 — CRUD de categorías.
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /** Verifica nombre duplicado antes de crear/editar (RF12: "Nombre único"). */
    boolean existsByNombre(String nombre);

    /** Busca por nombre exacto para validaciones. */
    Optional<Categoria> findByNombre(String nombre);

    /** Cuenta productos activos en una categoría (para validar desactivación RF12). */
    // Esta query se implementa desde el lado de ProductoRepository.
    // Aquí solo tenemos las queries propias de Categoria.
}
