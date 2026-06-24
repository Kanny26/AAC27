-- =============================================================
-- Flyway Migration: V7__apartados.sql
-- Descripción: Módulo de apartados/layaway:
--              apartado, abono_apartado
-- Referencia: Sección 3.19, RF-V01 del documento AAC27.
-- Depende de: V6__ventas.sql (venta), V3__productos.sql (producto)
--
-- CONCEPTO DEL APARTADO (LAYAWAY):
-- Un cliente separa un producto pagando un anticipo mínimo del 20%.
-- El stock se BLOQUEA (no se descuenta) hasta completar el pago.
-- Al completar el pago → se genera la venta definitiva y se descuenta el stock.
-- Si el cliente cancela → el stock se libera (InventarioMovimiento tipo 'liberacion').
--
-- Diferencia con venta a crédito:
-- - Crédito: el cliente se lleva el producto y paga después.
-- - Layaway: el producto permanece en tienda hasta que se paga completamente.
-- =============================================================

SET time_zone = 'America/Bogota';

-- =============================================================
-- TABLA: apartado
-- Sección 3.19 — RF-V01.
-- Reserva de producto con abonos periódicos.
-- Estado del stock: RESERVADO (no disponible para otras ventas, no entregado).
-- =============================================================
CREATE TABLE IF NOT EXISTS apartado (
    apartado_id        BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    cliente_id         BIGINT UNSIGNED  NOT NULL COMMENT 'Apartado siempre requiere cliente identificado',
    usuario_id         BIGINT UNSIGNED  NOT NULL COMMENT 'Vendedor que registra',
    producto_id        BIGINT UNSIGNED  NOT NULL,
    cantidad           INT              NOT NULL,
        CONSTRAINT chk_apar_cantidad CHECK (cantidad > 0),
    precio_unitario    DECIMAL(14,2)    NOT NULL
        COMMENT 'Precio fijado al momento del apartado. Histórico — no cambia.',
    total_a_pagar      DECIMAL(14,2)    NOT NULL,
        CONSTRAINT chk_apar_total CHECK (total_a_pagar > 0),
    total_abonado      DECIMAL(14,2)    NOT NULL DEFAULT 0.00,
        CONSTRAINT chk_apar_abonado CHECK (total_abonado >= 0),
    saldo_pendiente    DECIMAL(14,2)    NOT NULL
        COMMENT 'total_a_pagar - total_abonado. Calculado/actualizado en Java.',
    fecha_limite       DATE             NOT NULL
        COMMENT 'Fecha máxima para completar el pago. Validado en Service: > hoy.',
    estado             ENUM('activo','completado','cancelado','vencido') NOT NULL DEFAULT 'activo',
    -- Si el apartado se completa, esta FK apunta a la venta definitiva generada
    venta_id           BIGINT UNSIGNED  NULL
        COMMENT 'Populated when estado=completado. FK a la venta definitiva.',
    primer_abono_ok    TINYINT(1)       NOT NULL DEFAULT 0
        COMMENT '1 si el primer abono >= 20% del total fue validado',
    observaciones      TEXT             NULL,
    created_at         DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_apartado          PRIMARY KEY (apartado_id),
    CONSTRAINT fk_apar_cliente      FOREIGN KEY (cliente_id)
        REFERENCES cliente (cliente_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_apar_usuario      FOREIGN KEY (usuario_id)
        REFERENCES usuario (usuario_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_apar_producto     FOREIGN KEY (producto_id)
        REFERENCES producto (producto_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_apar_venta        FOREIGN KEY (venta_id)
        REFERENCES venta (venta_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_apar_saldo       CHECK (saldo_pendiente >= 0),
    CONSTRAINT chk_apar_fecha_lim   CHECK (fecha_limite >= DATE(created_at))

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Apartados/layaway: reserva con abonos. Stock bloqueado hasta completar (RF-V01).';

CREATE INDEX idx_apartado_cliente ON apartado (cliente_id);
CREATE INDEX idx_apartado_estado  ON apartado (estado);

-- =============================================================
-- TABLA: abono_apartado
-- Historial de pagos realizados al apartado.
-- Cada abono actualiza total_abonado y saldo_pendiente en la tabla apartado.
-- El primer abono debe ser >= 20% del total (validado en Java).
-- =============================================================
CREATE TABLE IF NOT EXISTS abono_apartado (
    abono_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    apartado_id    BIGINT UNSIGNED NOT NULL,
    metodo_pago_id BIGINT UNSIGNED NOT NULL,
    monto          DECIMAL(14,2)   NOT NULL,
        CONSTRAINT chk_abono_apar_monto CHECK (monto > 0),
    es_primer_abono TINYINT(1)     NOT NULL DEFAULT 0
        COMMENT '1 si este es el abono inicial (debe ser >= 20% del total)',
    referencia     VARCHAR(100)    NULL COMMENT 'Número de referencia de pago',
    fecha_abono    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado         ENUM('registrado','anulado') NOT NULL DEFAULT 'registrado',

    CONSTRAINT pk_abono_apartado    PRIMARY KEY (abono_id),
    CONSTRAINT fk_abono_apar_apar   FOREIGN KEY (apartado_id)
        REFERENCES apartado (apartado_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_abono_apar_metpag FOREIGN KEY (metodo_pago_id)
        REFERENCES metodo_pago (metodo_pago_id)
        ON UPDATE CASCADE ON DELETE RESTRICT

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Abonos al apartado. Primer abono >= 20% del total requerido (RF-V01).';

CREATE INDEX idx_abono_apartado ON abono_apartado (apartado_id);
