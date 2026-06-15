-- =============================================================
-- Flyway Migration: V1__crear_esquema_inicial.sql
-- Descripción: Crea las tablas de seguridad y usuarios del sistema.
--              Tablas: rol, usuario
-- Referencia: Secciones 3.1 y 3.2 del documento de arquitectura AAC27.
--
-- REGLA FLYWAY: Nunca modificar este archivo una vez aplicado.
--               Los cambios futuros van en V2__, V3__, etc.
-- =============================================================

-- Zona horaria para todas las operaciones de esta sesión
SET time_zone = 'America/Bogota';

-- =============================================================
-- TABLA: rol
-- Sección 3.2
-- Los roles son inmutables (RF01: "Los roles no pueden eliminarse").
-- Los 3 registros se insertan como datos semilla al final.
-- =============================================================
CREATE TABLE IF NOT EXISTS rol (
    rol_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre      ENUM('superadministrador','administrador','vendedor') NOT NULL,
    descripcion VARCHAR(255)    NULL,

    CONSTRAINT pk_rol      PRIMARY KEY (rol_id),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Roles del sistema. Inmutables: no eliminar.';

-- =============================================================
-- TABLA: usuario
-- Sección 3.1
-- Implementa UserDetails de Spring Security.
-- Índices recomendados en Sección 4.4.
-- =============================================================
CREATE TABLE IF NOT EXISTS usuario (
    usuario_id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre               VARCHAR(150)    NOT NULL,
    numero_documento     VARCHAR(20)     NOT NULL,
    correo               VARCHAR(255)    NOT NULL,
    telefono             VARCHAR(20)     NULL,
    password_hash        VARCHAR(255)    NOT NULL    COMMENT 'BCrypt hash, cost>=12',
    es_password_temporal TINYINT(1)      NOT NULL    DEFAULT 1 COMMENT '1=provisional, 0=personalizada',
    estado               ENUM('activo','inactivo') NOT NULL DEFAULT 'activo',
    rol_id               BIGINT UNSIGNED NOT NULL,
    ultimo_acceso        DATETIME        NULL        COMMENT 'Timestamp del último login exitoso',
    foto_perfil_url      VARCHAR(500)    NULL,
    created_at           DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_usuario         PRIMARY KEY (usuario_id),
    CONSTRAINT uq_usuario_correo  UNIQUE (correo),
    CONSTRAINT uq_usuario_doc     UNIQUE (numero_documento),
    CONSTRAINT fk_usuario_rol     FOREIGN KEY (rol_id)
        REFERENCES rol (rol_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT   -- Un rol no puede eliminarse si tiene usuarios asignados

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Usuarios del sistema (empleados y administradores).';

-- Índices de rendimiento (Sección 4.4)
CREATE INDEX idx_usuario_correo ON usuario (correo);
CREATE INDEX idx_usuario_estado ON usuario (estado);

-- =============================================================
-- TABLA: token_recuperacion
-- Sección 3.27 — Para el flujo de recuperación de contraseña (RF03).
-- Tokens de un solo uso con expiración de 15 minutos.
-- =============================================================
CREATE TABLE IF NOT EXISTS token_recuperacion (
    token_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario_id  BIGINT UNSIGNED NOT NULL,
    token_hash  VARCHAR(255)    NOT NULL  COMMENT 'Hash SHA-256 del código de 6 dígitos',
    expira_en   DATETIME        NOT NULL  COMMENT '15 minutos desde la generación',
    usado       TINYINT(1)      NOT NULL  DEFAULT 0 COMMENT '1 si ya fue utilizado',
    created_at  DATETIME        NOT NULL  DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_token_rec    PRIMARY KEY (token_id),
    CONSTRAINT uq_token_hash   UNIQUE (token_hash),
    CONSTRAINT fk_token_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (usuario_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE   -- Si se borra el usuario, se borran sus tokens

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Tokens de recuperación de contraseña (RF03). Un solo uso, 15min.';

-- Índice de Sección 4.4: búsqueda eficiente de tokens activos
CREATE INDEX idx_token_usuario ON token_recuperacion (usuario_id, usado, expira_en);

-- =============================================================
-- TABLA: auditoria_log
-- Sección 3.26 — Registro inmutable de acciones críticas (RF-S01).
-- Solo lectura para el Administrador.
-- =============================================================
CREATE TABLE IF NOT EXISTS auditoria_log (
    log_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    usuario_id      BIGINT UNSIGNED NULL     COMMENT 'NULL si fue acción sin autenticación (login fallido)',
    accion          VARCHAR(100)    NOT NULL COMMENT 'Ej: CREAR_VENTA, EDITAR_USUARIO',
    entidad         VARCHAR(100)    NOT NULL COMMENT 'Nombre de la tabla/entidad afectada',
    entidad_id      BIGINT UNSIGNED NULL,
    datos_anteriores JSON           NULL     COMMENT 'Estado previo (UPDATE/DELETE)',
    datos_nuevos     JSON           NULL     COMMENT 'Estado nuevo (CREATE/UPDATE)',
    ip_cliente      VARCHAR(45)     NULL     COMMENT 'IPv4 o IPv6 del cliente',
    user_agent      VARCHAR(500)    NULL,
    fecha_hora      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Inmutable',

    CONSTRAINT pk_auditoria PRIMARY KEY (log_id),
    -- FK opcional: si el usuario se elimina (nunca pasa con borrado lógico), el log permanece
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id)
        REFERENCES usuario (usuario_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Registro inmutable de auditoría (RF-S01). No modificar registros.';

-- Índice de Sección 4.4
CREATE INDEX idx_auditoria_usuario_fecha ON auditoria_log (usuario_id, fecha_hora DESC);

-- =============================================================
-- DATOS SEMILLA: Roles del sistema
-- Los 3 roles base son inmutables. Se insertan aquí una sola vez.
-- INSERT IGNORE: si ya existen (re-ejecución en desarrollo), los omite.
-- =============================================================
INSERT IGNORE INTO rol (nombre, descripcion) VALUES
    ('superadministrador', 'Acceso técnico total al sistema. No asignar a usuarios de negocio.'),
    ('administrador',       'Acceso completo a todas las funcionalidades del negocio.'),
    ('vendedor',            'Acceso limitado: registro de ventas y consulta de datos propios.');

-- =============================================================
-- DATOS SEMILLA: Usuario administrador inicial
-- Contraseña provisional: Admin2026! (BCrypt cost 12)
-- El usuario DEBE cambiarla en el primer acceso (es_password_temporal = 1).
-- IMPORTANTE: Cambiar esta contraseña en producción antes del despliegue.
-- =============================================================
INSERT IGNORE INTO usuario (
    nombre,
    numero_documento,
    correo,
    telefono,
    password_hash,
    es_password_temporal,
    estado,
    rol_id
) VALUES (
    'Administrador Sistema',
    '1000000000',
    'admin@aac27.com',
    '3001234567',
    -- BCrypt hash de 'Admin2026!' con cost 12
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/UrGeLEnhuyO.0aGMq',
    1,        -- es_password_temporal = true
    'activo',
    (SELECT rol_id FROM rol WHERE nombre = 'administrador')
);
