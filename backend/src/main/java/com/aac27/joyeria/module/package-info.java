/**
 * Paquete raíz de módulos de negocio.
 *
 * <p>Cada submódulo sigue la estructura de capas (Sección 4.1):
 * <pre>
 * module/
 * ├── seguridad/         -- RF01-RF05, RF-S01, RF-S02
 * │   ├── controller/
 * │   ├── service/
 * │   ├── repository/
 * │   ├── dto/
 * │   └── entity/
 * ├── usuario/           -- RF06-RF08, RF-U01, RF-U02
 * ├── proveedor/         -- RF09-RF11, RF-P01
 * ├── compra/            -- RF11-RF-C03
 * ├── catalogo/          -- RF12-RF-CAT05
 * ├── inventario/        -- RF15-RF-INV03
 * ├── venta/             -- RF17-RF-V04
 * ├── postventa/         -- RF24-RF-PV02
 * ├── pago/              -- RF28-RF-PAG02
 * └── reporte/           -- RF30-RF-R03
 * </pre>
 *
 * <p>Esta organización por módulos facilita escalar a microservicios
 * en el futuro si el sistema crece más allá de Fase 5.
 */
package com.aac27.joyeria.module;
