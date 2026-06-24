-- =============================================================
-- Flyway Migration: V3__productos.sql
-- Descripción: Crea las tablas de productos e inventario:
--              producto, producto_subcategoria, producto_trazabilidad,
--              inventario_movimiento.
-- Referencia: Secciones 3.7, 3.8, 3.9, 3.22
--             del documento de arquitectura AAC27.
-- Depende de: V2__catalogo_base.sql
-- =============================================================

SET time_zone = 'America/Bogota';

-- =============================================================
-- TABLA: producto
-- Sección 3.7 — RF-CAT01.
-- Producto con SKU único, precio, stock, trazabilidad y control de versión.
--
-- CAMPO @Version (optimistic locking):
-- La columna 'version' es usada por Hibernate para detectar
-- modificaciones concurrentes de stock.
-- Si dos transacciones intentan modificar el mismo producto al mismo tiempo,
-- Hibernate lanza OptimisticLockException (RNF-REN04).
-- =============================================================
CREATE TABLE IF NOT EXISTS producto (
    producto_id  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    codigo       VARCHAR(20)      NOT NULL COMMENT 'SKU único generado por el sistema',
    nombre       VARCHAR(255)     NOT NULL,
    descripcion  TEXT             NULL
        COMMENT 'Mín 10 chars, máx 500 chars — validado en Java',
    categoria_id BIGINT UNSIGNED  NOT NULL,
    material_id  BIGINT UNSIGNED  NOT NULL,
    proveedor_id BIGINT UNSIGNED  NULL     COMMENT 'Proveedor asociado (opcional)',
    precio_costo DECIMAL(14,2)    NOT NULL,
    precio_venta DECIMAL(14,2)    NOT NULL
        COMMENT 'Debe ser > precio_costo (validado en Java, RF-CAT01)',
    stock        INT              NOT NULL DEFAULT 0,
    stock_minimo INT              NOT NULL DEFAULT 5
        COMMENT 'Umbral para alerta de stock bajo (RF-INV01)',
    imagen_url   VARCHAR(500)     NULL,
    numero_serie VARCHAR(100)     NULL COMMENT 'Para productos únicos de alto valor',
    numero_lote  VARCHAR(100)     NULL COMMENT 'Número de lote del proveedor',
    estado       ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
    -- Campo de control de concurrencia optimista (Hibernate @Version — Sección 3.29)
    -- Hibernaate incrementa este valor en cada UPDATE. Si dos transacciones
    -- leen el mismo valor y ambas intentan guardar, la segunda falla con OptimisticLockException.
    version      INT              NOT NULL DEFAULT 0,
    created_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_producto        PRIMARY KEY (producto_id),
    CONSTRAINT uq_producto_codigo UNIQUE (codigo),
    CONSTRAINT chk_prod_costo     CHECK (precio_costo >= 0),
    CONSTRAINT chk_prod_stock     CHECK (stock >= 0),
    CONSTRAINT chk_prod_stock_min CHECK (stock_minimo >= 0),
    CONSTRAINT fk_prod_categoria  FOREIGN KEY (categoria_id)
        REFERENCES categoria (categoria_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_prod_material   FOREIGN KEY (material_id)
        REFERENCES material (material_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_prod_proveedor  FOREIGN KEY (proveedor_id)
        REFERENCES proveedor (proveedor_id)
        ON UPDATE CASCADE ON DELETE SET NULL

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Catálogo de productos (RF-CAT01). Campo version para concurrencia optimista.';

-- Índices de rendimiento (Sección 4.4)
CREATE INDEX idx_producto_categoria ON producto (categoria_id);
CREATE INDEX idx_producto_estado    ON producto (estado);
CREATE INDEX idx_producto_codigo    ON producto (codigo);  -- Búsqueda por SKU (RF-B02)

-- =============================================================
-- TABLA: producto_subcategoria (relación N:M)
-- Sección 3.9 — Un producto puede pertenecer a múltiples subcategorías.
-- Ejemplo: un anillo puede ser a la vez "Matrimonio" y "Compromiso".
-- =============================================================
CREATE TABLE IF NOT EXISTS producto_subcategoria (
    producto_id     BIGINT UNSIGNED NOT NULL,
    subcategoria_id BIGINT UNSIGNED NOT NULL,

    CONSTRAINT pk_prod_subcat     PRIMARY KEY (producto_id, subcategoria_id),
    CONSTRAINT fk_ps_producto     FOREIGN KEY (producto_id)
        REFERENCES producto (producto_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_ps_subcategoria FOREIGN KEY (subcategoria_id)
        REFERENCES subcategoria (subcategoria_id)
        ON UPDATE CASCADE ON DELETE RESTRICT

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Relación N:M entre productos y subcategorías (Sección 3.9).';

-- =============================================================
-- TABLA: producto_trazabilidad
-- Sección 3.8 — RF-CAT02.
-- Solo aplica a productos con material.es_trazable = 1.
-- Uno a uno con producto (UQ en producto_id).
-- =============================================================
CREATE TABLE IF NOT EXISTS producto_trazabilidad (
    trazabilidad_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    producto_id              BIGINT UNSIGNED NOT NULL,
    ley                      VARCHAR(20)     NULL COMMENT '925, 750, 585, etc.',
    quilates                 DECIMAL(5,2)    NULL COMMENT '18K, 14K, 10K, etc.',
    certificado_procedencia  VARCHAR(255)    NULL COMMENT 'Número de certificado',
    piedras_descripcion      TEXT            NULL COMMENT 'Tipo, quilates y certificado de piedras',
    created_at               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_trazabilidad      PRIMARY KEY (trazabilidad_id),
    CONSTRAINT uq_trazabilidad_prod UNIQUE (producto_id),  -- 1 producto: 1 registro
    CONSTRAINT fk_traz_producto     FOREIGN KEY (producto_id)
        REFERENCES producto (producto_id)
        ON UPDATE CASCADE ON DELETE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Datos de trazabilidad para metales y piedras preciosas (RF-CAT02).';

-- =============================================================
-- TABLA: inventario_movimiento
-- Sección 3.22 — RF15, RF16.
-- Registro inmutable de todos los movimientos de stock.
-- Incluye stock_antes y stock_despues para evitar recalcular historial (Sección 3.29).
-- =============================================================
CREATE TABLE IF NOT EXISTS inventario_movimiento (
    movimiento_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    producto_id      BIGINT UNSIGNED NOT NULL,
    tipo             ENUM('entrada','salida','ajuste','reserva','liberacion') NOT NULL,
    cantidad         INT             NOT NULL
        COMMENT 'Positivo para entradas, negativo para salidas',
    stock_antes      INT             NOT NULL,
    stock_despues    INT             NOT NULL,
    referencia_tipo  VARCHAR(50)     NOT NULL COMMENT 'venta, compra, ajuste, apartado',
    referencia_id    BIGINT UNSIGNED NOT NULL COMMENT 'ID del documento origen',
    usuario_id       BIGINT UNSIGNED NOT NULL,
    motivo           VARCHAR(255)    NULL
        COMMENT 'Obligatorio para ajustes manuales (mín 20 chars — validado en Java)',
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_inventario_mov    PRIMARY KEY (movimiento_id),
    CONSTRAINT chk_mov_cantidad     CHECK (cantidad != 0),
    CONSTRAINT chk_mov_antes        CHECK (stock_antes >= 0),
    CONSTRAINT chk_mov_despues      CHECK (stock_despues >= 0),
    CONSTRAINT fk_inv_producto      FOREIGN KEY (producto_id)
        REFERENCES producto (producto_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_inv_usuario       FOREIGN KEY (usuario_id)
        REFERENCES usuario (usuario_id)
        ON UPDATE CASCADE ON DELETE RESTRICT

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Historial inmutable de movimientos de inventario (RF15-RF16).';

-- Índice de Sección 4.4: historial de movimientos por producto y fecha
CREATE INDEX idx_inventario_movimiento_producto_fecha
    ON inventario_movimiento (producto_id, created_at DESC);
