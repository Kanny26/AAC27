# Roles y permisos — AAC27

> Matriz **por endpoint** (fuente operativa viva): `docs/matriz-permisos.md`, sincronizada con
> `RbacMatrizTest`. Este documento da la vista de alto nivel (roles, ámbito de datos, auditoría)
> en formato GENESIS D.5. Referencias: RNF-SEG02, RF01, RF05, RF06, RF17–RF18, RF24–RF26.

## Roles
| Rol | Descripción | Ámbito de datos |
|---|---|---|
| `superadministrador` | Soporte técnico. No opera la tienda. **Sin permisos de negocio heredados** (no hay jerarquía a propósito). | Solo endpoints "cualquier autenticado". |
| `administrador` | Dueño/gerente. Opera todo el negocio. | Todos los registros. |
| `vendedor` | Atención en tienda. | Clientes, ventas (solo las propias), apartados, promociones vigentes (consulta), postventa (abrir/consultar sus casos). |

## Matriz de permisos (resumen por recurso)
Detalle y notas en `docs/matriz-permisos.md`. Leyenda: ✅ sí · ❌ 403 · Auth (cualquier autenticado) · Pub (público).

| Recurso / acción | Superadmin | Admin | Vendedor |
|---|---|---|---|
| Auth: login / refresh | Pub | Pub | Pub |
| Auth: /me, cambiar-password | Auth | Auth | Auth |
| Usuarios: registro (RF06) | ❌ | ✅ | ❌ |
| Catálogo: lectura | Auth | Auth | Auth |
| Catálogo: crear/editar/estado | ❌ | ✅ | ❌ |
| Clientes: CRUD | ❌ | ✅ | ✅ |
| Promociones: consultar vigentes | ❌ | ✅ | ✅ |
| Promociones: crear/editar | ❌ | ✅ | ❌ |
| Compras: todo | ❌ | ✅ | ❌ |
| Ventas: listar/ver | ❌ | ✅ todas | ✅ solo propias |
| Ventas/Apartados: crear | ❌ | ✅ | ✅ |
| Postventa: abrir caso | ❌ | ✅ | ✅ |
| Postventa: cambiar estado (RF25) | ❌ | ✅ | ❌ |
| Postventa: buscar por factura (RF26) | ❌ | ✅ cualquiera | ✅ solo sus ventas |
| Proveedor / Producto / Material (T-10/T-11) | ❌ | ✅ | ❌ (pendiente de implementar y agregar a la matriz) |

## Reglas de visibilidad por propietario (más allá del rol)
- Vendedor ve solo **sus** ventas (lista y detalle) — RF17/RF18 (implementado).
- Vendedor busca casos postventa solo de **sus** ventas — RF26 (implementado, con test).
- **Pendientes** (ver `matriz-permisos.md` §5): D1 ventas por cliente sin filtro; D2 apartados sin filtro por vendedor; D3 Swagger público en prod.

## Segregación de funciones y auditoría
- **Clave provisional (RF02):** con `es_password_temporal=true` solo se permite `/auth/me`, `cambiar-password`, `login`, `refresh-token`; el resto → 403 `PASSWORD_CHANGE_REQUIRED`.
- **Acciones que generan auditoría (RF-S01, pendiente T-37):** login, CRUD usuarios, compras, ventas, cambios de stock, promociones, cambios de estado. Registro inmutable (quién, qué, cuándo, valor anterior/nuevo).
- **Restricciones de segregación:** admin no cambia su propio rol ni se autodesactiva (RF05/RF08); alta no crea superadministrador (RN-012).
