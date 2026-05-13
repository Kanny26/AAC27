package com.aac27.joyeria.module.venta.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA para la tabla {@code cliente}.
 *
 * <p>Clientes del negocio con CRM básico y programa de fidelidad (RF-U02, RF-V03).
 * El campo {@code puntosFidelidad} es el saldo acumulado:
 * <ul>
 *   <li>Se SUMA cuando completa una venta (+puntosGanados)</li>
 *   <li>Se RESTA cuando canjea en una venta (-puntosFidelidadUsados)</li>
 * </ul>
 *
 * <p>Referencia: Sección 3.3, RF-U02, RF-V03.
 */
@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cliente_id", nullable = false, updatable = false)
    private Long clienteId;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "numero_documento", unique = true, length = 20)
    private String numeroDocumento;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "correo", unique = true, length = 255)
    private String correo;

    @Column(name = "direccion", length = 500)
    private String direccion;

    @Column(name = "fecha_nacimiento")
    private java.time.LocalDate fechaNacimiento;

    /**
     * Saldo de puntos acumulados por el programa de fidelidad (RF-V03).
     * 1 punto = 100 COP de descuento en compras.
     * 1 punto se gana por cada 10,000 COP en compras.
     */
    @Column(name = "puntos_fidelidad", nullable = false)
    @Builder.Default
    private Integer puntosFidelidad = 0;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

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

    // ── MÉTODOS DE NEGOCIO (RF-V03) ──────────────────────────

    /**
     * Agrega puntos al saldo de fidelidad del cliente.
     * Llamado al confirmar una venta.
     */
    public void sumarPuntos(int puntos) {
        if (puntos > 0) this.puntosFidelidad += puntos;
    }

    /**
     * Resta puntos canjeados en una venta.
     *
     * @throws IllegalStateException si no tiene saldo suficiente
     */
    public void canjearPuntos(int puntos) {
        if (puntos <= 0) return;
        if (puntos > this.puntosFidelidad) {
            throw new IllegalStateException(
                    "El cliente no tiene suficientes puntos. Disponible: "
                    + this.puntosFidelidad + ", solicitado: " + puntos);
        }
        this.puntosFidelidad -= puntos;
    }
}
