# Requisitos funcionales — AAC27

> Vista estructurada en formato GENESIS, **derivada** de `REQUISITOS_Y_ARQUITECTURA_AAC27.md` §1,
> que sigue siendo la fuente de detalle (reglas de negocio completas, restricciones). Los IDs son
> canónicos y no cambian. Patrón EARS resumido en la columna Requisito.

## Módulo Seguridad y Perfiles
| ID | Nombre | Prioridad | Actor(es) | Requisito (resumen) |
|---|---|---|---|---|
| RF01 | Gestionar Roles y Permisos | Alta | Admin | El sistema evalúa el rol en cada request (RBAC); roles base inmutables; un rol activo por usuario. |
| RF02 | Inicio de Sesión | Alta | Admin, Vendedor | Valida usuario/contraseña; emite JWT (8h) + refresh (7d); detecta clave provisional y fuerza cambio. |
| RF03 | Recuperación de Contraseña | Alta | Admin, Vendedor | Código 6 dígitos por correo, válido 15 min, uso único; máx 3 intentos. |
| RF04 | Cierre de Sesión | Alta | Admin, Vendedor | Invalida JWT (lista negra/rotación); limpia storage; cabeceras anti-caché. |
| RF05 | Control de Estado del Usuario | Alta | Admin | Verifica activo/inactivo en cada autenticación; bloqueo en tiempo real; admin no se autodesactiva. |
| RF-S01 | Registro de Auditoría | Alta | Admin | Registra acciones críticas (inmutable, IP, user-agent, retención ≥1 año). |
| RF-S02 | Política de Contraseñas | Alta | Admin | Longitud/complejidad/historial (últimas 5); validación front + back; BCrypt. |

## Módulo Usuarios
| ID | Nombre | Prioridad | Actor(es) | Requisito (resumen) |
|---|---|---|---|---|
| RF06 | Registrar Usuarios | Alta | Admin | Crea cuenta con rol/estado; genera contraseña provisional y la envía por correo; correo/documento únicos. |
| RF07 | Listar Usuarios | Alta | Admin | Listado paginado server-side con búsqueda y filtros por estado/rol. |
| RF08 | Editar Usuarios | Alta | Admin | Edita nombre/teléfono/rol/estado; correo y documento inmutables; admin no cambia su propio rol. |
| RF-U01 | Perfil de Usuario | Media | Admin, Vendedor | Cada usuario edita su propio perfil (foto ≤2MB); cambio de contraseña exige la actual. |
| RF-U02 | Gestión de Clientes (CRM) | Alta | Admin, Vendedor | Cliente por documento único; historial de solo lectura; puntos de fidelidad automáticos. |

## Módulo Proveedores
| ID | Nombre | Prioridad | Actor(es) | Requisito (resumen) |
|---|---|---|---|---|
| RF09 | Listar Proveedores | Alta | Admin | Listado paginado con búsqueda por nombre/material y contadores. |
| RF10 | Registrar Proveedor | Alta | Admin | NIT único; ≥1 teléfono y ≥1 correo; ≥1 material; fecha inicio no futura. |
| RF11-E | Editar Proveedor | Alta | Admin | Edita monto mínimo/contacto/materiales/estado; documento inmutable. |
| RF-P01 | Evaluación de Proveedores | Baja | Admin | Evaluaciones periódicas (calidad/entrega/precio) escala 1-5; promedio ponderado. |

## Módulo Compras
| ID | Nombre | Prioridad | Actor(es) | Requisito (resumen) |
|---|---|---|---|---|
| RF11 | Gestionar Órdenes de Compra | Alta | Admin | Crea/visualiza/descarga; totales automáticos; al guardar incrementa stock; PDF; inmutable tras guardar. |
| RF-C01 | Compra a Crédito | Alta | Admin | Método 'crédito' crea `credito_compra` con saldo = total; estados activo/pagado/vencido. |
| RF-C02 | Abonos a Créditos de Compra | Alta | Admin | Abono ≤ saldo; solo créditos activos; registro con fecha y método. |
| RF-C03 | Recepción Parcial de Compra | Media | Admin | Recepciones múltiples; estado recibido_parcial/completo; stock por cantidad recibida. |

## Módulo Catálogo
| ID | Nombre | Prioridad | Actor(es) | Requisito (resumen) |
|---|---|---|---|---|
| RF12 | Gestionar Categorías | Media | Admin | CRUD; nombre único; no desactivar con productos activos. |
| RF13 | Gestionar Subcategorías | Media | Admin | CRUD por categoría; nombre único en la categoría. |
| RF14 | Gestionar Materiales | Media | Admin | CRUD; nombre único; no desactivar con productos activos. |
| RF-CAT01 | Gestionar Productos | Alta | Admin | CRUD con SKU único; precio venta > costo; stock ≥0; lote/serie para metales preciosos. |
| RF-CAT02 | Trazabilidad de Metales/Piedras | Media | Admin | Ley/quilates/certificado/piedras para materiales trazables. |
| RF-CAT03 | Certificado de Autenticidad | Media | Admin, Vendedor | PDF inmutable por producto; vinculable a venta. |
| RF-CAT04 | Gestionar Tallas y Medidas | Media | Admin | Catálogo de tallas por categoría; variantes con stock independiente. |
| RF-CAT05 | Reserva Temporal de Producto | Media | Admin, Vendedor | Reserva ≤48h que bloquea stock; expiración libera; no doble reserva. |

