package com.aac27.joyeria.module.seguridad.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Hash de una contraseña que tuvo un usuario (RF-S02).
 *
 * <p>Solo se inserta, nunca se modifica. Se guarda el id del usuario como número y no como
 * relación JPA: para comparar hashes no hace falta cargar al usuario.
 */
@Entity
@Table(name = "usuario_password_historial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historial_id", nullable = false, updatable = false)
    private Long historialId;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(name = "password_hash", nullable = false, updatable = false, length = 255)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void alCrear() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}