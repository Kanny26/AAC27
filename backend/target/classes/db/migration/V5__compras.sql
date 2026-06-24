-- =============================================================
-- Flyway Migration: V5__compras.sql
-- Descripción: Módulo completo de compras:
--              compra, detalle_compra, credito_compra, abono_credito_compra
-- Referencia: Secciones 3.14, 3.15, 3.24, 3.25 del documento AAC27.
--             RF11, RF-C01, RF-C02, RF-C03.
-- Depende de: V3__productos.sql (producto), V2__catalogo_base.sql (proveedor, metodo_pago)
--
-- REGLA DE NEGOCIO CRÍTICA (RF11):
-- "Una vez guardada la compra no se puede modificar (integridad contable)."
-- Esta regla se enforce en Java (Service), no en BD.
-- =============================================================

SET time_zone = 'America/Bogota';

-- =============================================================
-- TABLA: compra
-- Sección 3.14 — RF11.
-- Cabecera de la orden de compra a un proveedor.
-- Inmutable una vez guardada (RF11 — validado en Service).
-- =============================================================
CREATE TABLE IF NOT EXISTS compra (
    compra_id               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    proveedor_id            BIGINT UNSIGNED  NOT NULL,
    usuario_id              BIGINT UNSIGNED  NOT NULL COMMENT 'Administrador que registró',
    fecha_factura           DATE             NOT NULL
        COMMENT 'Validado en Java: no puede ser futura',
    fecha_entrega_esperada  DATE             NULL
        COMMENT 'Validado en Java: >= fecha_factura',
    fecha_recepcion_real    DATE             NULL,
    metodo_pago_id          BIGINT UNSIGNED  NOT NULL,
    tipo_pago               ENUM('contado','credito') NOT NULL,
    subtotal                DECIMAL(14,2)    NOT NULL DEFAULT 0.00,
        CONSTRAINT chk_compra_subtotal CHECK (subtotal >= 0),
    total                   DECIMAL(14,2)    NOT NULL DEFAULT 0.00,
        CONSTRAINT chk_compra_total CHECK (total >= 0),
    estado                  ENUM('pendiente','recibido_parcial','recibido_completo','cancelada')
                            NOT NULL DEFAULT 'pendiente',
    notas                   TEXT             NULL,
    created_at              DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_compra        PRIMARY KEY (compra_id),
    CONSTRAINT fk_compra_prov   FOREIGN KEY (proveedor_id)
        REFERENCES proveedor (proveedor_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_compra_user   FOREIGN KEY (usuario_id)
        REFERENCES usuario (usuario_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_compra_metpag FOREIGN KEY (metodo_pago_id)
        REFERENCES metodo_pago (metodo_pago_id)
        ON UPDATE CASCADE ON DELETE RESTRICT

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Órdenes de compra a proveedores (RF11). Inmutables una vez creadas.';

CREATE INDEX idx_compra_proveedor ON compra (proveedor_id);
CREATE INDEX idx_compra_fecha     ON compra (fecha_factura);

-- =============================================================
-- TABLA: detalle_compra
-- Sección 3.15 — RF11, RF-C03.
-- Líneas de detalle de cada orden de compra.
-- cantidad_recibida se actualiza en recepciones parciales (RF-C03).
-- =============================================================
CREATE TABLE IF NOT EXISTS detalle_compra (
    detalle_compra_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    compra_id           BIGINT UNSIGNED NOT NULL,
    producto_id         BIGINT UNSIGNED NOT NULL,
    cantidad_pedida     INT             NOT NULL,
        CONSTRAINT chk_det_cantidad_pedida CHECK (cantidad_pedida > 0),
    cantidad_recibida   INT             NOT NULL DEFAULT 0,
        CONSTRAINT chk_det_cantidad_recibida CHECK (cantidad_recibida >= 0),
    precio_unitario     DECIMAL(14,2)   NOT NULL,
        CONSTRAINT chk_det_precio CHECK (precio_unitario >= 0),
    subtotal            DECIMAL(14,2)   NOT NULL
        COMMENT 'cantidad_pedida × precio_unitario. Calculado en Java.',

    CONSTRAINT pk_detalle_compra    PRIMARY KEY (detalle_compra_id),
    CONSTRAINT fk_det_compra        FOREIGN KEY (compra_id)
        REFERENCES compra (compra_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_det_producto      FOREIGN KEY (producto_id)
        REFERENCES producto (producto_id)
        ON UPDATE CASCADE ON DELETE RESTRICT

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Líneas de detalle de cada orden de compra (RF11).';

CREATE INDEX idx_detalle_compra_compra   ON detalle_compra (compra_id);
CREATE INDEX idx_detalle_compra_producto ON detalle_compra (producto_id);

-- =============================================================
-- TABLA: credito_compra
-- Sección 3.24 — RF-C01, RF29-A.
-- Registro de la deuda cuando la compra es a crédito.
-- Una compra → máximo un crédito activo simultáneo.
-- =============================================================
CREATE TABLE IF NOT EXISTS credito_compra (
    credito_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    compra_id        BIGINT UNSIGNED NOT NULL,
    monto_total      DECIMAL(14,2)   NOT NULL,
        CONSTRAINT chk_cred_monto CHECK (monto_total > 0),
    saldo_pendiente  DECIMAL(14,2)   NOT NULL,
        CONSTRAINT chk_cred_saldo CHECK (saldo_pendiente >= 0),
    fecha_inicio     DATE            NOT NULL,
    fecha_vencimiento DATE           NOT NULL
        COMMENT 'Validado en Java: > fecha_inicio',
    estado           ENUM('activo','pagado','vencido') NOT NULL DEFAULT 'activo',

    CONSTRAINT pk_credito_compra    PRIMARY KEY (credito_id),
    -- Una compra solo puede tener UN crédito activo (RF-C01)
    CONSTRAINT uq_credito_compra    UNIQUE (compra_id),
    CONSTRAINT fk_credito_compra    FOREIGN KEY (compra_id)
        REFERENCES compra (compra_id)
        ON UPDATE CASCADE ON DELETE RESTRICT

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Crédito generado al registrar compra a crédito (RF-C01). 1 compra : 1 crédito.';

-- =============================================================
-- TABLA: abono_credito_compra
-- Sección 3.25 — RF-C02, RF29-B.
-- Pagos parciales o totales a créditos de compra activos.
-- =============================================================
CREATE TABLE IF NOT EXISTS abono_credito_compra (
    abono_id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    credito_id      BIGINT UNSIGNED NOT NULL,
    metodo_pago_id  BIGINT UNSIGNED NOT NULL,
    monto           DECIMAL(14,2)   NOT NULL,
        CONSTRAINT chk_abono_monto CHECK (monto > 0),
    fecha_abono     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado          ENUM('registrado','anulado') NOT NULL DEFAULT 'registrado',

    CONSTRAINT pk_abono_credito     PRIMARY KEY (abono_id),
    CONSTRAINT fk_abono_credito     FOREIGN KEY (credito_id)
        REFERENCES credito_compra (credito_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_abono_metpag      FOREIGN KEY (metodo_pago_id)
        REFERENCES metodo_pago (metodo_pago_id)
        ON UPDATE CASCADE ON DELETE RESTRICT

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Abonos a créditos de compra (RF-C02). Solo créditos activos admiten abonos.';

CREATE INDEX idx_abono_credito ON abono_credito_compra (credito_id);
