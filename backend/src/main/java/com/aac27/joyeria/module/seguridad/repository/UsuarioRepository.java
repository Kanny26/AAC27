package com.aac27.joyeria.module.seguridad.repository;

import com.aac27.joyeria.module.seguridad.entity.Rol;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link Usuario}.
 *
 * <p>Extiende {@link JpaRepository} que provee automáticamente:
 * save, findById, findAll, delete, count, existsById, etc.
 *
 * <p>Los métodos definidos aquí siguen dos patrones:
 * <ol>
 *   <li><strong>Derived queries</strong>: Spring genera el SQL del nombre del método.
 *       Ejemplo: {@code findByCorreo} → {@code SELECT * FROM usuario WHERE correo = ?}</li>
 *   <li><strong>JPQL queries</strong>: escritas con {@code @Query} en sintaxis JPQL
 *       (orientada a objetos, no SQL puro) para consultas más complejas.</li>
 * </ol>
 *
 * <p>Referencia: Sección 4.1 — Capa Repository.
 * <br>Índice recomendado: {@code idx_usuario_correo} en Sección 4.4.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // ============================================================
    // BÚSQUEDAS PARA AUTENTICACIÓN
    // ============================================================

    /**
     * Busca un usuario por su correo electrónico.
     *
     * <p>Consulta generada: {@code SELECT * FROM usuario WHERE correo = ?}
     * <p>Uso principal: {@code UserDetailsService.loadUserByUsername(correo)} (Paso 4).
     * <p>El LEFT JOIN FETCH carga el rol en la misma query, evitando una
     * segunda consulta (N+1 problem). Necesario porque getAuthorities() accede al rol.
     *
     * @param correo Correo electrónico del usuario
     * @return Optional con el usuario y su rol cargado, vacío si no existe
     */
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.rol WHERE u.correo = :correo")
    Optional<Usuario> findByCorreoWithRol(@Param("correo") String correo);

    /**
     * Verifica si ya existe un usuario con ese correo (para validación al crear).
     *
     * <p>Consulta generada: {@code SELECT COUNT(*) > 0 FROM usuario WHERE correo = ?}
     * <p>Más eficiente que {@code findByCorreo().isPresent()} porque no carga el objeto completo.
     *
     * @param correo Correo a verificar
     * @return {@code true} si el correo ya está registrado
     */
    boolean existsByCorreo(String correo);

    /**
     * Verifica si ya existe un usuario con ese número de documento (para validación al crear).
     *
     * @param numeroDocumento Número de documento a verificar
     * @return {@code true} si el documento ya está registrado
     */
    boolean existsByNumeroDocumento(String numeroDocumento);

    // ============================================================
    // LISTADO Y FILTROS (RF07: Listar Usuarios)
    // ============================================================

    /**
     * Retorna todos los usuarios con un estado específico, paginado.
     *
     * <p>Uso: filtrar por "activo" o "inactivo" en el panel de administración.
     * <p>{@link Pageable} controla: página actual, tamaño, y campo de ordenamiento.
     *
     * @param estado  Estado del usuario a filtrar
     * @param pageable Configuración de paginación y ordenamiento
     * @return Página de usuarios con el estado indicado
     */
    Page<Usuario> findByEstado(Usuario.EstadoUsuario estado, Pageable pageable);

    /**
     * Retorna todos los usuarios con un rol específico, paginado.
     *
     * @param rol     Rol a filtrar
     * @param pageable Configuración de paginación
     * @return Página de usuarios con ese rol
     */
    Page<Usuario> findByRol(Rol rol, Pageable pageable);

    /**
     * Búsqueda por nombre o correo con LIKE (búsqueda parcial).
     *
     * <p>Equivale a: {@code WHERE nombre LIKE '%termino%' OR correo LIKE '%termino%'}
     * <p>El {@code %:termino%} en JPQL funciona igual que en SQL.
     *
     * <p>Uso: barra de búsqueda en RF07 — "Búsqueda por nombre, correo, rol".
     *
     * @param termino  Texto a buscar
     * @param pageable Paginación
     * @return Página de usuarios que coinciden con el término
     */
    @Query("SELECT u FROM Usuario u WHERE " +
            "LOWER(u.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
            "LOWER(u.correo) LIKE LOWER(CONCAT('%', :termino, '%'))")
    Page<Usuario> buscarPorNombreOCorreo(@Param("termino") String termino, Pageable pageable);

    // ============================================================
    // ACTUALIZACIÓN DE ÚLTIMO ACCESO
    // ============================================================

    /**
     * Actualiza la fecha de último acceso del usuario al hacer login.
     *
     * <p>{@code @Modifying}: necesario para queries de UPDATE/DELETE en JPQL.
     * <p>{@code @Transactional}: esta query modifica datos; debe estar en una transacción.
     *    Al estar en el repositorio, el servicio que la llame debe abrir la transacción.
     *
     * <p>Referencia: RF02 — "ultimo_acceso: Timestamp del último inicio de sesión exitoso."
     *
     * @param usuarioId  ID del usuario
     * @param ultimoAcceso Fecha y hora del login
     */
    @Modifying
    @Query("UPDATE Usuario u SET u.ultimoAcceso = :ultimoAcceso WHERE u.usuarioId = :usuarioId")
    void actualizarUltimoAcceso(@Param("usuarioId") Long usuarioId,
                                 @Param("ultimoAcceso") LocalDateTime ultimoAcceso);
}
