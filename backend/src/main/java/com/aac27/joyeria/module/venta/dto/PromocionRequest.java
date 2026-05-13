package com.aac27.joyeria.module.venta.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de petición para crear o editar una promoción.
 * Referencia: RF-V02, Sección 3.28.
 *
 * <p>Solo el Administrador puede crear/editar promociones.
 */
@Getter
@Setter
@NoArgsConstructor
public class PromocionRequest {

    @NotBlank(message = "El nombre de la promoción es obligatorio")
    @Size(min = 3, max = 150, message = "El nombre debe tener entre 3 y 150 caracteres")
    private String nombre;

    @Size(max = 2000, message = "La descripción no puede exceder 2000 caracteres")
    private String descripcion;

    @NotBlank(message = "El tipo de descuento es obligatorio")
    @Pattern(regexp = "^(porcentaje|monto_fijo)$",
             message = "El tipo debe ser 'porcentaje' o 'monto_fijo'")
    private String tipo;

    @NotNull(message = "El valor del descuento es obligatorio")
    @DecimalMin(value = "0.01", message = "El valor debe ser mayor a 0")
    @DecimalMax(value = "9999999.99", message = "El valor excede el máximo permitido")
    private BigDecimal valor;

    @NotBlank(message = "El campo 'aplica_a' es obligatorio")
    @Pattern(regexp = "^(venta|categoria|producto)$",
             message = "Debe ser 'venta', 'categoria' o 'producto'")
    private String aplicaA;

    /**
     * ID de la entidad a la que aplica.
     * Requerido si {@code aplicaA} es 'categoria' o 'producto'.
     */
    @Positive(message = "El ID de entidad debe ser positivo")
    private Long entidadId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    /** true por defecto. Puede desactivarse sin eliminar la promoción. */
    private boolean activa = true;
}
