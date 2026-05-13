package com.aac27.joyeria.module.postventa.entity;

import com.aac27.joyeria.module.seguridad.entity.Usuario;
import com.aac27.joyeria.module.venta.entity.Cliente;
import com.aac27.joyeria.module.venta.entity.Venta;
import com.aac27.joyeria.shared.audit.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad base para todos los casos de postventa.
 * 
 * <p>Estrategia JOINED: Esta clase mapea a 'caso_postventa'.
 * Las clases hijas (ej. Reparacion) tendrán su propia tabla ('reparacion')
 * vinculada por caso_id.
 */
@Entity
@Table(name = "caso_postventa")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // Usamos SuperBuilder en lugar de Builder por la herencia
public class CasoPostventa extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "caso_id")
    private Long casoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario; // Vendedor o admin que abre el caso

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoCaso tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoCaso estado;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "respuesta_admin", columnDefinition = "TEXT")
    private String respuestaAdmin;

    @Column(name = "fecha_apertura", nullable = false)
    @lombok.Builder.Default
    private LocalDateTime fechaApertura = LocalDateTime.now();

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    // Relaciones Cascade para Detalles e Historial
    @OneToMany(mappedBy = "casoPostventa", cascade = CascadeType.ALL, orphanRemoval = true)
    @lombok.Builder.Default
    private List<DetalleCasoPostventa> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "casoPostventa", cascade = CascadeType.ALL, orphanRemoval = true)
    @lombok.Builder.Default
    private List<HistorialCaso> historial = new ArrayList<>();

    public void agregarDetalle(DetalleCasoPostventa detalle) {
        detalles.add(detalle);
        detalle.setCasoPostventa(this);
    }

    public void agregarHistorial(HistorialCaso registro) {
        historial.add(registro);
        registro.setCasoPostventa(this);
    }

    public enum TipoCaso {
        reclamo, devolucion, cambio, reparacion
    }

    public enum EstadoCaso {
        abierto, en_revision, aprobado, rechazado, cerrado
    }
}
