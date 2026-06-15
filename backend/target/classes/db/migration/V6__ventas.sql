-- =============================================================
-- Flyway Migration: V6__ventas.sql
-- Descripción: Módulo completo de ventas:
--              venta, detalle_venta, pago_venta, garantia
-- Referencia: Secciones 3.16, 3.17, 3.18, 3.20 del documento AAC27.
--             RF17-RF21, RF-V03, RF-V04.
-- Depende de: V3__productos.sql, V2__catalogo_base.sql (cliente, metodo_pago)
--
-- CONCEPTOS CLAVE DE DISEÑO:
-- 1. Precio histórico: detalle_venta guarda el precio en el momento de venta,
--    NO usa una FK al precio actual del producto. Esto es desnormalización intencional
--    (Sección 3.29) para integridad contable.
-- 2. numero_factura: generado secuencialmente por año (ej: VTA-2026-00001).
--    Implementado con un campo AUTO_INCREMENT en tabla auxiliar o generado en Java.
-- 3. puntos_fidelidad_usados: los puntos canjeados en ESTA venta (historial).
-- =============================================================

SET time_zone = 'America/Bogota';

-- =============================================================
-- TABLA: venta
-- Sección 3.16 — RF17-RF21.
-- Cabecera de cada transacción de venta.
-- =============================================================
CREATE TABLE IF NOT EXISTS venta (
    venta_id               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    numero_factura         VARCHAR(20)      NOT NULL COMMENT 'Ej: VTA-2026-00001. Generado en Java.',
    cliente_id             BIGINT UNSIGNED  NULL     COMMENT 'NULL = venta a cliente ocasional (RF17)',
    usuario_id             BIGINT UNSIGNED  NOT NULL COMMENT 'Vendedor que registra la venta',
    fecha_venta            DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modalidad_pago         ENUM('contado','anticipo','credito','layaway') NOT NULL,
    estado                 ENUM('pendiente','confirmada','entregada','cancelada')
                           NOT NULL DEFAULT 'confirmada',
    -- Estado del pago (Sección 3.16): si el total ha sido cancelado, está pendiente o vencido
    estado_pago            ENUM('pagado','pendiente','vencido')
                           NOT NULL DEFAULT 'pendiente',
    subtotal               DECIMAL(14,2)    NOT NULL DEFAULT 0.00
        CONSTRAINT chk_venta_subtotal CHECK (subtotal >= 0),
    descuento_total        DECIMAL(14,2)    NOT NULL DEFAULT 0.00
        CONSTRAINT chk_venta_descuento CHECK (descuento_total >= 0),
    puntos_fidelidad_usados INT UNSIGNED    NOT NULL DEFAULT 0
        COMMENT 'Puntos canjeados en esta venta. 1 punto = 100 COP descuento (RF-V03)',
    descuento_puntos       DECIMAL(14,2)    NOT NULL DEFAULT 0.00
        COMMENT 'Descuento en pesos generado por canje de puntos',
    total                  DECIMAL(14,2)    NOT NULL DEFAULT 0.00
        CONSTRAINT chk_venta_total CHECK (total >= 0),
    puntos_ganados         INT UNSIGNED     NOT NULL DEFAULT 0
        COMMENT '1 punto por cada 10,000 COP en compras (RF-V03)',
    observaciones          TEXT             NULL,
    created_at             DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_venta             PRIMARY KEY (venta_id),
    CONSTRAINT uq_venta_factura     UNIQUE (numero_factura),
    CONSTRAINT fk_venta_cliente     FOREIGN KEY (cliente_id)
        REFERENCES cliente (cliente_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_venta_usuario     FOREIGN KEY (usuario_id)
        REFERENCES usuario (usuario_id)
        ON UPDATE CASCADE ON DELETE RESTRICT

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Ventas del negocio (RF17-RF21). numero_factura único y consecutivo por año.';

-- Índices de rendimiento (Sección 4.4)
CREATE INDEX idx_venta_cliente       ON venta (cliente_id);
CREATE INDEX idx_venta_fecha         ON venta (fecha_venta);
CREATE INDEX idx_venta_usuario_fecha ON venta (usuario_id, fecha_venta);

-- =============================================================
-- TABLA: detalle_venta
-- Sección 3.17.
-- Cada línea de la venta: un producto con precio histórico.
--
-- DECISIÓN CLAVE: No hay FK a un "precio" — guardamos precio_unitario
-- directamente. Si el precio del producto cambia mañana, esta venta
-- conserva el precio original. Integridad contable garantizada.
-- =============================================================
CREATE TABLE IF NOT EXISTS detalle_venta (
    detalle_venta_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    venta_id             BIGINT UNSIGNED NOT NULL,
    producto_id          BIGINT UNSIGNED NOT NULL,
    cantidad             INT             NOT NULL
        CONSTRAINT chk_det_venta_cantidad CHECK (cantidad > 0),
    precio_unitario      DECIMAL(14,2)   NOT NULL
        COMMENT 'Precio en el MOMENTO de la venta. Precio histórico — no FK al producto.',
    precio_con_descuento DECIMAL(14,2)   NOT NULL
        COMMENT 'precio_unitario - descuento_item. Nunca negativo.',
    descuento_item       DECIMAL(14,2)   NOT NULL DEFAULT 0.00
        CONSTRAINT chk_det_desc CHECK (descuento_item >= 0),
    subtotal             DECIMAL(14,2)   NOT NULL
        COMMENT 'cantidad × precio_con_descuento. Calculado en Java.',
    -- Garantía por ítem (RF-V04)
    garantia_meses       TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT 'Duración de garantía en meses. 0 = sin garantía.',
    garantia_vence       DATE             NULL
        COMMENT 'fecha_venta + garantia_meses. Calculado en Java.',
    notas_item           VARCHAR(255)     NULL,
    -- URL del certificado de autenticidad del ítem (RF-CAT03). NULL hasta Fase 4.
    certificado_url      VARCHAR(500)     NULL
        COMMENT 'URL al PDF del certificado de autenticidad. Generado en Fase 4.',

    CONSTRAINT pk_detalle_venta      PRIMARY KEY (detalle_venta_id),
    CONSTRAINT fk_det_venta_venta    FOREIGN KEY (venta_id)
        REFERENCES venta (venta_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_det_venta_producto FOREIGN KEY (producto_id)
        REFERENCES producto (producto_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_precio_positivo   CHECK (precio_con_descuento >= 0)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Detalles de venta con precio histórico (Sección 3.17). Inmutables.';

CREATE INDEX idx_detalle_venta_venta    ON detalle_venta (venta_id);
CREATE INDEX idx_detalle_venta_producto ON detalle_venta (producto_id);

-- =============================================================
-- TABLA: pago_venta
-- Sección 3.18 — RF-V01 (anticipo), RF17 (contado).
-- Registro de cada pago recibido en una venta.
-- Una venta puede tener múltiples pagos (abonos anticipados, crédito).
-- =============================================================
CREATE TABLE IF NOT EXISTS pago_venta (
    pago_id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    venta_id        BIGINT UNSIGNED NOT NULL,
    metodo_pago_id  BIGINT UNSIGNED NOT NULL,
    monto           DECIMAL(14,2)   NOT NULL
        CONSTRAINT chk_pago_monto CHECK (monto > 0),
    tipo_pago       ENUM('abono','pago_completo','anticipo','canje_puntos') NOT NULL,
    fecha_pago      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    referencia      VARCHAR(100)    NULL  COMMENT 'Número de referencia de transferencia/Nequi',
    estado          ENUM('registrado','anulado') NOT NULL DEFAULT 'registrado',

    CONSTRAINT pk_pago_venta     PRIMARY KEY (pago_id),
    CONSTRAINT fk_pago_venta     FOREIGN KEY (venta_id)
        REFERENCES venta (venta_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_pago_metpag    FOREIGN KEY (metodo_pago_id)
        REFERENCES metodo_pago (metodo_pago_id)
        ON UPDATE CASCADE ON DELETE RESTRICT

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Pagos recibidos por venta (RF17, RF-V01). Múltiples pagos por venta.';

CREATE INDEX idx_pago_venta ON pago_venta (venta_id);

-- =============================================================
-- NOTA: La tabla venta_promocion se crea en V8__promociones.sql
-- porque depende de la tabla 'promocion' que se crea en esa migración.
-- =============================================================

-- =============================================================
-- TABLA: credito_venta (ventas a crédito — RF-C02)
-- Similar a credito_compra pero para el lado de ventas.
-- Una venta a crédito genera UN registro de crédito.
-- =============================================================
CREATE TABLE IF NOT EXISTS credito_venta (
    credito_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    venta_id         BIGINT UNSIGNED NOT NULL,
    monto_total      DECIMAL(14,2)   NOT NULL CONSTRAINT chk_cv_monto CHECK (monto_total > 0),
    saldo_pendiente  DECIMAL(14,2)   NOT NULL CONSTRAINT chk_cv_saldo CHECK (saldo_pendiente >= 0),
    fecha_inicio     DATE            NOT NULL,
    fecha_vencimiento DATE           NOT NULL,
    estado           ENUM('activo','pagado','vencido') NOT NULL DEFAULT 'activo',

    CONSTRAINT pk_credito_venta   PRIMARY KEY (credito_id),
    CONSTRAINT uq_credito_venta   UNIQUE (venta_id),
    CONSTRAINT fk_cv_venta        FOREIGN KEY (venta_id)
        REFERENCES venta (venta_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_cv_fechas      CHECK (fecha_vencimiento > fecha_inicio)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Crédito de venta (RF-C02). 1 venta a crédito : 1 registro de crédito.';
