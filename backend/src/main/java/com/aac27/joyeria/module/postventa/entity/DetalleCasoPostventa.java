package com.aac27.joyeria.module.postventa.entity;

import com.aac27.joyeria.module.catalogo.entity.Producto;
import jakarta.persistence.*;
import lombok.*;

/**
 * Representa los productos específicos devueltos o cambiados en un caso.
 */
@Entity
@Table(name = "detalle_caso_postventa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleCasoPostventa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detalle_caso_id")
    private Long detalleCasoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "caso_id", nullable = false)
    private CasoPostventa casoPostventa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "motivo", nullable = false, length = 255)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_producto", nullable = false)
    private EstadoDevolucion estadoProducto;

    public enum EstadoDevolucion {
        bueno, defectuoso
    }
}
