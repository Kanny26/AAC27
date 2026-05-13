package com.aac27.joyeria.module.venta.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad JPA para la tabla {@code promocion}.
 *
 * <p><strong>Reglas de negocio (RF-V02):</strong>
 * <ul>
 *   <li>El descuento puede ser por PORCENTAJE o MONTO FIJO.</li>
 *   <li>Tiene vigencia: {@code fechaInicio} y {@code fechaFin}.</li>
 *   <li>Puede aplicar a: toda la venta, una categoría o un producto específico.</li>
 *   <li>El descuento NO puede resultar en precio negativo (validado en Service).</li>
 * </ul>
 *
 * <p>Una promoción está ACTIVA si:
 * <ul>
 *   <li>{@code activa = true}</li>
 *   <li>La fecha actual está entre {@code fechaInicio} y {@code fechaFin} inclusive.</li>
 * </ul>
 *
 * <p>Referencia: Sección 3.28 — RF-V02.
 */
@Entity
@Table(
        name = "promocion",
        indexes = {
                @Index(name = "idx_promo_fechas", columnList = "fecha_inicio, fecha_fin"),
                @Index(name = "idx_promo_activa", columnList = "activa, fecha_inicio, fecha_fin"),
                @Index(name = "idx_promo_aplica", columnList = "aplica_a, entidad_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promocion_id", nullable = false, updatable = false)
    private Long promocionId;

    /** Nombre descriptivo. Ej: "Black Friday 20%", "Descuento Aniversario". */
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Tipo de descuento:
     * <ul>
     *   <li>{@code porcentaje}: el campo {@code valor} es un porcentaje (0-100)</li>
     *   <li>{@code monto_fijo}: el campo {@code valor} es un monto fijo en COP</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 15)
    private TipoDescuento tipo;

    /**
     * Valor del descuento.
     * Si {@code tipo = porcentaje}: número entre 0 y 100 (ej: 20.00 = 20%).
     * Si {@code tipo = monto_fijo}: monto en COP (ej: 50000.00 = $50,000).
     */
    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    /**
     * Alcance de la promoción:
     * <ul>
     *   <li>{@code venta}: aplica al total de la venta</li>
     *   <li>{@code categoria}: aplica a productos de una categoría</li>
     *   <li>{@code producto}: aplica a un producto específico</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "aplica_a", nullable = false, length = 15)
    @Builder.Default
    private AplicaA aplicaA = AplicaA.venta;

    /**
     * ID de la entidad a la que aplica.
     * NULL si {@code aplicaA = venta}.
     * ID de categoría si {@code aplicaA = categoria}.
     * ID de producto si {@code aplicaA = producto}.
     */
    @Column(name = "entidad_id")
    private Long entidadId;

    /** Fecha de inicio de la vigencia (inclusive). */
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /** Fecha de fin de la vigencia (inclusive). Debe ser >= fechaInicio. */
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    /**
     * Si la promoción está activada manualmente.
     * La vigencia real depende también de las fechas.
     */
    @Column(name = "activa", nullable = false)
    @Builder.Default
    private boolean activa = true;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = ahora;
        this.updatedAt = ahora;
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum TipoDescuento { porcentaje, monto_fijo }
    public enum AplicaA      { venta, categoria, producto }

    // ── MÉTODOS DE NEGOCIO ─────────────────────────────────────

    /**
     * Verifica si la promoción está vigente en la fecha indicada.
     *
     * @param fecha Fecha a verificar (normalmente LocalDate.now())
     * @return true si la promoción está activa y dentro de vigencia
     */
    public boolean estaVigente(LocalDate fecha) {
        return activa
                && !fecha.isBefore(fechaInicio)
                && !fecha.isAfter(fechaFin);
    }

    /**
     * Calcula el descuento en COP para un precio base dado.
     * Si el descuento resultara en precio negativo, retorna el precio base completo.
     *
     * @param precioBase Precio sobre el que aplica el descuento
     * @return Monto del descuento en COP (nunca mayor al precioBase)
     */
    public BigDecimal calcularDescuento(BigDecimal precioBase) {
        if (precioBase == null || precioBase.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal descuento;
        if (this.tipo == TipoDescuento.porcentaje) {
            descuento = precioBase.multiply(this.valor)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        } else {
            descuento = this.valor;
        }
        // El descuento nunca puede ser mayor que el precio base (no precio negativo)
        return descuento.min(precioBase);
    }
}
