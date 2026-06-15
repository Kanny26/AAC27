-- ============================================================
-- Flyway Migration: V9__postventa.sql
-- Descripción: Módulo de Postventa, Garantías y Reparaciones.
-- Referencia: RF24, RF25, RF26, RF-PV01, RF-PV02.
-- Estrategia: Herencia 'Table-per-Subclass' para Reparaciones.
-- ============================================================

-- 1. TABLA BASE: caso_postventa
CREATE TABLE IF NOT EXISTS caso_postventa (
    caso_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    venta_id         BIGINT UNSIGNED NULL COMMENT 'Puede ser NULL si es un reclamo general o compra directa',
    usuario_id       BIGINT UNSIGNED NOT NULL COMMENT 'Usuario que abre el caso',
    cliente_id       BIGINT UNSIGNED NOT NULL,
    tipo             ENUM('reclamo','devolucion','cambio','reparacion') NOT NULL,
    estado           ENUM('abierto','en_revision','aprobado','rechazado','cerrado') NOT NULL DEFAULT 'abierto',
    descripcion      TEXT            NOT NULL,
    respuesta_admin  TEXT            NULL,
    fecha_apertura   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre     DATETIME        NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_caso_postventa PRIMARY KEY (caso_id),
    CONSTRAINT fk_caso_venta   FOREIGN KEY (venta_id)   REFERENCES venta (venta_id)   ON DELETE SET NULL,
    CONSTRAINT fk_caso_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (usuario_id) ON DELETE RESTRICT,
    CONSTRAINT fk_caso_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cliente_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Casos de postventa (RF24)';

-- 2. TABLA HIJA (Herencia JOINED): reparacion
CREATE TABLE IF NOT EXISTS reparacion (
    caso_id                BIGINT UNSIGNED NOT NULL,
    descripcion_trabajo    TEXT            NOT NULL,
    presupuesto            DECIMAL(12,2)   NULL,
    costo_real             DECIMAL(12,2)   NULL,
    estado_reparacion      ENUM('recibido','en_proceso','listo','entregado') NOT NULL DEFAULT 'recibido',
    fecha_entrega_estimada DATE            NULL,

    -- La clave primaria es también la foránea hacia la tabla padre
    CONSTRAINT pk_reparacion PRIMARY KEY (caso_id),
    CONSTRAINT fk_reparacion_caso FOREIGN KEY (caso_id) REFERENCES caso_postventa (caso_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Extensión de postventa para reparaciones (RF-PV01)';

-- 3. DETALLE DE DEVOLUCIONES/CAMBIOS
-- Para saber exactamente qué producto(s) de la venta se devolvieron o cambiaron.
CREATE TABLE IF NOT EXISTS detalle_caso_postventa (
    detalle_caso_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    caso_id          BIGINT UNSIGNED NOT NULL,
    producto_id      BIGINT UNSIGNED NOT NULL,
    cantidad         INT             NOT NULL,
    motivo           VARCHAR(255)    NOT NULL,
    estado_producto  ENUM('bueno','defectuoso') NOT NULL COMMENT 'Bueno = vuelve a inventario; Defectuoso = descarte',

    CONSTRAINT pk_detalle_caso PRIMARY KEY (detalle_caso_id),
    CONSTRAINT fk_dc_caso     FOREIGN KEY (caso_id)     REFERENCES caso_postventa (caso_id) ON DELETE CASCADE,
    CONSTRAINT fk_dc_producto FOREIGN KEY (producto_id) REFERENCES producto (producto_id)   ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Ítems específicos devueltos o cambiados (RF25)';

-- 4. HISTORIAL DE ESTADOS (Auditoría de Casos)
CREATE TABLE IF NOT EXISTS historial_caso (
    historial_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    caso_id          BIGINT UNSIGNED NOT NULL,
    usuario_id       BIGINT UNSIGNED NOT NULL,
    estado_anterior  VARCHAR(50)     NULL,
    estado_nuevo     VARCHAR(50)     NOT NULL,
    comentario       TEXT            NULL,
    fecha_cambio     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_historial_caso PRIMARY KEY (historial_id),
    CONSTRAINT fk_hc_caso    FOREIGN KEY (caso_id)    REFERENCES caso_postventa (caso_id) ON DELETE CASCADE,
    CONSTRAINT fk_hc_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (usuario_id)     ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Historial de cambios de estado del caso (Auditoría)';
