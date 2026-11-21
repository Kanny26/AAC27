-- =============================================================
-- V10: Historial de contraseñas (RF-S02)
-- Guarda el hash BCrypt de cada contraseña que ha tenido un usuario,
-- para prohibir la reutilización de las últimas 5 (la vigente cuenta como una).
-- Nunca se guarda ninguna contraseña en texto plano (RNF-SEG03).
-- =============================================================
CREATE TABLE IF NOT EXISTS usuario_password_historial (
                                                          historial_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                                          usuario_id    BIGINT UNSIGNED NOT NULL,
                                                          password_hash VARCHAR(255)    NOT NULL COMMENT 'BCrypt hash, cost>=12',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_usuario_password_historial PRIMARY KEY (historial_id),
    CONSTRAINT fk_pwd_historial_usuario FOREIGN KEY (usuario_id)
    REFERENCES usuario (usuario_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
    INDEX idx_pwd_historial_usuario (usuario_id, historial_id)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci
    COMMENT='Hashes de contraseñas anteriores de cada usuario (RF-S02).';

-- Los usuarios que ya existen entran con su contraseña vigente como primera fila,
-- de modo que "las últimas 5 incluida la actual" sea cierto desde el primer día.
INSERT INTO usuario_password_historial (usuario_id, password_hash)
SELECT usuario_id, password_hash FROM usuario;