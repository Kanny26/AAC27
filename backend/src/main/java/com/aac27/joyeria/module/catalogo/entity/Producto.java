package com.aac27.joyeria.module.catalogo.entity;

import com.aac27.joyeria.module.proveedor.entity.Proveedor;
import com.aac27.joyeria.shared.audit.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA para la tabla {@code producto}.
 *
 * <p><strong>Relaciones:</strong>
 * <ul>
 *   <li>{@code @ManyToOne} → {@link Categoria} (N productos : 1 categoría)</li>
 *   <li>{@code @ManyToOne} → {@link Material} (N productos : 1 material)</li>
 *   <li>{@code @ManyToOne} → {@link Proveedor} (N productos : 1 proveedor, opcional)</li>
 *   <li>{@code @ManyToMany} → {@link Subcategoria} (a través de tabla {@code producto_subcategoria})</li>
 *   <li>{@code @OneToOne} → {@link ProductoTrazabilidad} (solo si material.esTrazable=true)</li>
 * </ul>
 *
 * <p><strong>Optimistic Locking ({@code @Version}):</strong>
 * El campo {@code version} es gestionado automáticamente por Hibernate.
 * Cuando dos transacciones leen el mismo producto y ambas intentan guardar,
 * la segunda falla con {@code OptimisticLockingFailureException}.
 * Esto previene que el stock quede en estado inconsistente (RNF-REN04).
 *
 * <p>Ejemplo: dos cajeros venden el último producto simultáneamente.
 * Sin locking → ambas ventas pasan y el stock queda en -1.
 * Con @Version → la segunda falla → se reintenta → stock = 0 correcto.
 *
 * <p>Referencia: Sección 3.7, Sección 3.29 — RF-CAT01.
 */
