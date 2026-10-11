-- =============================================================
-- Flyway Migration: V2__catalogo_base.sql
-- Descripción: Crea las tablas base del catálogo:
--              metodo_pago, cliente, proveedor (y sus sub-tablas),
--              categoria, subcategoria, material.
-- Referencia: Secciones 3.3 a 3.6, 3.10 a 3.13, 3.23
--             del documento de arquitectura AAC27.
-- Depende de: V1__crear_esquema_inicial.sql
-- =============================================================

SET time_zone = 'America/Bogota';

-- =============================================================
-- TABLA: metodo_pago
-- Sección 3.23 — RF28.
-- Métodos de pago disponibles: Efectivo, Nequi, Transferencia, etc.
-- No se elimina si tiene transacciones asociadas.
-- =============================================================
CREATE TABLE IF NOT EXISTS metodo_pago (
    metodo_pago_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(100)    NOT NULL COMMENT 'Efectivo, Transferencia, Nequi, etc.',
    estado          ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',

    CONSTRAINT pk_metodo_pago       PRIMARY KEY (metodo_pago_id),
    CONSTRAINT uq_metodo_pago_nombre UNIQUE (nombre)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Métodos de pago del sistema (RF28). No eliminar si tiene transacciones.';

-- =============================================================
-- TABLA: cliente
-- Sección 3.3 — RF-U02 (CRM Básico).
-- Gestión de clientes con historial de compras y puntos de fidelidad.
-- =============================================================
CREATE TABLE IF NOT EXISTS cliente (
    cliente_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre           VARCHAR(150)    NOT NULL,
    numero_documento VARCHAR(20)     NULL COMMENT 'Único si se ingresa',
    telefono         VARCHAR(20)     NULL,
    correo           VARCHAR(255)    NULL COMMENT 'Único si se ingresa',
    direccion        VARCHAR(500)    NULL,
    fecha_nacimiento DATE            NULL,
    puntos_fidelidad INT UNSIGNED    NOT NULL DEFAULT 0
        COMMENT '1 punto por cada 10,000 COP en compras (RF-V03)',
    notas            TEXT            NULL COMMENT 'Preferencias del cliente',
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_cliente          PRIMARY KEY (cliente_id),
    CONSTRAINT uq_cliente_documento UNIQUE (numero_documento),
    CONSTRAINT uq_cliente_correo    UNIQUE (correo)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Clientes del negocio (CRM básico - RF-U02).';

-- Índices de búsqueda (RF-B01: búsqueda por nombre, documento o teléfono)
CREATE INDEX idx_cliente_nombre ON cliente (nombre);

-- =============================================================
-- TABLA: proveedor
-- Sección 3.10 — RF09, RF10.
-- =============================================================
CREATE TABLE IF NOT EXISTS proveedor (
    proveedor_id BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(255)     NOT NULL COMMENT 'Razón social o nombre comercial',
    documento    VARCHAR(50)      NOT NULL COMMENT 'NIT o cédula. Inmutable.',
    fecha_inicio DATE             NOT NULL COMMENT 'Inicio de relación comercial. No puede ser futura.',
    minimo_compra DECIMAL(14,2)   NOT NULL DEFAULT 0.00,
    estado       ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
    created_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_proveedor        PRIMARY KEY (proveedor_id),
    CONSTRAINT uq_proveedor_doc    UNIQUE (documento),
    CONSTRAINT chk_prov_minimo     CHECK (minimo_compra >= 0)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Proveedores de joyería (RF09-RF11). Documento inmutable.';

-- =============================================================
-- TABLA: proveedor_telefono
-- Sección 3.11 — Un proveedor puede tener múltiples teléfonos.
-- =============================================================
CREATE TABLE IF NOT EXISTS proveedor_telefono (
    telefono_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    proveedor_id BIGINT UNSIGNED NOT NULL,
    telefono     VARCHAR(20)     NOT NULL,
    es_principal TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '1 si es contacto principal',

    CONSTRAINT pk_prov_tel     PRIMARY KEY (telefono_id),
    CONSTRAINT fk_prov_tel_prov FOREIGN KEY (proveedor_id)
        REFERENCES proveedor (proveedor_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Teléfonos del proveedor. Al menos uno requerido (RF10).';

-- =============================================================
-- TABLA: proveedor_correo
-- Sección 3.12 — Un proveedor puede tener múltiples correos.
-- =============================================================
CREATE TABLE IF NOT EXISTS proveedor_correo (
    correo_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    proveedor_id BIGINT UNSIGNED NOT NULL,
    correo       VARCHAR(255)    NOT NULL,
    es_principal TINYINT(1)      NOT NULL DEFAULT 0,

    CONSTRAINT pk_prov_correo      PRIMARY KEY (correo_id),
    CONSTRAINT uq_prov_correo      UNIQUE (correo),
    CONSTRAINT fk_prov_correo_prov FOREIGN KEY (proveedor_id)
        REFERENCES proveedor (proveedor_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Correos del proveedor. Al menos uno requerido (RF10).';

-- =============================================================
-- TABLA: categoria
-- Sección 3.4 — RF12.
-- Categorías de productos: Anillos, Collares, Pulseras, etc.
-- =============================================================
CREATE TABLE IF NOT EXISTS categoria (
    categoria_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(100)    NOT NULL,
    icono_url    VARCHAR(500)    NULL,
    estado       ENUM('activo','inactivo') NOT NULL DEFAULT 'activo'
        COMMENT 'No se desactiva si tiene productos activos (RF12)',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_categoria       PRIMARY KEY (categoria_id),
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Categorías de productos (RF12). No desactivar con productos activos.';

-- =============================================================
-- TABLA: subcategoria
-- Sección 3.5 — RF13.
-- Subcategorías: 15 años, Matrimonio, Uso Diario, etc.
-- =============================================================
CREATE TABLE IF NOT EXISTS subcategoria (
    subcategoria_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(100)    NOT NULL,
    estado          ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_subcategoria       PRIMARY KEY (subcategoria_id),
    CONSTRAINT uq_subcategoria_nombre UNIQUE (nombre)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Subcategorías de productos (RF13).';

-- =============================================================
-- TABLA: material
-- Sección 3.6 — RF14.
-- Materiales: Plata Ley 950, Covergold, Acero Inoxidable, etc.
-- =============================================================
CREATE TABLE IF NOT EXISTS material (
    material_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(100)    NOT NULL,
    es_trazable  TINYINT(1)      NOT NULL DEFAULT 0
        COMMENT '1 = requiere datos de trazabilidad (ley, quilates) — Sección RF-CAT02',
    estado       ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',

    CONSTRAINT pk_material        PRIMARY KEY (material_id),
    CONSTRAINT uq_material_nombre UNIQUE (nombre)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Materiales de joyería (RF14). es_trazable activa flujo de certificación.';

-- =============================================================
-- TABLA: proveedor_material (relación N:M)
-- Sección 3.13 — Un proveedor puede suministrar múltiples materiales.
-- =============================================================
CREATE TABLE IF NOT EXISTS proveedor_material (
    proveedor_id BIGINT UNSIGNED NOT NULL,
    material_id  BIGINT UNSIGNED NOT NULL,

    CONSTRAINT pk_prov_mat      PRIMARY KEY (proveedor_id, material_id),
    CONSTRAINT fk_pm_proveedor  FOREIGN KEY (proveedor_id)
        REFERENCES proveedor (proveedor_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_pm_material   FOREIGN KEY (material_id)
        REFERENCES material (material_id)
        ON UPDATE CASCADE ON DELETE RESTRICT

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Materiales suministrados por cada proveedor (RF10: al menos uno requerido).';

-- =============================================================
-- DATOS SEMILLA: Métodos de pago
-- =============================================================
INSERT IGNORE INTO metodo_pago (nombre, estado) VALUES
    ('Efectivo',         'activo'),
    ('Transferencia',    'activo'),
    ('Nequi',            'activo'),
    ('Daviplata',        'activo'),
    ('Tarjeta Débito',   'activo'),
    ('Tarjeta Crédito',  'activo');

-- =============================================================
-- DATOS SEMILLA: Categorías de joyería típicas
-- =============================================================
INSERT IGNORE INTO categoria (nombre) VALUES
    ('Anillos'),
    ('Collares'),
    ('Pulseras'),
    ('Aretes'),
    ('Dijes'),
    ('Cadenas');

-- =============================================================
-- DATOS SEMILLA: Subcategorías comunes
-- =============================================================
INSERT IGNORE INTO subcategoria (nombre) VALUES
    ('15 Años'),
    ('Matrimonio'),
    ('Uso Diario'),
    ('Compromiso'),
    ('Bautizo'),
    ('Primera Comunión'),
    ('Graduación');

-- =============================================================
-- DATOS SEMILLA: Materiales
-- es_trazable=1 para metales preciosos (activa flujo RF-CAT02)
-- =============================================================
INSERT IGNORE INTO material (nombre, es_trazable) VALUES
    ('Plata Ley 950',       1),  -- Metal precioso: requiere certificación
    ('Oro Amarillo',        1),  -- Metal precioso
    ('Oro Blanco',          1),  -- Metal precioso
    ('Oro Rosa',            1),  -- Metal precioso
    ('Covergold',           0),  -- Chapado: no requiere trazabilidad
    ('Acero Inoxidable',    0),
    ('Plata Bañada',        0),
    ('Fantasía',            0)   -- Material genérico sin valor intrínseco
    ;
