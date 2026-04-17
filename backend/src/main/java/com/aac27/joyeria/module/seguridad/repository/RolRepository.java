package com.aac27.joyeria.module.seguridad.repository;

import com.aac27.joyeria.module.seguridad.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link Rol}.
 *
 * <p>Al extender {@link JpaRepository}, Spring Data genera automáticamente
 * la implementación de todos los métodos CRUD:
 * <ul>
 *   <li>{@code findAll()} → SELECT * FROM rol</li>
 *   <li>{@code findById(id)} → SELECT * FROM rol WHERE rol_id = ?</li>
 *   <li>{@code save(rol)} → INSERT o UPDATE según si tiene ID</li>
 *   <li>{@code delete(rol)} → DELETE (evitamos esto; roles son inmutables)</li>
 * </ul>
 *
 * <p>Los métodos adicionales ({@code findByNombre}) siguen la convención
 * de nombres de Spring Data: Spring genera el SQL automáticamente.
 * No necesitamos escribir ninguna query.
 *
 * <p>Referencia: Sección 4.1 — Capa Repository.
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    /**
     * Busca un rol por su nombre (enum).
     *
     * <p>Spring Data genera: {@code SELECT * FROM rol WHERE nombre = ?}
     *
     * <p>Uso típico en {@code AuthService}: al crear un usuario nuevo,
     * buscamos su rol para asignárselo.
     *
     * @param nombre Valor del enum {@link Rol.NombreRol}
     * @return {@code Optional} con el rol si existe, vacío si no.
     *         Usamos Optional para manejar el caso "no encontrado" sin NullPointerException.
     */
    Optional<Rol> findByNombre(Rol.NombreRol nombre);
}
