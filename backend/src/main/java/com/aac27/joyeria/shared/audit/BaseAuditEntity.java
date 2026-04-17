package com.aac27.joyeria.shared.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Clase base de auditoría para todas las entidades JPA que necesiten
 * registrar automáticamente su fecha de creación y última modificación.
 *
 * <p>Uso: Extender esta clase en cualquier @Entity que tenga campos
 * {@code created_at} y {@code updated_at} en la base de datos.
 *
 * <p>Requiere {@code @EnableJpaAuditing} en la clase principal ({@code JoyeriaApplication}).
 * Spring intercepta cada save/update y rellena automáticamente las fechas.
 *
 * <p>Referencia: Sección 3.29 del documento de arquitectura:
 * "Auditoría automática: usar @CreatedDate y @LastModifiedDate de Spring Data con @EnableJpaAuditing"
 */
@Getter
@Setter
// @MappedSuperclass: le dice a JPA que esta clase NO es una tabla propia,
// sino que sus campos se "heredan" a las entidades hijas.
@MappedSuperclass
// @EntityListeners: activa el listener de Spring que rellena las fechas automáticamente.
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    /**
     * Fecha y hora de creación del registro.
     * Spring la rellena automáticamente la primera vez que se guarda la entidad.
     * {@code updatable = false}: nunca se modifica después de la creación.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "DATETIME")
    private LocalDateTime createdAt;

    /**
     * Fecha y hora de la última modificación del registro.
     * Spring la actualiza automáticamente cada vez que se guarda la entidad.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "DATETIME")
    private LocalDateTime updatedAt;
}
