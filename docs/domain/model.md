# Modelo de dominio — AAC27

> Derivado del diccionario de datos de `REQUISITOS_Y_ARQUITECTURA_AAC27.md` §3 (29 tablas) y de las
> reglas de estado ya codificadas. Fuente de verdad del esquema: migraciones Flyway `V1…V10`.

## Entidades clave (invariantes)
- **Producto** — SKU `codigo` único; `precio_venta > precio_costo` (RN-003); `stock ≥ 0` (RN-002); `@Version` para concurrencia (RN-004).
- **Venta** — `numero_factura` consecutivo único; inmutable tras registrar (RN-010); total recalculado en backend (RN-009).
- **Apartado** — primer abono ≥20% (RN-006); bloquea stock; estados RN-007.
- **Crédito de compra** — un activo por compra; `0 ≤ saldo ≤ total` (RN-008).
- **Inventario_movimiento** — append-only; guarda `stock_antes`/`stock_despues` (desnormalización intencional).
- **Auditoria_log** — inmutable (RN-014).

## Relaciones (ER simplificado)
```mermaid
erDiagram
  ROL ||--o{ USUARIO : tiene
  USUARIO ||--o{ VENTA : registra
  CLIENTE ||--o{ VENTA : compra
  VENTA ||--o{ DETALLE_VENTA : contiene
  VENTA ||--o{ PAGO_VENTA : recibe
  PRODUCTO ||--o{ DETALLE_VENTA : aparece
  CATEGORIA ||--o{ PRODUCTO : clasifica
  MATERIAL ||--o{ PRODUCTO : compone
  PROVEEDOR ||--o{ PRODUCTO : suministra
  PROVEEDOR ||--o{ COMPRA : abastece
  COMPRA ||--o{ DETALLE_COMPRA : contiene
  COMPRA ||--o| CREDITO_COMPRA : genera
  CREDITO_COMPRA ||--o{ ABONO_CREDITO_COMPRA : recibe
  PRODUCTO ||--o{ INVENTARIO_MOVIMIENTO : registra
  CLIENTE ||--o{ APARTADO : reserva
  APARTADO ||--o| VENTA : "se convierte en"
  VENTA ||--o{ CASO_POSTVENTA : origina
  CASO_POSTVENTA ||--o| REPARACION : extiende
```

## Ciclos de vida (máquinas de estado)

### Venta — `estado_venta`
```mermaid
stateDiagram-v2
  [*] --> activa
  activa --> cancelada: cancelación
  activa --> devuelta: devolución total [RF25]
```
Pago: `estado_pago` pendiente → pagado (saldo 0) · pendiente → vencido (plazo superado).

### Apartado — `estado` [RN-007]
```mermaid
stateDiagram-v2
  [*] --> activo: primer abono >=20% [RN-006]
  activo --> completado: pago total -> genera venta
  activo --> cancelado: libera stock
  activo --> vencido: supera fecha_limite
```

### Crédito de compra — `estado` [RN-008]
```mermaid
stateDiagram-v2
  [*] --> activo
  activo --> pagado: saldo = 0
  activo --> vencido: supera fecha_vencimiento
```

### Caso postventa — `estado`
```mermaid
stateDiagram-v2
  [*] --> en_proceso
  en_proceso --> aprobado
  en_proceso --> rechazado
  aprobado --> cerrado
  rechazado --> cerrado
```
Reparación (si `tipo=reparacion`): recibido → en_proceso → listo → entregado.

## Clasificación de datos
| Dato | Clase | Cifrado en reposo | Retención |
|---|---|---|---|
| password_hash | Restringido | BCrypt (hash) | vida de la cuenta |
| token_recuperacion | Restringido | hash | 15 min |
| datos cliente (documento, contacto) | Confidencial | — (BD protegida) | según normativa |
| auditoria_log | Interno | — | ≥1 año |
| catálogo, precios | Interno | — | histórico |
