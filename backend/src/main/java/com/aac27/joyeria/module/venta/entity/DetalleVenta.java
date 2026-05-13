package com.aac27.joyeria.module.venta.entity;

import com.aac27.joyeria.module.catalogo.entity.Producto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidad JPA para la tabla {@code detalle_venta}.
 *
 * <p>
 * <strong>Precio histórico (desnormalización intencional):</strong>
 * El campo {@code precioUnitario} guarda el precio EN EL MOMENTO de la venta.
 * NO hay FK a ninguna tabla de precios. Si el producto sube de precio mañana,
 * esta venta conserva el precio original para integridad contable.
 *
 * <p>
 * <strong>Garantías (RF-V04):</strong>
 * Cada ítem puede tener garantía en meses. La fecha de vencimiento
 * se calcula en Java: {@code fechaVenta + garantiaMeses}.
 *
 * <p>
 * Referencia: Sección 3.17, RF-V04.
 */
@Entity
@Table(name = "detalle_venta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detalle_venta_id", nullable = false, updatable = false)
    private Long detalleVentaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    /** Producto vendido. La FK conserva la referencia pero NO el precio. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    /**
     * Precio en el MOMENTO de la venta. INMUTABLE. Precio histórico.
     * Se copia desde producto.precioVenta al crear el detalle.
     */
    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    /** Descuento aplicado a ESTE ítem (por promoción o manual). */
    @Column(name = "descuento_item", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal descuentoItem = BigDecimal.ZERO;

    /**
     * Precio final por unidad: precioUnitario - descuentoItem/cantidad.
     * Nunca negativo.
     */
    @Column(name = "precio_con_descuento", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioConDescuento;

    /**
     * Subtotal del ítem: cantidad × precioConDescuento.
     * Calculado en Service.
     */
    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    // ── GARANTÍA (RF-V04) ──────────────────────────────────────

    /** Duración de garantía en meses. 0 = sin garantía. */
    @Column(name = "garantia_meses", nullable = false)
    @Builder.Default
    private Integer garantiaMeses = 0;

    /**
     * Fecha de vencimiento de garantía.
     * Calculada en Java: fechaVenta.toLocalDate().plusMonths(garantiaMeses).
     * NULL si garantiaMeses = 0.
     */
    @Column(name = "garantia_vence")
    private LocalDate garantiaVence;

    @Column(name = "notas_item", length = 255)
    private String notasItem;

    /**
     * URL del certificado de autenticidad vinculado a este ítem (RF-CAT03).
     * NULL hasta que se genere el PDF en Fase 4.
     * Hook para el módulo de certificados.
     */
    @Column(name = "certificado_url", length = 500)
    private String certificadoUrl;

    /** Calcula el subtotal: cantidad × precioConDescuento */
    @Transient
    public BigDecimal calcularSubtotal() {
        return precioConDescuento.multiply(BigDecimal.valueOf(cantidad));
    }
}
