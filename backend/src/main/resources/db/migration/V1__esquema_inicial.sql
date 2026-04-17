-- ============================================================
-- BASE DE DATOS: gestor_abbyac27
-- Versión corregida: relación Producto-Subcategoría normalizada
-- ============================================================

SET FOREIGN_KEY_CHECKS=0;

DROP DATABASE IF EXISTS gestor_abbyac27;
CREATE DATABASE gestor_abbyac27 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gestor_abbyac27;

-- ============================================================
-- MÓDULO 1: USUARIOS DEL SISTEMA
-- ============================================================
CREATE TABLE Usuario (
    usuario_id     INT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(255)  NOT NULL,
    pass           VARCHAR(255)  NOT NULL,
    estado         BOOLEAN       NOT NULL DEFAULT 1,
    pass_temporal  TINYINT(1)    NOT NULL DEFAULT 1,
    fecha_creacion DATETIME      NOT NULL DEFAULT NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Telefono_Usuario (
    telefono_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    telefono    VARCHAR(50)  NOT NULL,
    usuario_id  INT UNSIGNED NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES Usuario(usuario_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Correo_Usuario (
    correo_id  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    usuario_id INT UNSIGNED NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES Usuario(usuario_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MÓDULO 2: ROLES Y PERMISOS
-- ============================================================
CREATE TABLE Rol (
    rol_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    cargo  ENUM('superadministrador','administrador','vendedor') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Usuario_Rol (
    usuario_id INT UNSIGNED NOT NULL,
    rol_id     INT UNSIGNED NOT NULL,
    PRIMARY KEY (usuario_id, rol_id),
    FOREIGN KEY (usuario_id) REFERENCES Usuario(usuario_id) ON DELETE CASCADE,
    FOREIGN KEY (rol_id)     REFERENCES Rol(rol_id)         ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Permiso (
    permiso_id  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(255) NOT NULL,
    descripcion TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Rol_Permiso (
    rol_id     INT UNSIGNED NOT NULL,
    permiso_id INT UNSIGNED NOT NULL,
    PRIMARY KEY (rol_id, permiso_id),
    FOREIGN KEY (rol_id)     REFERENCES Rol(rol_id)         ON DELETE CASCADE,
    FOREIGN KEY (permiso_id) REFERENCES Permiso(permiso_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Recuperacion_Contrasena (
    recuperacion_id     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id          INT UNSIGNED NOT NULL,
    codigo_verificacion INT UNIQUE   NOT NULL,
    fecha_solicitud     DATETIME     NOT NULL,
    fecha_expiracion    DATETIME     NOT NULL,
    estado              BOOLEAN      NOT NULL DEFAULT 1,
    FOREIGN KEY (usuario_id) REFERENCES Usuario(usuario_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MÓDULO 3: PROVEEDORES
-- ============================================================
CREATE TABLE Proveedor (
    proveedor_id   INT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(255)  NOT NULL,
    documento      VARCHAR(50)   UNIQUE NOT NULL,
    fecha_registro DATE          NOT NULL DEFAULT (CURDATE()),
    fecha_inicio   DATE,
    minimo_compra  DECIMAL(10,2) NOT NULL,
    estado         BOOLEAN       NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Telefono_Proveedor (
    telefono_id  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    telefono     VARCHAR(50)  NOT NULL,
    proveedor_id INT UNSIGNED NOT NULL,
    FOREIGN KEY (proveedor_id) REFERENCES Proveedor(proveedor_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Correo_Proveedor (
    correo_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email        VARCHAR(255) NOT NULL,
    proveedor_id INT UNSIGNED NOT NULL,
    FOREIGN KEY (proveedor_id) REFERENCES Proveedor(proveedor_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MÓDULO 4: CLIENTES
-- ============================================================
CREATE TABLE Cliente (
    cliente_id     INT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(255)  NOT NULL,
    documento      VARCHAR(50)   UNIQUE,
    fecha_registro DATE          NOT NULL DEFAULT (CURDATE()),
    minimo_compra  DECIMAL(10,2),
    estado         BOOLEAN       NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Telefono_Cliente (
    telefono_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    telefono    VARCHAR(50)  NOT NULL,
    cliente_id  INT UNSIGNED NOT NULL,
    FOREIGN KEY (cliente_id) REFERENCES Cliente(cliente_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Correo_Cliente (
    correo_id  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    cliente_id INT UNSIGNED NOT NULL,
    FOREIGN KEY (cliente_id) REFERENCES Cliente(cliente_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MÓDULO 5: CATEGORÍAS, SUBCATEGORÍAS Y MATERIALES
-- ============================================================
CREATE TABLE Categoria (
    categoria_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(255) NOT NULL,
    icono        VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Subcategoria (
    subcategoria_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Catálogo de combinaciones válidas (Categoria ↔ Subcategoria)
-- Define qué subcategorías son aplicables a cada categoría
CREATE TABLE Categoria_Subcategoria (
    categoria_id    INT UNSIGNED NOT NULL,
    subcategoria_id INT UNSIGNED NOT NULL,
    PRIMARY KEY (categoria_id, subcategoria_id),
    FOREIGN KEY (categoria_id)    REFERENCES Categoria(categoria_id)    ON DELETE CASCADE,
    FOREIGN KEY (subcategoria_id) REFERENCES Subcategoria(subcategoria_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Material (
    material_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Proveedor_Material (
    proveedor_id INT UNSIGNED NOT NULL,
    material_id  INT UNSIGNED NOT NULL,
    PRIMARY KEY (proveedor_id, material_id),
    FOREIGN KEY (proveedor_id) REFERENCES Proveedor(proveedor_id) ON DELETE CASCADE,
    FOREIGN KEY (material_id)  REFERENCES Material(material_id)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MÓDULO 6: PRODUCTOS
-- CORRECCIÓN: se elimina subcategoria_id de esta tabla.
-- La relación con subcategorías se maneja en Producto_Subcategoria.
-- ============================================================
CREATE TABLE Producto (
    producto_id     INT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    codigo          VARCHAR(10)   NOT NULL UNIQUE,
    nombre          VARCHAR(255)  NOT NULL,
    descripcion     VARCHAR(500),
    stock           INT           NOT NULL DEFAULT 0,
    estado          BOOLEAN       NOT NULL DEFAULT 1,
    precio_unitario DECIMAL(10,2) NOT NULL,
    precio_venta    DECIMAL(10,2) NOT NULL,
    fecha_registro  DATE          NOT NULL DEFAULT (CURDATE()),
    imagen          VARCHAR(255),
    imagen_data     MEDIUMBLOB,
    imagen_tipo     VARCHAR(50),
    material_id     INT UNSIGNED  NOT NULL,
    categoria_id    INT UNSIGNED  NOT NULL,
    proveedor_id    INT UNSIGNED  NOT NULL,
    FOREIGN KEY (material_id)  REFERENCES Material(material_id),
    FOREIGN KEY (categoria_id) REFERENCES Categoria(categoria_id),
    FOREIGN KEY (proveedor_id) REFERENCES Proveedor(proveedor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- NUEVA TABLA: Producto_Subcategoria
-- Un producto pertenece a UNA categoría (en Producto)
-- y puede tener VARIAS subcategorías (aquí).
-- Solo se permiten combos válidos según Categoria_Subcategoria.
-- ============================================================
CREATE TABLE Producto_Subcategoria (
    producto_id     INT UNSIGNED NOT NULL,
    subcategoria_id INT UNSIGNED NOT NULL,
    PRIMARY KEY (producto_id, subcategoria_id),
    FOREIGN KEY (producto_id)     REFERENCES Producto(producto_id)          ON DELETE CASCADE,
    FOREIGN KEY (subcategoria_id) REFERENCES Subcategoria(subcategoria_id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MÓDULO 7: INVENTARIO
-- ============================================================
CREATE TABLE Inventario_Movimiento (
    movimiento_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    producto_id   INT UNSIGNED NOT NULL,
    usuario_id    INT UNSIGNED NULL,
    tipo          ENUM('entrada','salida','ajuste') NOT NULL,
    cantidad      INT          NOT NULL,
    fecha         DATETIME     NOT NULL DEFAULT NOW(),
    referencia    VARCHAR(255),
    FOREIGN KEY (producto_id) REFERENCES Producto(producto_id),
    FOREIGN KEY (usuario_id)  REFERENCES Usuario(usuario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MÓDULO 8: COMPRAS
-- ============================================================
CREATE TABLE Compra (
    compra_id     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    proveedor_id  INT UNSIGNED NOT NULL,
    fecha_compra  DATE         NOT NULL,
    fecha_entrega DATE         NOT NULL,
    FOREIGN KEY (proveedor_id) REFERENCES Proveedor(proveedor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Detalle_Compra (
    detalle_compra_id INT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    compra_id         INT UNSIGNED  NOT NULL,
    producto_id       INT UNSIGNED  NOT NULL,
    precio_unitario   DECIMAL(10,2) NOT NULL,
    cantidad          INT           NOT NULL,
    FOREIGN KEY (compra_id)   REFERENCES Compra(compra_id)    ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES Producto(producto_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MÓDULO 9: VENTAS
-- ============================================================
CREATE TABLE Venta (
    venta_id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id    INT UNSIGNED NOT NULL,
    cliente_id    INT UNSIGNED NOT NULL,
    fecha_emision DATE         NOT NULL DEFAULT (CURDATE()),
    FOREIGN KEY (usuario_id) REFERENCES Usuario(usuario_id),
    FOREIGN KEY (cliente_id) REFERENCES Cliente(cliente_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Detalle_Venta (
    detalle_venta_id INT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    venta_id         INT UNSIGNED  NOT NULL,
    producto_id      INT UNSIGNED  NOT NULL,
    cantidad         INT           NOT NULL,
    precio_unitario  DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (venta_id)    REFERENCES Venta(venta_id)        ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES Producto(producto_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MÓDULO 10: MÉTODOS DE PAGO
-- ============================================================
CREATE TABLE Metodo_Pago (
    metodo_pago_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre         VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Pago_Venta (
    pago_venta_id  INT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    venta_id       INT UNSIGNED  NOT NULL,
    metodo_pago_id INT UNSIGNED  NOT NULL,
    monto          DECIMAL(12,2) NOT NULL,
    fecha          DATETIME      NOT NULL DEFAULT NOW(),
    estado         ENUM('pendiente','confirmado') NOT NULL DEFAULT 'pendiente',
    FOREIGN KEY (venta_id)       REFERENCES Venta(venta_id)              ON DELETE CASCADE,
    FOREIGN KEY (metodo_pago_id) REFERENCES Metodo_Pago(metodo_pago_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Pago_Compra (
    pago_compra_id INT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    compra_id      INT UNSIGNED  NOT NULL,
    metodo_pago_id INT UNSIGNED  NOT NULL,
    monto          DECIMAL(12,2) NOT NULL,
    fecha          DATETIME      NOT NULL DEFAULT NOW(),
    estado         ENUM('pendiente','confirmado','rechazado') NOT NULL DEFAULT 'pendiente',
    FOREIGN KEY (compra_id)      REFERENCES Compra(compra_id)            ON DELETE CASCADE,
    FOREIGN KEY (metodo_pago_id) REFERENCES Metodo_Pago(metodo_pago_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MÓDULO 11: CRÉDITOS
-- ============================================================
CREATE TABLE Credito_Compra (
    credito_id        INT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    compra_id         INT UNSIGNED  NOT NULL UNIQUE,
    monto_total       DECIMAL(12,2) NOT NULL,
    saldo_pendiente   DECIMAL(12,2) NOT NULL,
    fecha_inicio      DATE          NOT NULL,
    fecha_vencimiento DATE          NOT NULL,
    estado            ENUM('activo','pagado','vencido') NOT NULL DEFAULT 'activo',
    FOREIGN KEY (compra_id) REFERENCES Compra(compra_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Abono_Credito (
    abono_id       INT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    credito_id     INT UNSIGNED  NOT NULL,
    metodo_pago_id INT UNSIGNED  NOT NULL,
    monto_abono    DECIMAL(12,2) NOT NULL,
    fecha          DATETIME      NOT NULL DEFAULT NOW(),
    estado         ENUM('pendiente','confirmado') NOT NULL DEFAULT 'pendiente',
    FOREIGN KEY (credito_id)     REFERENCES Credito_Compra(credito_id),
    FOREIGN KEY (metodo_pago_id) REFERENCES Metodo_Pago(metodo_pago_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MÓDULO 12: POSTVENTA
-- ============================================================
CREATE TABLE Caso_Postventa (
    caso_id  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    venta_id INT UNSIGNED NOT NULL,
    tipo     ENUM('cambio','devolucion','reclamo') NOT NULL,
    cantidad INT          NOT NULL,
    motivo   TEXT,
    fecha    DATE         NOT NULL DEFAULT (CURDATE()),
    estado   ENUM('en_proceso','aprobado','cancelado') NOT NULL DEFAULT 'en_proceso',
    FOREIGN KEY (venta_id) REFERENCES Venta(venta_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Historial_Caso_Postventa (
    historial_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    caso_id      INT UNSIGNED NOT NULL,
    estado       ENUM('en_proceso','aprobado','cancelado') NOT NULL,
    fecha        DATETIME     NOT NULL DEFAULT NOW(),
    observacion  TEXT,
    usuario_id   INT UNSIGNED NOT NULL,
    FOREIGN KEY (caso_id)    REFERENCES Caso_Postventa(caso_id),
    FOREIGN KEY (usuario_id) REFERENCES Usuario(usuario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- MÓDULO 13: AUDITORÍA
-- ============================================================
CREATE TABLE Auditoria_Log (
    log_id           INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id       INT UNSIGNED,
    accion           VARCHAR(100) NOT NULL,
    entidad          VARCHAR(50),
    entidad_id       INT UNSIGNED,
    datos_anteriores JSON,
    datos_nuevos     JSON,
    direccion_ip     VARCHAR(45),
    fecha_hora       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES Usuario(usuario_id) ON DELETE SET NULL,
    INDEX idx_usuario_fecha (usuario_id, fecha_hora),
    INDEX idx_accion        (accion),
    INDEX idx_entidad       (entidad),
    INDEX idx_fecha         (fecha_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;

