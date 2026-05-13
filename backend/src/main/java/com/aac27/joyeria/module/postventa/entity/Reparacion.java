package com.aac27.joyeria.module.postventa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidad hija de CasoPostventa para gestionar reparaciones (RF-PV01).
 * 
 * <p>Mapea a la tabla 'reparacion'. Su PK (caso_id) es también la FK
 * que la une a la tabla 'caso_postventa'.
 */
@Entity
@Table(name = "reparacion")
@PrimaryKeyJoinColumn(name = "caso_id") // Indica cómo se hace el JOIN con el padre
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Reparacion extends CasoPostventa {

    @Column(name = "descripcion_trabajo", nullable = false, columnDefinition = "TEXT")
    private String descripcionTrabajo;

    @Column(name = "presupuesto", precision = 12, scale = 2)
    private BigDecimal presupuesto;

    @Column(name = "costo_real", precision = 12, scale = 2)
    private BigDecimal costoReal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_reparacion", nullable = false)
    @lombok.Builder.Default
    private EstadoReparacion estadoReparacion = EstadoReparacion.recibido;

    @Column(name = "fecha_entrega_estimada")
    private LocalDate fechaEntregaEstimada;

    public enum EstadoReparacion {
        recibido, en_proceso, listo, entregado
    }
}
