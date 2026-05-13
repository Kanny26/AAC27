package com.aac27.joyeria.module.compra.repository;

import com.aac27.joyeria.module.catalogo.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio extendido de Producto para operaciones con lock explícito.
 *
 * <p>Se usa en CompraService para actualizar stock con Pessimistic Locking.
 * Separado del CatalogoProductoRepository para no mezclar responsabilidades.
 *
 * <p><strong>¿Qué genera {@code @Lock(PESSIMISTIC_WRITE)}?</strong>
 * <pre>SELECT * FROM producto WHERE producto_id = ? FOR UPDATE</pre>
 * MySQL bloquea la FILA hasta el final de la transacción.
 * Ninguna otra transacción puede leer/modificar esa fila hasta liberar el lock.
 */
@Repository
public interface ProductoLockRepository extends JpaRepository<Producto, Long> {

    /**
     * Carga un producto con bloqueo exclusivo de fila (Pessimistic Write Lock).
     * Genera: {@code SELECT * FROM producto WHERE producto_id = ? FOR UPDATE}
     *
     * <p>Usado en {@code CompraService} para actualizar stock de forma segura.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.productoId = :id")
    Optional<Producto> findByIdWithLock(@Param("id") Long id);
}
