package com.aac27.joyeria.module.compra.entity;

import com.aac27.joyeria.module.catalogo.entity.Producto;
import com.aac27.joyeria.module.seguridad.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA para la tabla {@code inventario_movimiento}.
 *
 * <p>Registro INMUTABLE de todos los movimientos de stock.
 * Cada vez que el stock de un producto cambia (compra, venta, ajuste,
 * reserva, liberación), se crea un registro aquí.
 *
 * <p>Los campos {@code stockAntes} y {@code stockDespues} se almacenan
 * para evitar recalcular el historial (desnormalización intencional — Sección 3.29).
 * Esto permite auditar el stock en cualquier momento sin recorrer todos los movimientos.
 *
 * <p>Referencia: Sección 3.22 — RF15, RF16.
 */
@Entity
@Table(
        name = "inventario_movimiento",
        indexes = {
                @Index(name = "idx_inventario_movimiento_producto_fecha",
                       columnList = "producto_id, created_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movimiento_id", nullable = false, updatable = false)
    private Long movimientoId;

    /** Producto al que pertenece este movimiento. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    /**
     * Tipo de movimiento:
     * <ul>
     *   <li>{@code entrada}: stock incrementado (compra recibida)</li>
     *   <li>{@code salida}: stock decrementado (venta confirmada)</li>
     *   <li>{@code ajuste}: ajuste manual por mérma o error</li>
     *   <li>{@code reserva}: stock bloqueado temporalmente (apartado)</li>
     *   <li>{@code liberacion}: reserva cancelada — stock liberado</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 15)
    private TipoMovimiento tipo;

    /**
     * Cantidad del movimiento.
     * Positivo para entradas, negativo para salidas.
     * CHECK cantidad != 0 en BD.
     */
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    /** Stock ANTES del movimiento. Para auditoría y reconstrucción del historial. */
    @Column(name = "stock_antes", nullable = false)
    private Integer stockAntes;

    /** Stock DESPUÉS del movimiento. Siempre ≥ 0. */
    @Column(name = "stock_despues", nullable = false)
    private Integer stockDespues;

    /**
     * Tipo del documento que originó el movimiento.
     * Valores: "venta", "compra", "ajuste", "apartado" (Sección 3.22).
     */
    @Column(name = "referencia_tipo", nullable = false, length = 50)
    private String referenciaTipo;

    /** ID del documento origen (ID de la venta, compra, ajuste, etc.). */
    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;

    /** Usuario que generó el movimiento (vendedor, administrador o sistema). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Motivo del movimiento.
     * OBLIGATORIO para ajustes manuales (mínimo 20 caracteres — validado en Service).
     */
    @Column(name = "motivo", length = 255)
    private String motivo;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public enum TipoMovimiento { entrada, salida, ajuste, reserva, liberacion }
}
