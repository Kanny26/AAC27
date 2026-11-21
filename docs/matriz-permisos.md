# Matriz de permisos (RBAC) — AAC27

> Estado: refleja `develop` tras el PR #9 (T-08). Cada cambio de permisos debe actualizar este
> documento y `RbacMatrizTest`.
> Referencias: RNF-SEG02, RF01, RF05, RF06, RF12–RF14, RF17–RF18, RF24–RF26.

## 1. Roles

| Rol | Propósito | Permisos de negocio |
|---|---|---|
| `superadministrador` | **Soporte técnico.** No opera la tienda. | **Ninguno heredado.** Solo endpoints abiertos a "cualquier usuario autenticado". No hay jerarquía de roles a propósito. |
| `administrador` | Dueño/gerente. | Todo el negocio. |
| `vendedor` | Atención en tienda. | Clientes, ventas, apartados, consulta de promociones vigentes y postventa (abrir y consultar). |

El alta de usuarios no permite crear `superadministrador` (cubierto en `AuthFlujoTest`).

## 2. Cómo se aplica

- **Roles:** `@PreAuthorize` en la clase o el método del controller. El de método tiene prioridad sobre
  el de clase. Sin `@PreAuthorize`, basta con estar autenticado (`anyRequest().authenticated()`).
- **Filtro por propietario:** en el servicio o controller, para los casos donde el rol no basta
  (ver sección 4).
- **Clave provisional (RF02):** mientras `es_password_temporal = true`, cualquier usuario solo puede
  llamar a `GET /auth/me`, `POST /auth/cambiar-password`, `POST /auth/login` y
  `POST /auth/refresh-token`. El resto responde 403 con código `PASSWORD_CHANGE_REQUIRED`
  (`CambioPasswordObligatorioFilter`, lista blanca).
- **Respuestas:** sin token → 401; token válido sin el rol → 403.

## 3. Matriz por endpoint

Leyenda: **Sí** puede · **No** recibe 403 · **Público** no requiere token · **Auth** cualquier usuario
autenticado, sin distinguir rol.

### Autenticación — `/api/v1/auth`

| Endpoint | Superadmin | Admin | Vendedor | Notas |
|---|---|---|---|---|
| `POST /login` | Público | Público | Público | |
| `POST /refresh-token` | Público | Público | Público | Solo acepta tokens de tipo refresh. |
| `GET /me` | Sí | Sí | Sí | Auth. |
| `POST /cambiar-password` | Sí | Sí | Sí | Auth. Aplica la política de contraseñas y el historial (RF-S02). |
| `POST /registro` | No | Sí | No | RF06. |

### Catálogo — `/api/v1/categorias`

| Endpoint | Superadmin | Admin | Vendedor | Notas |
|---|---|---|---|---|
| `GET /`, `GET /{id}` | Sí | Sí | Sí | Auth. Lectura abierta: la necesita el POS. |
| `POST /`, `PUT /{id}`, `PATCH /{id}/estado` | No | Sí | No | RF12. |

### Clientes — `/api/v1/clientes`

| Endpoint | Superadmin | Admin | Vendedor | Notas |
|---|---|---|---|---|
| `GET /`, `GET /{id}`, `POST /`, `PUT /{id}` | No | Sí | Sí | Sin filtro por propietario. |

### Promociones — `/api/v1/promociones`

| Endpoint | Superadmin | Admin | Vendedor | Notas |
|---|---|---|---|---|
| `GET /vigentes`, `GET /producto/{productoId}` | No | Sí | Sí | El vendedor solo consulta; no crea descuentos. |
| `GET /`, `GET /{id}` | No | Sí | No | |
| `POST /`, `PUT /{id}`, `PATCH /{id}/activar`, `PATCH /{id}/desactivar` | No | Sí | No | |

### Compras — `/api/v1/compras`

| Endpoint | Superadmin | Admin | Vendedor | Notas |
|---|---|---|---|---|
| `POST /`, `GET /`, `GET /{id}` | No | Sí | No | RF11. |

### Ventas — `/api/v1/ventas`

| Endpoint | Superadmin | Admin | Vendedor | Notas |
|---|---|---|---|---|
| `GET /` | No | Sí (todas) | Sí (solo las suyas) | RF17. |
| `GET /{id}` | No | Sí | Sí (solo las suyas) | RF18. |
| `GET /cliente/{clienteId}` | No | Sí | Sí (**sin filtro**) | Ver decisión pendiente D1. |
| `POST /`, `POST /apartados` | No | Sí | Sí | RF20. |

### Apartados — `/api/v1/apartados`

| Endpoint | Superadmin | Admin | Vendedor | Notas |
|---|---|---|---|---|
| `GET /`, `GET /cliente/{clienteId}`, `GET /{id}`, `POST /{id}/abonos`, `POST /{id}/cancelar` | No | Sí | Sí | Sin filtro por propietario (ver D2). |

### Postventa — `/api/v1/postventa`

| Endpoint | Superadmin | Admin | Vendedor | Notas |
|---|---|---|---|---|
| `POST /` | No | Sí | Sí | Abre un caso. |
| `PUT /{id}/estado` | No | Sí | **No** | RF25: gestionar casos es del Administrador. |
| `GET /buscar?factura=` | No | Sí (cualquier factura) | Sí (solo facturas de sus ventas) | RF26, solo lectura. |
| `GET /mis-casos` | No | Sí | Sí | Casos que abrió el usuario. |

### Abiertos sin token (infraestructura)

`/actuator/health`, `/actuator/info`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` y las
peticiones `OPTIONS` (preflight CORS).

## 4. Reglas por propietario (más allá del rol)

| Regla | Dónde vive | Estado |
|---|---|---|
| Vendedor ve solo sus ventas (lista y detalle) — RF17/RF18 | `VentaController` | Implementada. |
| Vendedor busca casos solo de sus ventas — RF26 | `PostventaService.buscarPorFactura` | Implementada, con test unitario. |
| Vendedor ve solo los casos que abrió | `PostventaService.listarPorVendedor` | Implementada. |
| Vendedor ve ventas de un cliente | `VentaController.ventasPorCliente` | **Sin filtro** (D1). |
| Vendedor ve apartados de otros vendedores | `ApartadoService` | **Sin filtro** (D2). |

## 5. Decisiones y pendientes

- **D1.** `GET /ventas/cliente/{id}` devuelve todas las ventas del cliente sin importar qué vendedor las
  registró. Su comentario dice que sirve para ver el historial al armar una venta (RF-B01), pero RF17
  limita al vendedor a sus propias ventas. Hay que decidir si se filtra o si se acepta como excepción.
- **D2.** Los requisitos de apartados (RF-V01) no limitan al vendedor a los suyos. Confirmar que es lo deseado.
- **D3.** Swagger y `/v3/api-docs` son públicos en todos los perfiles. Decidir en T-12/T-34 si se cierran en `prod`.
- **Controllers que aún no existen** (Proveedor, Producto, Material, Subcategoría, métodos de pago):
  T-10 y T-11 deben aplicar RBAC al crearlos y agregar sus filas aquí y en `RbacMatrizTest`.

## 6. Cobertura de pruebas

`RbacMatrizTest` recorre 13 endpoints con los tres roles y sin token. Usa endpoints sin cuerpo: Spring
valida el cuerpo (`@Valid`) antes de ejecutar `@PreAuthorize`, y un cuerpo inválido daría 400 en lugar de
403. Los endpoints con cuerpo que no están en la matriz (por ejemplo `POST /compras` o `POST /ventas`)
dependen de la anotación de clase, igual que los que sí se prueban.
