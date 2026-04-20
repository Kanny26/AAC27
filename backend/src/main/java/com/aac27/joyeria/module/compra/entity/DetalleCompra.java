package com.aac27.joyeria.module.compra.entity;

import com.aac27.joyeria.module.catalogo.entity.Producto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad JPA para la tabla {@code detalle_compra}.
 *
 * <p>Cada registro representa UN producto dentro de una orden de compra.
 * El subtotal se calcula: {@code cantidad_pedida × precio_unitario}.
 *
 * <p>El campo {@code cantidadRecibida} se actualiza en recepciones parciales
 * (RF-C03) SIN modificar la cabecera de la compra (inmutabilidad del total).
 *
 * <p>Referencia: Sección 3.15 — RF11, RF-C03.
 */
@Entity
@Table(name = "detalle_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detalle_compra_id", nullable = false, updatable = false)
    private Long detalleCompraId;

    /** Compra a la que pertenece este detalle. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    /** Producto comprado. No puede cambiar tras guardar la compra. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    /** Cantidad solicitada en la orden. Debe ser > 0. */
    @Column(name = "cantidad_pedida", nullable = false)
    private Integer cantidadPedida;

    /**
     * Cantidad efectivamente recibida.
     * Se actualiza en recepciones parciales (RF-C03).
     * Inicia en 0 al crear la orden.
     */
    @Column(name = "cantidad_recibida", nullable = false)
    @Builder.Default
    private Integer cantidadRecibida = 0;

    /**
     * Precio de costo por unidad en el momento de la compra.
     * Precio histórico — no cambia. Si el precio del producto cambia después,
     * este detalle conserva el precio original.
     */
    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    /**
     * Subtotal de esta línea.
     * {@code subtotal = cantidadPedida × precioUnitario}
     * Calculado en Service al crear el detalle.
     */
    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    /** Cantidad pendiente de recibir. */
    @Transient // No se persiste — calculado en memoria
    public Integer getCantidadPendiente() {
        return this.cantidadPedida - this.cantidadRecibida;
    }

    /** Verifica si este ítem ha sido completamente recibido. */
    @Transient
    public boolean estaCompleto() {
        return this.cantidadRecibida.equals(this.cantidadPedida);
    }
}
