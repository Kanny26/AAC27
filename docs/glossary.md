# Glosario (lenguaje ubicuo) — AAC27

> Un concepto = un nombre en todo el sistema (documentos, código, base de datos, UI).
> Fuente canónica de identificadores: diccionario de datos de `REQUISITOS_Y_ARQUITECTURA_AAC27.md` §3.
> El código vive en `com.aac27.joyeria`; comentarios en español, identificadores en inglés salvo los
> términos de dominio ya fijados en español en la BD (tablas/columnas) que se conservan.

| Término de negocio | Identificador en código / BD | Definición | Sinónimos prohibidos | Fuente |
|---|---|---|---|---|
| Usuario | `usuario` / `Usuario` | Trabajador con acceso al sistema (admin o vendedor). | empleado, cuenta | §3.1 |
| Rol | `rol` / `Rol.NombreRol` | `superadministrador`, `administrador`, `vendedor`. Inmutables. | perfil, grupo | §3.2 |
| Cliente | `cliente` / `Cliente` | Comprador. CRM básico con puntos de fidelidad. | comprador | §3.3 |
| Proveedor | `proveedor` / `Proveedor` | Suministra materiales/productos. | vendedor externo | §3.10 |
| Categoría | `categoria` | Agrupación de productos (Anillos, Collares…). | tipo | §3.4 |
| Subcategoría | `subcategoria` | Ocasión/uso dentro de categoría (15 años, Matrimonio…). | — | §3.5 |
| Material | `material` | Metal/insumo (Plata, Oro…). `es_trazable` marca metales preciosos. | metal | §3.6 |
| Producto | `producto` / `Producto` | Artículo del catálogo. SKU `codigo` único. | artículo, ítem, joya | §3.7 |
| Trazabilidad | `producto_trazabilidad` | Ley, quilates, piedras de un producto trazable. | — | §3.8 |
| Compra | `compra` | Orden de compra a proveedor. Incrementa stock. | orden | §3.14 |
| Venta | `venta` | Transacción de salida. `numero_factura` consecutivo. Descuenta stock. | factura, pedido | §3.16 |
| Apartado | `apartado` | Layaway: reserva con abonos; bloquea stock; factura al completar pago. | reserva, layaway | §3.19 |
| Promoción | `promocion` | Descuento con vigencia (porcentaje o monto fijo). | descuento, oferta | §3.28 |
| Caso postventa | `caso_postventa` | Reclamo/devolución/cambio/reparación ligado a venta o compra. | ticket, PQR | §3.20 |
| Reparación | `reparacion` | Extensión de caso postventa con presupuesto/estado de taller. | arreglo, servicio | §3.21 |
| Movimiento de inventario | `inventario_movimiento` | Entrada/salida/ajuste/reserva/liberación con `stock_antes`/`stock_despues`. | kardex (es el conjunto) | §3.22 |
| Kardex | — (consulta sobre `inventario_movimiento`) | Historial de movimientos de un producto. | libro de inventario | RF16 |
| Método de pago | `metodo_pago` | Efectivo, transferencia, etc. | forma de pago | §3.23 |
| Crédito de compra | `credito_compra` | Saldo pendiente de una compra a crédito. | cuenta por pagar | §3.24 |
| Abono | `abono_credito_compra` / `pago_venta` | Pago parcial a un crédito o venta. | cuota | §3.25/§3.18 |
| Puntos de fidelidad | `puntos_fidelidad` | 1 pto por cada 10.000 COP; canje 1 pto = 100 COP. | millas, recompensa | RF-V03 |
| Auditoría | `auditoria_log` | Registro inmutable de acciones críticas. | bitácora, historial editable | §3.26 |
| Token de recuperación | `token_recuperacion` | Código 6 dígitos, hash, un solo uso, 15 min. | OTP | §3.27 |
