-- =============================================================
-- Flyway Migration: V8__promociones.sql
-- Descripción: Módulo de descuentos y promociones (RF-V02).
--              Tabla: promocion, venta_promocion
-- Referencia: Sección 3.28, RF-V02 del documento AAC27.
-- Depende de: V6__ventas.sql (venta)
--
-- REGLAS DE NEGOCIO (RF-V02):
-- 1. El descuento puede ser por PORCENTAJE o MONTO FIJO.
-- 2. Tiene vigencia: fecha_inicio y fecha_fin.
-- 3. Puede aplicar a: toda la venta, una categoría, o un producto específico.
-- 4. El descuento NO puede resultar en precio negativo (validado en Java).
-- 5. Solo el Administrador puede crear/editar promociones.
-- 6. El Vendedor puede aplicar promociones ACTIVAS dentro de los límites.
-- =============================================================

SET time_zone = 'America/Bogota';

-- =============================================================
-- TABLA: promocion
-- Sección 3.28 — RF-V02.
-- Define el descuento, vigencia y alcance.
-- =============================================================
CREATE TABLE IF NOT EXISTS promocion (
    promocion_id    BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(150)     NOT NULL
        COMMENT 'Nombre descriptivo de la promoción (ej: "Black Friday 20%")',
    descripcion     TEXT             NULL,
    tipo            ENUM('porcentaje','monto_fijo') NOT NULL
        COMMENT 'porcentaje: valor es 0-100. monto_fijo: valor en COP',
    valor           DECIMAL(10,2)    NOT NULL
        CONSTRAINT chk_promo_valor CHECK (valor > 0)
        COMMENT 'Porcentaje (0-100) o monto fijo en COP según tipo',
    aplica_a        ENUM('venta','categoria','producto') NOT NULL DEFAULT 'venta'
        COMMENT 'Alcance de la promoción',
    entidad_id      BIGINT UNSIGNED  NULL
        COMMENT 'ID de categoría o producto si aplica_a != venta. NULL si aplica_a = venta',
    fecha_inicio    DATE             NOT NULL,
    fecha_fin       DATE             NOT NULL
        CONSTRAINT chk_promo_fechas CHECK (fecha_fin >= fecha_inicio),
    activa          TINYINT(1)       NOT NULL DEFAULT 1
        COMMENT '0: desactivada manualmente. 1: activa. La vigencia se controla por fechas.',
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_promocion PRIMARY KEY (promocion_id),
    -- Máximo 100% de descuento cuando es porcentaje (validado en Java también)
    CONSTRAINT chk_promo_porcentaje CHECK (
        tipo != 'porcentaje' OR valor <= 100
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Promociones y descuentos con vigencia (RF-V02, Sección 3.28).';

-- Índice para búsqueda de promociones vigentes
CREATE INDEX idx_promo_fechas  ON promocion (fecha_inicio, fecha_fin);
CREATE INDEX idx_promo_activa  ON promocion (activa, fecha_inicio, fecha_fin);
CREATE INDEX idx_promo_aplica  ON promocion (aplica_a, entidad_id);

-- =============================================================
-- TABLA: venta_promocion (relación N:M entre Venta y Promocion)
-- Registra qué promociones se aplicaron a cada venta y cuánto
-- fue el descuento efectivo aplicado.
-- Movida aquí desde V6 porque depende de la tabla promocion.
-- =============================================================
CREATE TABLE IF NOT EXISTS venta_promocion (
    venta_id            BIGINT UNSIGNED NOT NULL,
    promocion_id        BIGINT UNSIGNED NOT NULL,
    descuento_aplicado  DECIMAL(14,2)   NOT NULL
        COMMENT 'Descuento efectivo en COP que generó esta promoción en esta venta',

    CONSTRAINT pk_venta_promo     PRIMARY KEY (venta_id, promocion_id),
    CONSTRAINT fk_vp_venta        FOREIGN KEY (venta_id)
        REFERENCES venta (venta_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_vp_promocion    FOREIGN KEY (promocion_id)
        REFERENCES promocion (promocion_id)
        ON UPDATE CASCADE ON DELETE RESTRICT

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Promociones aplicadas a cada venta (RF-V02). Historial inmutable.';
