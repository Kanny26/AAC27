# Reglas de negocio — AAC27

> Reglas clave extraídas de `REQUISITOS_Y_ARQUITECTURA_AAC27.md` (§1 reglas de negocio y §3
> restricciones del diccionario). Cada regla debe terminar con ≥1 test al implementarse (Art. 4).
> Esta lista no es exhaustiva; se amplía al especificar cada feature.

## RN-001 — Dinero en decimal fijo
- **Tipo:** restricción. Todo campo monetario es `DECIMAL(14,2)` / `BigDecimal`. Prohibido `float`/`double`.
- **Usa:** todos los RF con dinero. **Ejemplo:** 10000.00 + 500.50 → 10500.50 (sin error de coma flotante).

## RN-002 — Stock nunca negativo
- **Tipo:** invariante. `producto.stock ≥ 0` siempre; toda modificación pasa por `inventario_movimiento` con `stock_antes`/`stock_despues`.
- **Ejemplos:** venta de 2 con stock 2 → stock 0 (ok); venta de 3 con stock 2 → rechazo 409. **Usa:** RF15, RF20, RF-V01.

## RN-003 — Precio de venta > precio de costo
- **Tipo:** validación. `producto.precio_venta > precio_costo`; mínimo configurable `costo × 2`.
- **Ejemplos:** costo 100.000, venta 250.000 → ok; costo 100.000, venta 90.000 → 400. **Usa:** RF-CAT01.

## RN-004 — Concurrencia de stock
- **Tipo:** restricción. Modificaciones de stock bajo `REPEATABLE_READ` + `@Version`/lock pesimista.
- **Ejemplo:** dos ventas simultáneas del último ítem → una gana, la otra recibe conflicto; nunca stock -1. **Usa:** RF15, RF-REN04, T-18.

## RN-005 — Puntos de fidelidad
- **Tipo:** cálculo. Acumula 1 punto por cada 10.000 COP de compra; canje 1 punto = 100 COP.
- **Ejemplos:** compra 125.000 → 12 puntos; canjear 50 puntos → 5.000 COP de descuento. **Usa:** RF-U02, RF-V03.

## RN-006 — Apartado: primer abono ≥ 20%
- **Tipo:** validación. `monto_primer_abono ≥ precio_acordado × 0.20`; `fecha_limite > hoy`; bloquea stock.
- **Ejemplos:** precio 1.000.000 → primer abono mínimo 200.000; abono 150.000 → 400. **Usa:** RF-V01.

## RN-007 — Ciclo de vida del apartado
- **Tipo:** transición de estado. `activo → completado` (pago total, genera venta) · `activo → cancelado` (libera stock) · `activo → vencido` (supera `fecha_limite`).
- **Usa:** RF-V01. Ver máquina de estados en `docs/domain/model.md`.

## RN-008 — Crédito de compra
- **Tipo:** transición. Un crédito activo por compra; `0 ≤ saldo ≤ total`; `activo → pagado` (saldo 0) · `activo → vencido` (pasa `fecha_vencimiento`). Abono ≤ saldo y solo a créditos activos.
- **Usa:** RF-C01, RF-C02, RF29-A, RF29-B.

## RN-009 — Venta: total calculado en backend
- **Tipo:** cálculo/seguridad. `total = Σ subtotales − descuentos`; el backend recalcula, nunca confía en el total del frontend. Descuento no produce precio negativo.
- **Usa:** RF20, RF-V02. **Ejemplo:** 2×(200.000−20.000) = 360.000.

## RN-010 — Inmutabilidad contable
- **Tipo:** restricción. Compra y venta guardadas no se modifican (integridad contable); correcciones vía postventa/ajuste. `numero_factura` consecutivo y único.
- **Usa:** RF11, RF18, RF19.

## RN-011 — Promoción vigente
- **Tipo:** validación temporal. Solo aplica entre `fecha_inicio` y `fecha_fin` con `activa=1`; límites de descuento configurados.
- **Ejemplos:** promo 2026-01-01..2026-01-31 aplicada el 2026-02-01 → rechazo. **Usa:** RF-V02.

## RN-012 — Roles inmutables y sin jerarquía
- **Tipo:** restricción. Roles base no se eliminan; `superadministrador` NO hereda permisos de negocio; alta de usuarios no crea superadministrador; admin no cambia su propio rol ni se desactiva.
- **Usa:** RF01, RF05, RF06, RF08. Ver `docs/matriz-permisos.md`.

## RN-013 — Política de contraseñas e historial
- **Tipo:** validación. Mín 8 chars, mayúscula y número; no reutilizar últimas 5; BCrypt antes de persistir; clave provisional fuerza cambio antes de operar.
- **Usa:** RF02, RF03, RF-S02. Implementado (T-07).

## RN-014 — Auditoría inmutable
- **Tipo:** restricción. `auditoria_log` no se edita ni borra; registra usuario, acción, entidad, datos anteriores/nuevos, IP, user-agent; retención ≥1 año.
- **Usa:** RF-S01, T-37.

## RN-015 — Integridad referencial por estado
- **Tipo:** restricción. No desactivar categoría/subcategoría/material con productos activos; no eliminar método de pago con transacciones; no eliminar cliente con ventas/apartados activos. Borrado lógico (cambio de estado), nunca físico de negocio.
- **Usa:** RF12, RF13, RF14, RF28, RF-U02.
