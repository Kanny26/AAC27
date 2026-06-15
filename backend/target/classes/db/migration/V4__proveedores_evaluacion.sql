-- =============================================================
-- Flyway Migration: V4__proveedores_evaluacion.sql
-- Descripción: Tabla de evaluación de proveedores (RF-P01).
--              Los datos base del proveedor están en V2__catalogo_base.sql.
-- Referencia: Sección 3.10, RF-P01 del documento de arquitectura AAC27.
-- Depende de: V2__catalogo_base.sql
-- =============================================================

SET time_zone = 'America/Bogota';

-- =============================================================
-- TABLA: evaluacion_proveedor
-- RF-P01: Registrar evaluaciones periódicas: calidad,
--         tiempo de entrega, precio. Indicador de desempeño.
-- Escala 1-5 por dimensión. Promedio ponderado almacenado.
-- Prioridad: Baja — se implementa en Fase 4.
-- =============================================================
CREATE TABLE IF NOT EXISTS evaluacion_proveedor (
    evaluacion_id       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    proveedor_id        BIGINT UNSIGNED NOT NULL,
    usuario_id          BIGINT UNSIGNED NOT NULL COMMENT 'Administrador que realizó la evaluación',
    -- Dimensiones de evaluación (escala 1-5)
    calidad             TINYINT UNSIGNED NOT NULL
        CONSTRAINT chk_eval_calidad CHECK (calidad BETWEEN 1 AND 5),
    tiempo_entrega      TINYINT UNSIGNED NOT NULL
        CONSTRAINT chk_eval_tiempo CHECK (tiempo_entrega BETWEEN 1 AND 5),
    precio              TINYINT UNSIGNED NOT NULL
        CONSTRAINT chk_eval_precio CHECK (precio BETWEEN 1 AND 5),
    -- Promedio ponderado calculado y almacenado (evita recalcular en cada consulta)
    promedio_ponderado  DECIMAL(3,2)    NOT NULL
        COMMENT 'Calculado en Java: (calidad*0.4 + tiempo*0.35 + precio*0.25)',
    observaciones       TEXT            NULL,
    fecha_evaluacion    DATE            NOT NULL DEFAULT (CURDATE()),
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_evaluacion        PRIMARY KEY (evaluacion_id),
    CONSTRAINT fk_eval_proveedor    FOREIGN KEY (proveedor_id)
        REFERENCES proveedor (proveedor_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_eval_usuario      FOREIGN KEY (usuario_id)
        REFERENCES usuario (usuario_id)
        ON UPDATE CASCADE ON DELETE RESTRICT

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Historial de evaluaciones de proveedor (RF-P01, prioridad Baja).';

CREATE INDEX idx_eval_proveedor ON evaluacion_proveedor (proveedor_id, fecha_evaluacion DESC);