@Entity
@Table(
        name = "producto",
        indexes = {
                @Index(name = "idx_producto_categoria", columnList = "categoria_id"),
                @Index(name = "idx_producto_estado",    columnList = "estado"),
                @Index(name = "idx_producto_codigo",    columnList = "codigo")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Producto extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "producto_id", nullable = false, updatable = false)
    private Long productoId;

    /**
     * Código SKU único generado por el sistema.
     * Formato sugerido: CAT-YYYYMM-NNNN (ej: ANI-202604-0001).
     * Inmutable tras la creación.
     */
    @Column(name = "codigo", nullable = false, unique = true,
            length = 20, updatable = false)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    /** Mín 10 chars, máx 500 chars — validado en DTO/Service. */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // ── RELACIONES @ManyToOne ──────────────────────────────────

    /**
     * Categoría principal del producto (Anillos, Collares, etc.).
     *
     * <p>{@code FetchType.LAZY}: la categoría no se carga a menos que se acceda.
     * Se carga con JOIN FETCH en las queries de catálogo que la necesitan.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    /** Material principal del producto. Determina si requiere trazabilidad. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    /** Proveedor asociado. Opcional (null si se desconoce o no aplica). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    // ── RELACIÓN @ManyToMany ───────────────────────────────────

    /**
     * Subcategorías del producto (múltiples permitidas).
     *
     * <p>{@code @ManyToMany}: un producto puede ser de múltiples subcategorías
     * y una subcategoría puede tener múltiples productos.
     *
     * <p>{@code @JoinTable}: especifica la tabla intermedia {@code producto_subcategoria}
     * y las claves foráneas que conectan ambas tablas.
     *
     * <p>{@code FetchType.LAZY}: las subcategorías se cargan solo cuando se acceden.
     *
     * <p>Usamos {@code Set} en lugar de {@code List} para evitar duplicados
     * y mejorar el rendimiento en operaciones de join (Hibernate recomienda Set para @ManyToMany).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "producto_subcategoria",
            // Columna que apunta a ESTA entidad (Producto)
            joinColumns = @JoinColumn(name = "producto_id"),
            // Columna que apunta a la entidad del OTRO lado (Subcategoria)
            inverseJoinColumns = @JoinColumn(name = "subcategoria_id")
    )
    @Builder.Default
    private Set<Subcategoria> subcategorias = new HashSet<>();

    // ── RELACIÓN @OneToOne OPCIONAL ────────────────────────────

    /**
     * Datos de trazabilidad (ley, quilates, certificado).
     * Solo presente si {@code material.esTrazable == true} (RF-CAT02).
     *
     * <p>{@code mappedBy}: la FK está en la tabla {@code producto_trazabilidad},
     * no en {@code producto}. Esto evita columna extra en la tabla producto.
     *
     * <p>{@code CascadeType.ALL}: al guardar/eliminar el producto,
     * se propaga a la trazabilidad. Sección 3.29: solo PERSIST y MERGE con precaución.
     */
    @OneToOne(mappedBy = "producto", cascade = {CascadeType.PERSIST, CascadeType.MERGE},
              fetch = FetchType.LAZY, orphanRemoval = true)
    private ProductoTrazabilidad trazabilidad;

    // ── PRECIOS Y STOCK ────────────────────────────────────────

    /**
     * Precio de costo o adquisición.
     * {@code BigDecimal}: NUNCA usar double/float para dinero (Sección 3.29).
     * {@code precision=14, scale=2} → hasta 999,999,999,999.99 COP.
     */
    @Column(name = "precio_costo", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioCosto;

    /**
     * Precio de venta al público.
     * Regla: precio_venta > precio_costo (validado en Service, RF-CAT01).
     * Regla adicional: precio_venta mínimo = precio_costo × 2 (configurable).
     */
    @Column(name = "precio_venta", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioVenta;

    /**
     * Stock disponible en inventario.
     * NUNCA negativo (CHECK en BD y validación en Service).
     */
    @Column(name = "stock", nullable = false)
    @Builder.Default
    private Integer stock = 0;

    /**
     * Umbral para alertas de stock bajo (RF-INV01).
     * Cuando stock < stockMinimo → se genera alerta al administrador.
     */
    @Column(name = "stock_minimo", nullable = false)
    @Builder.Default
    private Integer stockMinimo = 5;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    /** Para productos únicos de alto valor (joyas exclusivas). */
    @Column(name = "numero_serie", length = 100)
    private String numeroSerie;

    /** Número de lote del proveedor para trazabilidad de lotes. */
    @Column(name = "numero_lote", length = 100)
    private String numeroLote;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    @Builder.Default
    private EstadoProducto estado = EstadoProducto.activo;

    // ── OPTIMISTIC LOCKING ─────────────────────────────────────

    /**
     * Campo de control de concurrencia optimista.
     *
     * <p>Hibernate incrementa este valor automáticamente en cada UPDATE.
     * Si dos transacciones leen version=5 y ambas intentan guardar,
     * la segunda ve que en BD ya es version=6 → lanza {@code OptimisticLockingFailureException}.
     *
     * <p>El Service lo captura y puede reintentar o informar al cliente.
     * Referencia: Sección 3.29, RNF-REN04.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    public enum EstadoProducto { activo, inactivo }

    // ── MÉTODOS DE NEGOCIO ─────────────────────────────────────

    /**
     * Incrementa el stock por la cantidad indicada.
     * Llamado desde CompraService al recibir productos.
     *
     * @param cantidad Cantidad a incrementar (debe ser > 0)
     */
    public void incrementarStock(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a incrementar debe ser positiva");
        }
        this.stock += cantidad;
    }

    /**
     * Decrementa el stock por la cantidad indicada.
     * Llamado desde VentaService al confirmar una venta.
     *
     * @param cantidad Cantidad a decrementar
     * @throws IllegalStateException si el stock resultante sería negativo
     */
    public void decrementarStock(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a decrementar debe ser positiva");
        }
        if (this.stock < cantidad) {
            throw new IllegalStateException(
                    "Stock insuficiente. Disponible: " + this.stock + ", requerido: " + cantidad
            );
        }
        this.stock -= cantidad;
    }

    /** Verifica si el stock está por debajo del umbral mínimo (activa alerta RF-INV01). */
    public boolean tieneStockBajo() {
        return this.stock < this.stockMinimo;
    }
}
