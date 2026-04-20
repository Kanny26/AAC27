package com.aac27.joyeria.module.proveedor.entity;

import com.aac27.joyeria.module.catalogo.entity.Material;
import com.aac27.joyeria.shared.audit.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * Entidad JPA para la tabla {@code proveedor}.
 *
 * <p><strong>Relaciones:</strong>
 * <ul>
 *   <li>{@code @ManyToMany} → {@link Material} (a través de {@code proveedor_material})</li>
 *   <li>{@code @OneToMany} → {@link ProveedorTelefono} (múltiples teléfonos)</li>
 *   <li>{@code @OneToMany} → {@link ProveedorCorreo} (múltiples correos)</li>
 * </ul>
 *
 * <p>Documento (NIT/cédula) es INMUTABLE tras la creación (RF11-E).
 * No se elimina físicamente; se inactiva (borrado lógico).
 *
 * <p>Referencia: Sección 3.10 — RF09, RF10.
 */
@Entity
@Table(name = "proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proveedor_id", nullable = false, updatable = false)
    private Long proveedorId;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    /**
     * NIT o cédula del proveedor.
     * INMUTABLE tras la creación (RF11-E: "Documento e identificación interna inmutables").
     */
    @Column(name = "documento", nullable = false, unique = true,
            length = 50, updatable = false)
    private String documento;

    /**
     * Fecha de inicio de la relación comercial.
     * No puede ser futura (validada en BD y en Service).
     */
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /**
     * Monto mínimo de orden de compra en COP.
     * BigDecimal para evitar errores de punto flotante con dinero.
     */
    @Column(name = "minimo_compra", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal minimoCompra = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    @Builder.Default
    private EstadoProveedor estado = EstadoProveedor.activo;

    // ── TELÉFONOS (OneToMany) ──────────────────────────────────

    /**
     * Lista de teléfonos del proveedor.
     *
     * <p>{@code @OneToMany(mappedBy)}: un proveedor tiene muchos teléfonos.
     * {@code mappedBy = "proveedor"}: la FK está en {@link ProveedorTelefono},
     * no en esta tabla.
     *
     * <p>{@code CascadeType.ALL}: al guardar el proveedor, se guardan sus teléfonos.
     * Al eliminar (si se hiciera), se eliminan en cascada.
     *
     * <p>{@code orphanRemoval = true}: si se quita un teléfono de la lista,
     * Hibernate lo borra de la BD automáticamente.
     */
    @OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ProveedorTelefono> telefonos = new ArrayList<>();

    // ── CORREOS (OneToMany) ────────────────────────────────────

    /** Lista de correos del proveedor. Mismos principios que telefonos. */
    @OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ProveedorCorreo> correos = new ArrayList<>();

    // ── MATERIALES (ManyToMany) ────────────────────────────────

    /**
     * Materiales que suministra este proveedor.
     *
     * <p>{@code @ManyToMany}: un proveedor suministra múltiples materiales
     * y un material puede ser suministrado por múltiples proveedores.
     *
     * <p>{@code @JoinTable}: tabla intermedia {@code proveedor_material}.
     * En este lado definimos la tabla y las columnas.
     *
     * <p>Al menos UN material requerido al crear (RF10 — validado en Service).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "proveedor_material",
            joinColumns = @JoinColumn(name = "proveedor_id"),
            inverseJoinColumns = @JoinColumn(name = "material_id")
    )
    @Builder.Default
    private Set<Material> materiales = new HashSet<>();

    public enum EstadoProveedor { activo, inactivo }

    // ── MÉTODOS DE NEGOCIO ──────────────────────────────────────

    /** Agrega un teléfono y establece la referencia bidireccional. */
    public void agregarTelefono(ProveedorTelefono telefono) {
        telefono.setProveedor(this);
        this.telefonos.add(telefono);
    }

    /** Agrega un correo y establece la referencia bidireccional. */
    public void agregarCorreo(ProveedorCorreo correo) {
        correo.setProveedor(this);
        this.correos.add(correo);
    }
}