## Módulo Inventario
| ID | Nombre | Prioridad | Actor(es) | Requisito (resumen) |
|---|---|---|---|---|
| RF15 | Control de Inventario | Alta | Sistema | Stock automático ante compra/venta/devolución/ajuste; nunca negativo; todo en `inventario_movimiento`. |
| RF16 | Historial de Movimientos | Alta | Admin | Consulta filtrable (producto/tipo/fechas), solo lectura, referencia legible al documento origen. |
| RF-INV01 | Alertas de Stock Bajo | Media | Admin | Alerta en dashboard/correo bajo umbral por producto (default 5); una vez por evento. |
| RF-INV02 | Ajuste Manual de Inventario | Media | Admin | Ajuste con motivo ≥20 chars; registra tipo 'ajuste' y usuario. |
| RF-INV03 | Conteo Físico / Cíclico | Baja | Admin | Conteo en borrador; al confirmar genera ajustes; historial almacenado. |

## Módulo Ventas
| ID | Nombre | Prioridad | Actor(es) | Requisito (resumen) |
|---|---|---|---|---|
| RF17 | Listar Ventas | Alta | Admin, Vendedor | Paginado; Admin ve todas, Vendedor solo las propias; filtros. |
| RF18 | Consultar Detalle de Venta | Alta | Admin, Vendedor | Detalle inmutable; Vendedor solo las suyas; link a factura PDF. |
| RF19 | Descargar Factura PDF | Alta | Admin, Vendedor | PDF con número consecutivo único y QR. |
| RF20 | Registrar Venta | Alta | Admin, Vendedor | Cliente, productos, modalidad (contado/anticipo/crédito), descuentos; descuenta stock al confirmar. |
| RF21 | Seguimiento de Anticipos | Alta | Admin, Vendedor | Anticipo < total; saldo pendiente; comprobante por pago. |
| RF-V01 | Apartado / Layaway | Alta | Admin, Vendedor | Reserva con abonos; primer abono ≥20%; bloquea stock; factura al completar. |
| RF-V02 | Descuentos y Promociones | Media | Admin, Vendedor | % o monto fijo con vigencia; nunca precio negativo; cálculo en backend. |
| RF-V03 | Programa de Fidelidad | Media | Admin, Vendedor | 1 pto/10.000 COP; canje 1 pto=100 COP; registrado en detalle. |
| RF-V04 | Garantías de Producto | Media | Admin, Vendedor | Garantía por ítem (duración/cobertura/vencimiento); comprobante PDF. |

## Módulo Postventa
| ID | Nombre | Prioridad | Actor(es) | Requisito (resumen) |
|---|---|---|---|---|
| RF24 | Listar Casos Postventa | Media | Admin, Vendedor | Clasificados por tipo/estado/fecha; Admin todos, Vendedor solo los de sus ventas. |
| RF25 | Gestionar Casos Postventa | Media | Admin | Registra/actualiza/cierra; devoluciones generan 'entrada'; cambios ajustan stock; historial de estado. |
| RF26 | Consulta Vendedor Postventa | Media | Vendedor | Solo lectura por número de factura de sus ventas. |
| RF-PV01 | Gestión de Reparaciones y Ajustes | Alta | Admin, Vendedor | Presupuesto aprobado antes de iniciar; estados recibido→entregado; no toca inventario del catálogo. |
| RF-PV02 | Notificaciones de Estado de Caso | Baja | Sistema, Admin | Correo al cliente al cambiar estado; log de notificaciones. |

## Módulo Pagos y Créditos
| ID | Nombre | Prioridad | Actor(es) | Requisito (resumen) |
|---|---|---|---|---|
| RF28 | Gestionar Métodos de Pago | Media | Admin | CRUD; nombres únicos; no eliminar con transacciones (solo desactivar). |
| RF29-A | Gestionar Créditos de Compra | Alta | Admin | Un crédito activo por compra; 0 ≤ saldo ≤ total; estados automáticos. |
| RF29-B | Registrar Abonos a Créditos | Alta | Admin | Abono ≤ saldo; solo créditos activos; registro con fecha/monto/método. |
| RF-PAG01 | Conciliación de Caja | Media | Admin | Cierre diario por método vs ventas; diferencias anotadas; no modifica ventas. |
| RF-PAG02 | Historial de Pagos por Venta | Media | Admin, Vendedor | Solo lectura; Admin todos, Vendedor solo sus ventas. |

## Módulo Reportes
| ID | Nombre | Prioridad | Actor(es) | Requisito (resumen) |
|---|---|---|---|---|
| RF30 | Generar y Exportar Reportes | Media | Admin | Ventas/inventario/movimientos/desempeño; exporta PDF/Excel; exportación asíncrona en grandes. |
| RF-R01 | Dashboard con KPIs | Alta | Admin, Vendedor | KPIs en vivo (caché 5 min); Admin global, Vendedor propios. |
| RF-R02 | Reporte de Rentabilidad | Media | Admin | Margen por producto/categoría/período con precios históricos del detalle. |
| RF-R03 | Reporte de Fidelidad | Baja | Admin | Puntos acumulados/canjeados e historial. |

## Módulo Búsqueda
| ID | Nombre | Prioridad | Actor(es) | Requisito (resumen) |
|---|---|---|---|---|
| RF31 | Buscador Avanzado Global | Media | Admin, Vendedor | Tiempo real (debounce 300ms) multi-módulo; filtra según permisos; paginado. |
| RF-B01 | Búsqueda de Clientes | Alta | Admin, Vendedor | Autocompletado por nombre/documento/teléfono con historial resumido. |
| RF-B02 | Búsqueda de Productos en Venta | Alta | Admin, Vendedor | Por nombre/SKU/categoría/material; solo activos con stock > 0. |
