REQUISITOS Y ARQUITECTURA
Sistema de Gestión de Joyería AAC27
Requisitos Funcionales Completos | Requisitos No Funcionales
Modelo de Datos Mejorado | Lineamientos de Migración Spring Boot + React
Basado en el Manual Técnico AAC27 — SENA ADSO 2994281
Versión: 2.0 — Abril 2026
 
⚠️ Suposiciones Consideradas
Las siguientes suposiciones técnicas y de negocio se adoptaron al elaborar este documento:
•	La moneda base del sistema es el Peso Colombiano (COP). Los campos monetarios usan DECIMAL(14,2).
•	Se asume un único negocio/tienda (single-tenant). Multi-tenancy queda fuera del alcance inicial.
•	Los roles permanecen en dos niveles operativos (Administrador, Vendedor) más el rol técnico superadministrador ya existente en la BD.
•	El módulo CRM básico gestiona clientes sin campaña de marketing; se limita a historial de compras, preferencias y puntos de fidelidad.
•	Los certificados de autenticidad y garantías se emiten como documentos PDF generados internamente; no se integra con laboratorios externos en v1.
•	Las reparaciones/ajustes se modelan como un tipo especial de caso postventa con flujo propio.
•	El apartado (layaway) es un acuerdo de reserva con abonos periódicos; no genera factura final hasta completar el pago.
•	Los números de serie/lote se aplican a productos de alto valor (oro, plata fina, piedras preciosas); otros productos pueden omitirlos.
•	La infraestructura de despliegue objetivo es un VPS o instancia cloud con Docker; no se asume Kubernetes en v1.
•	JWT se usa para autenticación stateless; los refresh tokens se almacenan en Redis o en tabla BD.
•	Las imágenes de productos se almacenan en almacenamiento de objetos (S3-compatible) o sistema de archivos local según configuración.
•	Las tallas/medidas son texto libre o código estándar (p.ej. '16 mm', '7 US'); no se integra con estándares internacionales en v1.

 
1. Requisitos Funcionales Actualizados por Módulo
Cada requisito incluye: ID, Nombre, Descripción, Reglas de Negocio, Prioridad, Restricciones y Rol(es) involucrado(s).

1.1 Módulo de Seguridad y Perfiles

ID	Nombre	Descripción	Reglas de Negocio	Prioridad	Restricciones	Rol(es)
RF01	Gestionar Roles y Permisos	El sistema define roles (Administrador, Vendedor) con menús y acciones diferenciadas. Cada usuario ve únicamente las opciones de su rol.	Los roles no pueden eliminarse para garantizar trazabilidad. El rol se evalúa en cada request (RBAC). Un usuario solo puede tener un rol activo simultáneo.	Alta	Los roles base son inmutables. Solo el Administrador puede asignar/cambiar roles.	Administrador
RF02	Inicio de Sesión	Validar identidad mediante usuario/contraseña. Detectar primer acceso con clave provisional y forzar cambio.	Contraseñas almacenadas con BCrypt (cost≥12). Bloqueo de cuenta inactiva en tiempo real. Token JWT emitido tras autenticación exitosa (exp: 8h). Refresh token (exp: 7d).	Alta	Cuentas Inactivas no pueden autenticarse aunque la contraseña sea correcta.	Administrador, Vendedor
RF03	Recuperación de Contraseña	Flujo de restablecimiento vía correo electrónico con código temporal de 6 dígitos.	Código válido 15 minutos, uso único. Almacenado con hash en BD. Nueva contraseña debe cumplir política: mín. 8 caracteres, mayúscula, número. Se invalidan sesiones activas al cambiar contraseña.	Alta	Máximo 3 intentos de código fallidos antes de invalidar y requerir nuevo envío.	Administrador, Vendedor
RF04	Cierre de Sesión	Finalizar sesión activa; eliminar datos temporales y redirigir al login.	Invalidar JWT en lista negra (Redis) o mediante rotación de token family. Limpiar cookies/storage. Prevenir acceso con botón 'Atrás' del navegador (cabeceras Cache-Control).	Alta	Acceso directo a URL interna sin sesión válida redirige al login.	Administrador, Vendedor
RF05	Control de Estado del Usuario	Verificar estado Activo/Inactivo en cada autenticación.	El bloqueo aplica en tiempo real al actualizar la BD. Las sesiones activas de un usuario inactivado se invalidan en la siguiente petición.	Alta	Solo Administrador puede cambiar estado. Un admin no puede desactivar su propia cuenta.	Administrador
RF-S01	Registro de Auditoría	Registrar todas las acciones críticas (login, CRUD, cambios de estado, generación de documentos) con usuario, entidad, acción, datos anteriores y nuevos.	Registro inmutable; no se pueden editar ni eliminar logs. Incluye IP del cliente y user-agent. Retención mínima 1 año.	Alta	Solo lectura para Administrador. No expuesto al Vendedor.	Administrador
RF-S02	Política de Contraseñas	Aplicar política configurable: longitud mínima, complejidad, historial (últimas 5), expiración opcional.	Validación en frontend (inmediata) y backend (Bean Validation). Hash BCrypt antes de persistir. Prohibir reutilización de las últimas 5 contraseñas.	Alta	Política gestionada por Administrador. Cambio de política no afecta sesiones activas.	Administrador

1.2 Módulo de Usuarios

ID	Nombre	Descripción	Reglas de Negocio	Prioridad	Restricciones	Rol(es)
RF06	Registrar Usuarios	El Administrador crea cuentas con datos básicos, rol y estado inicial. El sistema genera contraseña provisional y la envía por correo.	Correo y número de documento únicos. Contraseña provisional generada con UUID aleatorio + hash BCrypt. Log de auditoría al crear.	Alta	Solo Administrador. Campos obligatorios: nombre, documento, correo, teléfono, rol.	Administrador
RF07	Listar Usuarios	Vista organizada con tarjetas, búsqueda, filtros por estado/rol y contadores resumen.	Paginación server-side (página 20 ítems). Búsqueda por nombre, correo, rol. Filtros combinables.	Alta	Solo Administrador ve todos los usuarios.	Administrador
RF08	Editar Usuarios	Actualizar nombre, teléfono, rol y estado. Correo y documento son inmutables tras la creación.	Cambios reflejados en tiempo real. El admin no puede cambiar su propio rol ni desactivar su cuenta. Log de auditoría al editar.	Alta	Correo y documento no editables.	Administrador
RF-U01	Perfil de Usuario	Cada usuario puede ver y actualizar su propio perfil: nombre, teléfono, foto de perfil. No puede cambiar su propio rol ni estado.	Foto de perfil: JPG/PNG ≤ 2 MB. Actualización de contraseña requiere confirmación de la actual.	Media	Usuario solo edita su propio perfil.	Administrador, Vendedor
RF-U02	Gestión de Clientes (CRM Básico)	Registrar y mantener la información de clientes: nombre, documento, contacto, dirección, historial de compras, preferencias y puntos de fidelidad.	Cliente identificado por documento único. El historial de compras es de solo lectura. Los puntos de fidelidad se calculan automáticamente (1 punto por cada 10,000 COP en compras). Los puntos se pueden canjear como descuento (1 punto = 100 COP).	Alta	Un cliente puede existir sin ventas asociadas. No se elimina un cliente con ventas o apartados activos.	Administrador, Vendedor

1.3 Módulo de Proveedores

ID	Nombre	Descripción	Reglas de Negocio	Prioridad	Restricciones	Rol(es)
RF09	Listar Proveedores	Vista centralizada con tarjetas detalladas, búsqueda por nombre/material y contadores (total/activos).	Paginación server-side. Acceso directo a Compras y Edición desde cada tarjeta. Solo Administrador.	Alta	Solo Administrador.	Administrador
RF10	Registrar Proveedor	Crear proveedor con razón social, documento, fecha inicio, monto mínimo compra, múltiples teléfonos/correos y materiales suministrados.	Documento/NIT único. Al menos un teléfono y un correo requeridos. Al menos un material seleccionado. Fecha de inicio no puede ser futura.	Alta	Solo Administrador.	Administrador
RF11-E	Editar Proveedor	Modificar monto mínimo, datos de contacto, materiales y estado (Activo/Inactivo).	Documento e identificación interna inmutables. Al inactivar, se advierte que no se podrán registrar nuevas compras. Productos activos del proveedor permanecen en el catálogo.	Alta	Solo Administrador puede editar.	Administrador
RF-P01	Evaluación de Proveedores	Registrar evaluaciones periódicas: calidad, tiempo de entrega, precio. Generar indicador de desempeño.	Escala 1-5 por dimensión. Promedio ponderado almacenado. Historial de evaluaciones por proveedor.	Baja	Solo Administrador. Evaluación no bloquea operaciones.	Administrador

1.4 Módulo de Compras

ID	Nombre	Descripción	Reglas de Negocio	Prioridad	Restricciones	Rol(es)
RF11	Gestionar Órdenes de Compra	Crear, visualizar y descargar órdenes de compra por proveedor. Resumen histórico del proveedor.	Una vez guardada la compra no se puede modificar (integridad contable). El sistema calcula subtotales y total automáticamente. Al guardar, incrementa stock automáticamente. Genera PDF del comprobante.	Alta	Solo Administrador. Proveedor debe estar Activo.	Administrador
RF-C01	Compra a Crédito	Al registrar compra con método 'Crédito', se crea registro en Credito_Compra con saldo pendiente = total.	Una compra solo puede tener un crédito activo simultáneo. Estado cambia a 'pagado' cuando saldo llega a cero. Cambia a 'vencido' si supera fecha de vencimiento sin pago completo.	Alta	Solo Administrador.	Administrador
RF-C02	Abonos a Créditos de Compra	Registrar abonos parciales o totales a créditos activos indicando monto y método de pago.	Monto del abono ≤ saldo pendiente. Solo créditos 'activos' admiten abonos. Cada abono registrado en Abono_Credito_Compra con fecha y método.	Alta	Solo Administrador.	Administrador
RF-C03	Recepción Parcial de Compra	Registrar la recepción parcial de productos de una orden de compra. El stock se incrementa por la cantidad recibida.	Una orden puede tener múltiples recepciones hasta completar la cantidad pedida. El estado de la orden pasa a 'recibido_parcial' o 'recibido_completo'.	Media	Solo Administrador.	Administrador

1.5 Módulo de Catálogo

ID	Nombre	Descripción	Reglas de Negocio	Prioridad	Restricciones	Rol(es)
RF12	Gestionar Categorías	CRUD de categorías de productos (Anillos, Collares, etc.) con ícono.	No se puede desactivar categoría con productos activos. Nombre único. Al eliminar, verificar integridad referencial.	Media	Solo Administrador.	Administrador
RF13	Gestionar Subcategorías	CRUD de subcategorías asociadas a categorías (15 años, Matrimonio, Uso Diario, etc.).	No se puede desactivar subcategoría con productos activos. Nombre único dentro de la categoría.	Media	Solo Administrador.	Administrador
RF14	Gestionar Materiales	CRUD de materiales (Plata, Oro, Acero, Covergold, etc.).	No se puede desactivar material con productos activos. Nombre único.	Media	Solo Administrador.	Administrador
RF-CAT01	Gestionar Productos	CRUD completo de productos con: código SKU, nombre, categoría, subcategorías (múltiples), material, proveedor, precio costo, precio venta, stock inicial, descripción, imagen, talla/medida, número de serie/lote (opcional).	Precio venta > precio costo. Código SKU único. Stock no puede ser negativo. Imagen: JPG/PNG ≤ 5 MB. Precio venta mínimo = precio costo × 2 (configurable). Número de lote/serie requerido para metales preciosos.	Alta	Solo Administrador puede crear/editar productos.	Administrador
RF-CAT02	Trazabilidad de Metales y Piedras	Para productos de metales preciosos u otros materiales rastreables, registrar: ley (925, 750, 585), quilates (para oro), certificación de procedencia, piedras preciosas con tipo/quilates/certificado.	Aplica solo a categorías configuradas como 'trazables'. Los datos de trazabilidad forman parte del certificado de autenticidad generado.	Media	Solo Administrador.	Administrador
RF-CAT03	Certificado de Autenticidad	Generar y descargar certificado PDF por producto que incluya: código, descripción, material, ley, piedras, número de serie/lote, fecha de emisión y sello del negocio.	El certificado es inmutable una vez emitido. Se almacena referencia al PDF generado. Puede vincularse a una venta específica.	Media	Administrador genera; Vendedor puede adjuntar a venta.	Administrador, Vendedor
RF-CAT04	Gestionar Tallas y Medidas	Catálogo de tallas/medidas disponibles por categoría (p. ej. tallas de anillo en mm o estándar US/EU).	Talla asociada a categoría específica. Un producto puede tener múltiples variantes de talla con stock independiente.	Media	Solo Administrador configura el catálogo de tallas.	Administrador
RF-CAT05	Reserva Temporal de Producto	Reservar un producto (o variante) para un cliente durante un período definido (máx. 48h), bloqueando el stock.	El stock reservado no se descuenta hasta confirmar venta/apartado. Expiración automática libera el stock. Una misma unidad no puede reservarse simultáneamente a dos clientes.	Media	Administrador y Vendedor pueden crear reservas.	Administrador, Vendedor

1.6 Módulo de Inventario

ID	Nombre	Descripción	Reglas de Negocio	Prioridad	Restricciones	Rol(es)
RF15	Control de Inventario	Actualización automática de stock ante compras, ventas, devoluciones y ajustes. Stock nunca negativo.	Concurrencia manejada con transacciones a nivel de BD (SELECT FOR UPDATE o optimistic locking). Todo movimiento registrado en Inventario_Movimiento.	Alta	Proceso automático del sistema.	Sistema
RF16	Historial de Movimientos	Consulta del historial completo filtrable por producto, tipo de movimiento y rango de fechas.	Solo lectura. Referencia legible al documento origen (venta #X, compra #Y, ajuste #Z). Solo Administrador ve el historial completo.	Alta	Solo Administrador.	Administrador
RF-INV01	Alertas de Stock Bajo	Notificaciones en dashboard y correo cuando el stock de un producto cae por debajo del umbral configurado por producto.	Umbral configurable por producto (default: 5 unidades). La alerta se genera una sola vez por evento de descenso. Se limpia al superar el umbral nuevamente.	Media	Administrador recibe alertas.	Administrador
RF-INV02	Ajuste Manual de Inventario	El Administrador puede registrar ajustes manuales de stock (por merma, pérdida, error de conteo) con motivo obligatorio.	Cada ajuste registrado en Inventario_Movimiento con tipo 'ajuste' y referencia al usuario. Requiere motivo de mín. 20 caracteres.	Media	Solo Administrador.	Administrador
RF-INV03	Conteo Físico / Inventario Cíclico	Registrar conteos físicos periódicos y generar diferencias vs. stock del sistema, creando ajustes automáticos.	El conteo queda en estado 'borrador' hasta confirmarse. Al confirmar, genera movimientos de ajuste. El historial de conteos queda almacenado.	Baja	Solo Administrador.	Administrador

1.7 Módulo de Ventas

ID	Nombre	Descripción	Reglas de Negocio	Prioridad	Restricciones	Rol(es)
RF17	Listar Ventas	Vista centralizada de ventas. Admin ve todas; Vendedor solo las propias. Filtros por fecha, estado, vendedor, cliente.	Paginación server-side. Contadores de ventas totales y con saldo pendiente.	Alta	Admin: todas. Vendedor: propias.	Administrador, Vendedor
RF18	Consultar Detalle de Venta	Ver información completa: vendedor, cliente, productos, totales, modalidad de pago, estado.	Datos inmutables tras registro. Incluye link a factura PDF.	Alta	Admin ve cualquier venta. Vendedor solo las suyas.	Administrador, Vendedor
RF19	Descargar Factura PDF	Generar PDF con datos completos de la venta: cliente, vendedor, productos, totales, número de factura, QR de verificación.	PDF solo lectura. El número de factura es consecutivo y único.	Alta	Admin descarga cualquiera. Vendedor solo las suyas.	Administrador, Vendedor
RF20	Registrar Venta	Crear venta con: datos del cliente (existente o nuevo), productos/cantidades, modalidad de pago (contado, anticipo, crédito), descuentos aplicables.	Stock se descuenta automáticamente al confirmar. Total = suma de subtotales. Se aplican puntos de fidelidad si el cliente los tiene. Descuentos por promoción/lealtad se registran en detalle.	Alta	Vendedor crea ventas. Admin también puede.	Administrador, Vendedor
RF21	Seguimiento de Anticipos	Registrar y hacer seguimiento a ventas con pago parcial anticipado; mostrar saldo pendiente.	El anticipo < total de la venta (si es igual, se registra como contado). El saldo pendiente debe pagarse antes de marcar la venta como completamente pagada. Se genera comprobante por cada pago.	Alta	Administrador y Vendedor.	Administrador, Vendedor
RF-V01	Apartado / Layaway	Registrar apartados: reserva de producto con plan de abonos periódicos. La factura final se emite al completar el pago.	El apartado bloquea el stock del producto. Se registra monto total, monto mínimo de primer abono (≥20%), plan de pagos y fecha límite. Al completar el pago, se genera la factura definitiva. Si el cliente cancela, el reembolso se rige por política de la tienda.	Alta	Administrador y Vendedor crean apartados.	Administrador, Vendedor
RF-V02	Descuentos y Promociones	Aplicar descuentos por porcentaje o monto fijo a ventas o ítems específicos. Gestionar promociones con vigencia.	El descuento no puede resultar en precio negativo. Las promociones tienen fecha de inicio y fin. El sistema calcula automáticamente el precio con descuento. Se registra el descuento aplicado en Detalle_Venta.	Media	Administrador crea/edita promociones. Vendedor aplica descuentos dentro de los límites configurados.	Administrador, Vendedor
RF-V03	Programa de Fidelidad	Acumular y canjear puntos de fidelidad en ventas.	1 punto por cada 10,000 COP. Al canjear, se aplica descuento adicional (1 punto = 100 COP). El canje se registra en el detalle de la venta.	Media	Vendedor aplica; Admin configura tasa.	Administrador, Vendedor
RF-V04	Garantías de Producto	Registrar garantía asociada a ítems de una venta: duración, cobertura, fecha de expiración.	La garantía se activa en la fecha de venta. Se emite comprobante de garantía en PDF junto con la factura. El cliente puede consultar la garantía por número de factura.	Media	Administrador y Vendedor.	Administrador, Vendedor

1.8 Módulo de Postventa

ID	Nombre	Descripción	Reglas de Negocio	Prioridad	Restricciones	Rol(es)
RF24	Listar Casos Postventa	Vista de todos los casos clasificados por tipo (reclamo, devolución, cambio, reparación), origen, estado y fecha.	Búsqueda por número de factura o cliente. Solo Admin ve todos; Vendedor solo los de sus ventas.	Media	Admin todos; Vendedor sus casos.	Administrador, Vendedor
RF25	Gestionar Casos Postventa	Admin registra, actualiza y cierra casos. Devoluciones generan movimiento de inventario 'entrada'. Cambios actualizan stock del producto devuelto y descontado.	Todo caso vinculado a venta o compra existente. Devoluciones no pueden exceder cantidad original. Cada cambio de estado registrado en historial.	Media	Solo Administrador gestiona.	Administrador
RF26	Consulta Vendedor Postventa	Vendedor consulta casos de sus ventas usando número de factura. Solo lectura.	Búsqueda requiere número de factura. Vendedor ve estado actual del caso pero no puede modificarlo.	Media	Solo Vendedor (para sus ventas).	Vendedor
RF-PV01	Gestión de Reparaciones y Ajustes	Registrar solicitudes de reparación/ajuste de joyas: descripción del trabajo, presupuesto, tiempo estimado, estado (recibido, en proceso, listo, entregado).	La pieza recibida se registra con descripción detallada y fotos. El presupuesto aprobado por el cliente antes de iniciar. Al entregar, se genera comprobante de servicio. La pieza no modifica el inventario del catálogo.	Alta	Administrador gestiona. Vendedor puede registrar ingreso de pieza.	Administrador, Vendedor
RF-PV02	Notificaciones de Estado de Caso	Enviar notificación por correo al cliente cuando el estado de su caso postventa o reparación cambie.	El correo del cliente debe estar registrado. La notificación incluye descripción del cambio y próximos pasos. Log de notificaciones enviadas.	Baja	Sistema automático; Administrador puede reenviar manualmente.	Sistema, Administrador

1.9 Módulo de Pagos y Créditos

ID	Nombre	Descripción	Reglas de Negocio	Prioridad	Restricciones	Rol(es)
RF28	Gestionar Métodos de Pago	CRUD de métodos de pago (efectivo, transferencia, tarjeta, etc.). Solo se puede desactivar, no eliminar si tiene transacciones.	Nombres únicos. Métodos activos disponibles en ventas y abonos. No se puede eliminar con transacciones asociadas.	Media	Solo Administrador.	Administrador
RF29-A	Gestionar Créditos de Compra	Registrar y gestionar compras a crédito con saldo pendiente, fechas inicio/vencimiento y estado.	Una compra, un crédito activo máximo. Saldo 0 ≤ saldo ≤ total. Estado: activo/pagado/vencido. Transición automática al llegar a saldo cero o superar vencimiento.	Alta	Solo Administrador.	Administrador
RF29-B	Registrar Abonos a Créditos	Registrar abonos parciales o totales. El sistema descuenta automáticamente del saldo.	Abono ≤ saldo pendiente. Solo a créditos 'activos'. Registro en Abono_Credito_Compra con fecha, monto, método de pago.	Alta	Solo Administrador.	Administrador
RF-PAG01	Conciliación de Caja	Registro del cierre de caja diario con totales por método de pago, comparados contra ventas del día.	El cierre de caja no modifica ventas. Solo registra lo que el operador reporta. Diferencias quedan anotadas para auditoría.	Media	Solo Administrador.	Administrador
RF-PAG02	Historial de Pagos por Venta	Consultar todos los pagos asociados a una venta (anticipo, abono, pago final) con fecha y método.	Solo lectura. Admin ve todos; Vendedor solo sus ventas.	Media	Admin y Vendedor (limitado).	Administrador, Vendedor

1.10 Módulo de Reportes

ID	Nombre	Descripción	Reglas de Negocio	Prioridad	Restricciones	Rol(es)
RF30	Generar y Exportar Reportes	Generar reportes de ventas por período, inventario actual, movimientos, desempeño de proveedores y vendedores. Exportar en PDF o Excel.	Solo lectura. Filtros por fecha y categoría. Los reportes son instantáneos (no agendados en v1). Exportación asíncrona para reportes grandes.	Media	Solo Administrador.	Administrador
RF-R01	Dashboard con KPIs	Panel de indicadores en tiempo real: ventas del día/mes, ingresos, productos más vendidos, clientes frecuentes, alertas de stock.	KPIs calculados sobre datos en vivo (caché de 5 minutos aceptable). El dashboard del Administrador incluye todos los módulos; el del Vendedor muestra solo sus métricas.	Alta	Admin: KPIs globales. Vendedor: KPIs propios.	Administrador, Vendedor
RF-R02	Reporte de Rentabilidad	Calcular margen bruto por producto, categoría y período: (precio_venta - precio_costo) / precio_venta × 100.	Usa precios históricos del Detalle_Venta. No usa precios actuales del catálogo. Solo Administrador.	Media	Solo Administrador.	Administrador
RF-R03	Reporte de Fidelidad	Reporte de clientes con puntos acumulados, puntos canjeados e historial de transacciones de puntos.	Solo Administrador.	Baja	Solo Administrador.	Administrador

1.11 Módulo de Búsqueda

ID	Nombre	Descripción	Reglas de Negocio	Prioridad	Restricciones	Rol(es)
RF31	Buscador Avanzado Global	Búsqueda en tiempo real (debounce 300ms) en múltiples módulos: productos, ventas, usuarios, clientes, proveedores.	Los resultados se filtran según permisos del usuario. El Vendedor no puede buscar ventas de otros vendedores. Resultados paginados.	Media	Admin: acceso completo. Vendedor: restringido.	Administrador, Vendedor
RF-B01	Búsqueda de Clientes	Búsqueda de clientes por nombre, documento o teléfono desde el formulario de venta para selección rápida.	Búsqueda dinámica con autocompletado. Muestra historial de compras resumido del cliente seleccionado.	Alta	Administrador y Vendedor.	Administrador, Vendedor
RF-B02	Búsqueda de Productos en Venta	Búsqueda de productos por nombre, código SKU, categoría o material desde el formulario de registro de venta.	Solo muestra productos activos con stock > 0. Muestra precio de venta y stock disponible.	Alta	Administrador y Vendedor.	Administrador, Vendedor

 
2. Requisitos No Funcionales — Spring Boot + React

2.1 Seguridad
RNF-SEG01	Autenticación JWT stateless. Tokens de acceso con exp 8h, refresh tokens con exp 7d almacenados en httpOnly cookie o Redis.
RNF-SEG02	Control de acceso basado en roles (RBAC) implementado con Spring Security. Anotaciones @PreAuthorize en cada endpoint.
RNF-SEG03	Contraseñas almacenadas con BCrypt (cost factor ≥ 12). Nunca almacenadas en texto plano ni en logs.
RNF-SEG04	Protección CSRF para operaciones de estado (POST/PUT/DELETE). Headers CORS configurados explícitamente por entorno.
RNF-SEG05	Protección XSS: sanitización de entradas en backend (validación Bean Validation) y CSP headers en frontend.
RNF-SEG06	Datos sensibles en tránsito protegidos con TLS 1.2+. Variables de entorno para credenciales; no hardcodeadas.
RNF-SEG07	Rate limiting en endpoints de autenticación: máximo 10 intentos fallidos por IP en 15 minutos antes de bloquear temporalmente.
RNF-SEG08	Tokens de recuperación de contraseña de un solo uso, expiración 15 minutos, almacenados con hash en BD.

2.2 Rendimiento y Escalabilidad
RNF-REN01	Tiempo de respuesta de API < 300ms para el percentil 95 bajo carga normal (hasta 50 usuarios concurrentes).
RNF-REN02	Caché de segundo nivel en Spring Boot con Caffeine o Redis para catálogos, categorías y materiales (TTL 10 minutos).
RNF-REN03	Paginación obligatoria en todos los endpoints de listado (page, size, sort). Tamaño máximo de página: 100 registros.
RNF-REN04	Manejo de concurrencia en stock: uso de transacciones con nivel de aislamiento REPEATABLE_READ y optimistic locking (versioned entities @Version en JPA).
RNF-REN05	Operaciones de generación de PDF procesadas de forma asíncrona (@Async). Para reportes pesados, uso de jobs en segundo plano.
RNF-REN06	Índices de base de datos en columnas de búsqueda frecuente: usuario.correo, producto.codigo, venta.fecha_emision, cliente.documento.
RNF-REN07	Frontend React con lazy loading de rutas, code splitting por módulo. Imágenes servidas con compresión WebP y caché de CDN/browser.

2.3 Mantenibilidad y Calidad
RNF-MAN01	Cobertura de pruebas unitarias ≥ 80% en capa de servicios (JUnit 5 + Mockito). Pruebas de integración para endpoints críticos (TestContainers).
RNF-MAN02	Logging estructurado en JSON con Logback (producción) y consola (desarrollo). Niveles: ERROR para excepciones, INFO para operaciones clave, DEBUG desactivado en prod.
RNF-MAN03	Documentación de API REST con OpenAPI 3.0 / Swagger UI. Cada endpoint documentado con descripción, parámetros, respuestas y ejemplos.
RNF-MAN04	Validación de entrada con Bean Validation (@NotNull, @Size, @Pattern, etc.) en DTOs. Mensajes de error en español, claros y sin stacktrace al cliente.
RNF-MAN05	Manejo de excepciones centralizado con @ControllerAdvice y respuestas estandarizadas (ApiResponse<T>).
RNF-MAN06	Versionado de esquema de BD con Flyway (migraciones en SQL versionadas).
RNF-MAN07	Monitoreo con Spring Boot Actuator + Micrometer. Métricas expuestas para Prometheus/Grafana en producción.

2.4 Usabilidad y UX
RNF-UX01	Diseño responsive (mobile-first). Funcional en pantallas desde 360px de ancho. Interfaz probada en Chrome, Firefox, Edge y Safari.
RNF-UX02	Accesibilidad básica WCAG 2.1 nivel AA: contraste de color ≥ 4.5:1, navegación por teclado, etiquetas ARIA en formularios.
RNF-UX03	Feedback inmediato al usuario: indicadores de carga (spinners), mensajes de éxito/error toast, validaciones en tiempo real en formularios.
RNF-UX04	Manejo de estados asíncronos en React: estados loading/error/success para cada petición. Componentes Skeleton para datos en carga.
RNF-UX05	Operaciones realizables en ≤ 3 pasos desde cualquier módulo con confirmación visual inmediata.
RNF-UX06	Internacionalización básica: todos los textos de la UI en español colombiano. Fechas en formato dd/mm/yyyy. Moneda en COP con separador de miles.

2.5 Infraestructura y Despliegue
RNF-INF01	Aplicación empaquetada en contenedores Docker. docker-compose.yml para entorno de desarrollo local con servicios: api (Spring Boot), web (React/Nginx), db (MySQL 8), cache (Redis).
RNF-INF02	Variables de entorno para toda configuración sensible: credenciales BD, secreto JWT, configuración SMTP, rutas de almacenamiento. No hardcodeadas en el código.
RNF-INF03	Pipeline CI/CD (GitHub Actions o GitLab CI): build, pruebas unitarias, análisis de calidad (SonarQube), build de imagen Docker, despliegue a staging automático.
RNF-INF04	Backups automáticos diarios de la base de datos MySQL. Retención de 30 días. Verificación de restauración mensual documentada.
RNF-INF05	Frontend servido por Nginx con compresión Gzip/Brotli, caché de assets estáticos (hash en nombre de archivo), SPA fallback a index.html.
RNF-INF06	Tiempo de recuperación ante fallo (RTO) < 4 horas. Tiempo de pérdida máximo de datos (RPO) < 24 horas (backup diario).

 
3. Modelo de Datos Mejorado — Diccionario de Datos
Todas las tablas usan el motor InnoDB de MySQL 8.0+. Los IDs son BIGINT UNSIGNED AUTO_INCREMENT salvo indicación. Las fechas de auditoría (created_at, updated_at) son DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP con ON UPDATE CURRENT_TIMESTAMP.

3.1 Tabla: usuario
Campo	Tipo	Nulidad	Clave	Descripción
usuario_id	BIGINT UNSIGNED	NOT NULL	PK	Identificador único autoincremental.
nombre	VARCHAR(150)	NOT NULL		Nombre completo del usuario.
numero_documento	VARCHAR(20)	NOT NULL	UQ	Cédula/NIT del trabajador. Inmutable tras creación.
correo	VARCHAR(255)	NOT NULL	UQ	Correo electrónico. Inmutable tras creación.
telefono	VARCHAR(20)	NULL		Teléfono de contacto.
password_hash	VARCHAR(255)	NOT NULL		Contraseña cifrada con BCrypt (cost≥12).
es_password_temporal	TINYINT(1)	NOT NULL DEFAULT 1		1: contraseña provisional; 0: personalizada.
estado	ENUM('activo','inactivo')	NOT NULL DEFAULT 'activo'		Estado de la cuenta.
rol_id	BIGINT UNSIGNED	NOT NULL	FK → rol	Rol asignado.
ultimo_acceso	DATETIME	NULL		Timestamp del último inicio de sesión exitoso.
foto_perfil_url	VARCHAR(500)	NULL		URL relativa o absoluta de la foto de perfil.
created_at	DATETIME	NOT NULL		Timestamp de creación.
updated_at	DATETIME	NOT NULL		Timestamp de última modificación.

3.2 Tabla: rol
Campo	Tipo	Nulidad	Clave	Descripción
rol_id	BIGINT UNSIGNED	NOT NULL	PK	Identificador único.
nombre	ENUM('superadministrador','administrador','vendedor')	NOT NULL	UQ	Nombre del rol. Inmutable.
descripcion	VARCHAR(255)	NULL		Descripción del rol.

3.3 Tabla: cliente
Campo	Tipo	Nulidad	Clave	Descripción
cliente_id	BIGINT UNSIGNED	NOT NULL	PK	Identificador único.
nombre	VARCHAR(150)	NOT NULL		Nombre completo del cliente.
numero_documento	VARCHAR(20)	NULL	UQ	Documento de identidad (opcional pero único si se ingresa).
telefono	VARCHAR(20)	NULL		Teléfono principal.
correo	VARCHAR(255)	NULL	UQ	Correo electrónico (único si se ingresa).
direccion	VARCHAR(500)	NULL		Dirección de entrega/referencia.
fecha_nacimiento	DATE	NULL		Fecha de nacimiento para campañas de fidelidad.
puntos_fidelidad	INT UNSIGNED	NOT NULL DEFAULT 0		Puntos de fidelidad acumulados.
notas	TEXT	NULL		Preferencias o notas del cliente.
created_at	DATETIME	NOT NULL		Timestamp de creación.
updated_at	DATETIME	NOT NULL		Timestamp de última modificación.

3.4 Tabla: categoria
Campo	Tipo	Nulidad	Clave	Descripción
categoria_id	BIGINT UNSIGNED	NOT NULL	PK	Identificador único.
nombre	VARCHAR(100)	NOT NULL	UQ	Nombre de la categoría.
icono_url	VARCHAR(500)	NULL		URL del ícono de la categoría.
estado	ENUM('activo','inactivo')	NOT NULL DEFAULT 'activo'		Estado. No se desactiva si tiene productos activos.
created_at	DATETIME	NOT NULL		

3.5 Tabla: subcategoria
Campo	Tipo	Nulidad	Clave	Descripción
subcategoria_id	BIGINT UNSIGNED	NOT NULL	PK	Identificador único.
nombre	VARCHAR(100)	NOT NULL	UQ	Nombre de la subcategoría.
estado	ENUM('activo','inactivo')	NOT NULL DEFAULT 'activo'		Estado.
created_at	DATETIME	NOT NULL		

3.6 Tabla: material
Campo	Tipo	Nulidad	Clave	Descripción
material_id	BIGINT UNSIGNED	NOT NULL	PK	Identificador único.
nombre	VARCHAR(100)	NOT NULL	UQ	Nombre del material (Plata Ley 950, Covergold, etc.).
es_trazable	TINYINT(1)	NOT NULL DEFAULT 0		1 si requiere datos de trazabilidad (ley, quilates).
estado	ENUM('activo','inactivo')	NOT NULL DEFAULT 'activo'		Estado.

3.7 Tabla: producto
Campo	Tipo	Nulidad	Clave	Descripción
producto_id	BIGINT UNSIGNED	NOT NULL	PK	Identificador único.
codigo	VARCHAR(20)	NOT NULL	UQ	Código SKU único generado por el sistema.
nombre	VARCHAR(255)	NOT NULL		Nombre comercial del producto.
descripcion	TEXT	NULL		Descripción del producto (mín. 10 chars, máx. 500 chars).
categoria_id	BIGINT UNSIGNED	NOT NULL	FK → categoria	Categoría principal.
material_id	BIGINT UNSIGNED	NOT NULL	FK → material	Material principal.
proveedor_id	BIGINT UNSIGNED	NULL	FK → proveedor	Proveedor asociado.
precio_costo	DECIMAL(14,2)	NOT NULL	CHECK ≥ 0	Precio de costo o adquisición.
precio_venta	DECIMAL(14,2)	NOT NULL	CHECK > precio_costo	Precio de venta al público.
stock	INT	NOT NULL DEFAULT 0	CHECK ≥ 0	Cantidad disponible en inventario.
stock_minimo	INT	NOT NULL DEFAULT 5	CHECK ≥ 0	Umbral de alerta de stock bajo.
imagen_url	VARCHAR(500)	NULL		URL de la imagen principal.
numero_serie	VARCHAR(100)	NULL		Número de serie (productos únicos de alto valor).
numero_lote	VARCHAR(100)	NULL		Número de lote del proveedor.
estado	ENUM('activo','inactivo')	NOT NULL DEFAULT 'activo'		Estado del producto en catálogo.
created_at	DATETIME	NOT NULL		Timestamp de creación.
updated_at	DATETIME	NOT NULL		Timestamp de última modificación.

3.8 Tabla: producto_trazabilidad
Aplica a productos con material.es_trazable = 1.
Campo	Tipo	Nulidad	Clave	Descripción
trazabilidad_id	BIGINT UNSIGNED	NOT NULL	PK	
producto_id	BIGINT UNSIGNED	NOT NULL	FK → producto, UQ	Un producto, un registro de trazabilidad.
ley	VARCHAR(20)	NULL		Ley del metal: '925', '750', '585', etc.
quilates	DECIMAL(5,2)	NULL		Quilates para oro (18K, 14K, 10K, etc.).
certificado_procedencia	VARCHAR(255)	NULL		Número de certificado de procedencia.
piedras_descripcion	TEXT	NULL		Descripción de piedras (tipo, quilates, certificado).
created_at	DATETIME	NOT NULL		

3.9 Tabla: producto_subcategoria (relación N:M)
Campo	Tipo	Nulidad	Clave	Descripción
producto_id	BIGINT UNSIGNED	NOT NULL	FK → producto, PK compuesto	
subcategoria_id	BIGINT UNSIGNED	NOT NULL	FK → subcategoria, PK compuesto	

3.10 Tabla: proveedor
Campo	Tipo	Nulidad	Clave	Descripción
proveedor_id	BIGINT UNSIGNED	NOT NULL	PK	Identificador único.
nombre	VARCHAR(255)	NOT NULL		Razón social o nombre.
documento	VARCHAR(50)	NOT NULL	UQ	NIT o cédula. Inmutable.
fecha_inicio	DATE	NOT NULL	CHECK ≤ CURDATE()	Fecha de inicio de relación comercial.
minimo_compra	DECIMAL(14,2)	NOT NULL DEFAULT 0	CHECK ≥ 0	Monto mínimo de orden.
estado	ENUM('activo','inactivo')	NOT NULL DEFAULT 'activo'		Estado del proveedor.
created_at	DATETIME	NOT NULL		
updated_at	DATETIME	NOT NULL		

3.11 Tabla: proveedor_telefono
Campo	Tipo	Nulidad	Clave	Descripción
telefono_id	BIGINT UNSIGNED	NOT NULL	PK	
proveedor_id	BIGINT UNSIGNED	NOT NULL	FK → proveedor	
telefono	VARCHAR(20)	NOT NULL		Número de teléfono.
es_principal	TINYINT(1)	NOT NULL DEFAULT 0		1 si es el contacto principal.

3.12 Tabla: proveedor_correo
Campo	Tipo	Nulidad	Clave	Descripción
correo_id	BIGINT UNSIGNED	NOT NULL	PK	
proveedor_id	BIGINT UNSIGNED	NOT NULL	FK → proveedor	
correo	VARCHAR(255)	NOT NULL	UQ	Correo del proveedor. Único globalmente.
es_principal	TINYINT(1)	NOT NULL DEFAULT 0		

3.13 Tabla: proveedor_material (relación N:M)
Campo	Tipo	Nulidad	Clave	Descripción
proveedor_id	BIGINT UNSIGNED	NOT NULL	FK → proveedor, PK compuesto	
material_id	BIGINT UNSIGNED	NOT NULL	FK → material, PK compuesto	

3.14 Tabla: compra
Campo	Tipo	Nulidad	Clave	Descripción
compra_id	BIGINT UNSIGNED	NOT NULL	PK	Identificador único.
proveedor_id	BIGINT UNSIGNED	NOT NULL	FK → proveedor	Proveedor de la compra.
usuario_id	BIGINT UNSIGNED	NOT NULL	FK → usuario	Administrador que registró la compra.
fecha_factura	DATE	NOT NULL	CHECK ≤ CURDATE()	Fecha de la factura del proveedor.
fecha_entrega_esperada	DATE	NULL	CHECK ≥ fecha_factura	Fecha de entrega pactada.
fecha_recepcion_real	DATE	NULL		Fecha efectiva de recepción total.
metodo_pago_id	BIGINT UNSIGNED	NOT NULL	FK → metodo_pago	Método de pago.
tipo_pago	ENUM('contado','credito')	NOT NULL		Modalidad de pago.
subtotal	DECIMAL(14,2)	NOT NULL DEFAULT 0	CHECK ≥ 0	Suma de subtotales de ítems.
total	DECIMAL(14,2)	NOT NULL DEFAULT 0	CHECK ≥ 0	Total de la orden de compra.
estado	ENUM('pendiente','recibido_parcial','recibido_completo','cancelada')	NOT NULL DEFAULT 'pendiente'		Estado de la orden.
notas	TEXT	NULL		Observaciones de la compra.
created_at	DATETIME	NOT NULL		

3.15 Tabla: detalle_compra
Campo	Tipo	Nulidad	Clave	Descripción
detalle_compra_id	BIGINT UNSIGNED	NOT NULL	PK	
compra_id	BIGINT UNSIGNED	NOT NULL	FK → compra	
producto_id	BIGINT UNSIGNED	NOT NULL	FK → producto	
cantidad_pedida	INT	NOT NULL	CHECK > 0	Cantidad solicitada.
cantidad_recibida	INT	NOT NULL DEFAULT 0	CHECK ≥ 0	Cantidad efectivamente recibida.
precio_unitario	DECIMAL(14,2)	NOT NULL	CHECK ≥ 0	Precio de costo por unidad.
subtotal	DECIMAL(14,2)	NOT NULL		cantidad_pedida × precio_unitario.

3.16 Tabla: venta
Campo	Tipo	Nulidad	Clave	Descripción
venta_id	BIGINT UNSIGNED	NOT NULL	PK	Identificador único.
numero_factura	VARCHAR(20)	NOT NULL	UQ	Número consecutivo de factura generado automáticamente.
usuario_id	BIGINT UNSIGNED	NOT NULL	FK → usuario	Vendedor que realizó la venta.
cliente_id	BIGINT UNSIGNED	NOT NULL	FK → cliente	Cliente.
fecha_emision	DATETIME	NOT NULL DEFAULT NOW()		Timestamp de la transacción.
subtotal	DECIMAL(14,2)	NOT NULL DEFAULT 0	CHECK ≥ 0	Suma de subtotales.
descuento_total	DECIMAL(14,2)	NOT NULL DEFAULT 0	CHECK ≥ 0	Descuento total aplicado.
total	DECIMAL(14,2)	NOT NULL DEFAULT 0	CHECK ≥ 0	Total a pagar.
modalidad_pago	ENUM('contado','anticipo','credito','layaway')	NOT NULL		Modalidad de pago.
estado_pago	ENUM('pagado','pendiente','vencido')	NOT NULL DEFAULT 'pendiente'		Estado del pago.
estado_venta	ENUM('activa','cancelada','devuelta')	NOT NULL DEFAULT 'activa'		Estado de la venta.
notas	TEXT	NULL		Observaciones.
created_at	DATETIME	NOT NULL		

3.17 Tabla: detalle_venta
Campo	Tipo	Nulidad	Clave	Descripción
detalle_venta_id	BIGINT UNSIGNED	NOT NULL	PK	
venta_id	BIGINT UNSIGNED	NOT NULL	FK → venta	
producto_id	BIGINT UNSIGNED	NOT NULL	FK → producto	
cantidad	INT	NOT NULL	CHECK > 0	Cantidad vendida.
precio_unitario	DECIMAL(14,2)	NOT NULL	CHECK ≥ 0	Precio al momento de la venta (histórico).
descuento_item	DECIMAL(14,2)	NOT NULL DEFAULT 0	CHECK ≥ 0	Descuento aplicado al ítem.
subtotal	DECIMAL(14,2)	NOT NULL		(precio_unitario - descuento_item) × cantidad.
garantia_duracion_meses	INT	NULL	CHECK ≥ 0	Meses de garantía del ítem (0 = sin garantía).
garantia_vencimiento	DATE	NULL		Fecha de vencimiento de la garantía del ítem.
certificado_url	VARCHAR(500)	NULL		URL del certificado de autenticidad vinculado.

3.18 Tabla: pago_venta
Campo	Tipo	Nulidad	Clave	Descripción
pago_id	BIGINT UNSIGNED	NOT NULL	PK	
venta_id	BIGINT UNSIGNED	NOT NULL	FK → venta	
metodo_pago_id	BIGINT UNSIGNED	NOT NULL	FK → metodo_pago	
monto	DECIMAL(14,2)	NOT NULL	CHECK > 0	Monto del pago.
tipo_pago	ENUM('anticipo','abono','pago_completo','canje_puntos')	NOT NULL		Tipo de pago.
estado	ENUM('confirmado','pendiente','rechazado')	NOT NULL DEFAULT 'confirmado'		Estado del pago.
referencia_transaccion	VARCHAR(100)	NULL		Referencia de transferencia o número de comprobante.
fecha_pago	DATETIME	NOT NULL DEFAULT NOW()		Timestamp del pago.

3.19 Tabla: apartado (Layaway)
Campo	Tipo	Nulidad	Clave	Descripción
apartado_id	BIGINT UNSIGNED	NOT NULL	PK	
cliente_id	BIGINT UNSIGNED	NOT NULL	FK → cliente	
usuario_id	BIGINT UNSIGNED	NOT NULL	FK → usuario	Vendedor que registró el apartado.
producto_id	BIGINT UNSIGNED	NOT NULL	FK → producto	Producto reservado.
cantidad	INT	NOT NULL DEFAULT 1	CHECK > 0	
precio_acordado	DECIMAL(14,2)	NOT NULL	CHECK > 0	Precio pactado al momento del apartado.
monto_primer_abono	DECIMAL(14,2)	NOT NULL	CHECK ≥ precio_acordado*0.20	Mínimo 20% del precio acordado.
saldo_pendiente	DECIMAL(14,2)	NOT NULL		Calculado automáticamente.
fecha_limite	DATE	NOT NULL	CHECK > CURDATE()	Fecha máxima para completar el pago.
estado	ENUM('activo','completado','cancelado','vencido')	NOT NULL DEFAULT 'activo'		Estado del apartado.
venta_id	BIGINT UNSIGNED	NULL	FK → venta	FK a la venta generada al completar el pago.
created_at	DATETIME	NOT NULL		

3.20 Tabla: caso_postventa
Campo	Tipo	Nulidad	Clave	Descripción
caso_id	BIGINT UNSIGNED	NOT NULL	PK	
venta_id	BIGINT UNSIGNED	NULL	FK → venta	Venta origen (NULL si viene de compra).
compra_id	BIGINT UNSIGNED	NULL	FK → compra	Compra origen (NULL si viene de venta). CHECK: venta_id IS NOT NULL OR compra_id IS NOT NULL.
usuario_reporta_id	BIGINT UNSIGNED	NOT NULL	FK → usuario	Usuario que registra el caso.
cliente_id	BIGINT UNSIGNED	NULL	FK → cliente	Cliente asociado (si aplica).
tipo	ENUM('reclamo','devolucion','cambio','reparacion')	NOT NULL		Tipo de caso.
estado	ENUM('en_proceso','aprobado','rechazado','cerrado')	NOT NULL DEFAULT 'en_proceso'		Estado actual.
descripcion	TEXT	NOT NULL		Descripción del caso.
respuesta_admin	TEXT	NULL		Resolución del administrador.
fecha_apertura	DATETIME	NOT NULL DEFAULT NOW()		
fecha_cierre	DATETIME	NULL		

3.21 Tabla: reparacion (extensión de caso_postventa)
Campo	Tipo	Nulidad	Clave	Descripción
reparacion_id	BIGINT UNSIGNED	NOT NULL	PK	
caso_id	BIGINT UNSIGNED	NOT NULL	FK → caso_postventa, UQ	Un caso, una reparación.
descripcion_trabajo	TEXT	NOT NULL		Descripción del trabajo a realizar.
presupuesto	DECIMAL(14,2)	NULL	CHECK ≥ 0	Presupuesto estimado.
presupuesto_aprobado	TINYINT(1)	NOT NULL DEFAULT 0		1 si el cliente aprobó el presupuesto.
fecha_entrega_estimada	DATE	NULL		
estado	ENUM('recibido','en_proceso','listo','entregado')	NOT NULL DEFAULT 'recibido'		Estado de la reparación.
costo_real	DECIMAL(14,2)	NULL	CHECK ≥ 0	Costo final de la reparación.

3.22 Tabla: inventario_movimiento
Campo	Tipo	Nulidad	Clave	Descripción
movimiento_id	BIGINT UNSIGNED	NOT NULL	PK	
producto_id	BIGINT UNSIGNED	NOT NULL	FK → producto	
tipo	ENUM('entrada','salida','ajuste','reserva','liberacion')	NOT NULL		Tipo de movimiento.
cantidad	INT	NOT NULL	CHECK ≠ 0	Positivo para entradas, negativo para salidas.
stock_antes	INT	NOT NULL	CHECK ≥ 0	Stock antes del movimiento.
stock_despues	INT	NOT NULL	CHECK ≥ 0	Stock después del movimiento.
referencia_tipo	VARCHAR(50)	NOT NULL		Tipo de documento origen: 'venta', 'compra', 'ajuste', 'apartado'.
referencia_id	BIGINT UNSIGNED	NOT NULL		ID del documento origen.
usuario_id	BIGINT UNSIGNED	NOT NULL	FK → usuario	Usuario que generó el movimiento.
motivo	VARCHAR(255)	NULL		Motivo (obligatorio para ajustes manuales).
created_at	DATETIME	NOT NULL DEFAULT NOW()		

3.23 Tabla: metodo_pago
Campo	Tipo	Nulidad	Clave	Descripción
metodo_pago_id	BIGINT UNSIGNED	NOT NULL	PK	
nombre	VARCHAR(100)	NOT NULL	UQ	Nombre del método (Efectivo, Transferencia, Nequi, etc.).
estado	ENUM('activo','inactivo')	NOT NULL DEFAULT 'activo'		No se elimina si tiene transacciones.

3.24 Tabla: credito_compra
Campo	Tipo	Nulidad	Clave	Descripción
credito_id	BIGINT UNSIGNED	NOT NULL	PK	
compra_id	BIGINT UNSIGNED	NOT NULL	FK → compra, UQ	Una compra máximo un crédito activo.
monto_total	DECIMAL(14,2)	NOT NULL	CHECK > 0	
saldo_pendiente	DECIMAL(14,2)	NOT NULL	CHECK ≥ 0	
fecha_inicio	DATE	NOT NULL		
fecha_vencimiento	DATE	NOT NULL	CHECK > fecha_inicio	
estado	ENUM('activo','pagado','vencido')	NOT NULL DEFAULT 'activo'		Estado del crédito.

3.25 Tabla: abono_credito_compra
Campo	Tipo	Nulidad	Clave	Descripción
abono_id	BIGINT UNSIGNED	NOT NULL	PK	
credito_id	BIGINT UNSIGNED	NOT NULL	FK → credito_compra	
metodo_pago_id	BIGINT UNSIGNED	NOT NULL	FK → metodo_pago	
monto	DECIMAL(14,2)	NOT NULL	CHECK > 0	Monto del abono.
fecha_abono	DATETIME	NOT NULL DEFAULT NOW()		
estado	ENUM('registrado','anulado')	NOT NULL DEFAULT 'registrado'		

3.26 Tabla: auditoria_log
Campo	Tipo	Nulidad	Clave	Descripción
log_id	BIGINT UNSIGNED	NOT NULL	PK	
usuario_id	BIGINT UNSIGNED	NULL	FK → usuario	NULL si la acción no requirió autenticación (p.ej. login fallido).
accion	VARCHAR(100)	NOT NULL		Descripción corta: 'CREAR_VENTA', 'EDITAR_USUARIO', etc.
entidad	VARCHAR(100)	NOT NULL		Nombre de la entidad afectada.
entidad_id	BIGINT UNSIGNED	NULL		ID de la entidad afectada.
datos_anteriores	JSON	NULL		Estado previo del registro (para UPDATE/DELETE).
datos_nuevos	JSON	NULL		Estado nuevo del registro (para CREATE/UPDATE).
ip_cliente	VARCHAR(45)	NULL		IP del cliente (IPv4 o IPv6).
user_agent	VARCHAR(500)	NULL		User-Agent del cliente.
fecha_hora	DATETIME	NOT NULL DEFAULT NOW()		Timestamp inmutable de la acción.

3.27 Tabla: token_recuperacion
Campo	Tipo	Nulidad	Clave	Descripción
token_id	BIGINT UNSIGNED	NOT NULL	PK	
usuario_id	BIGINT UNSIGNED	NOT NULL	FK → usuario	
token_hash	VARCHAR(255)	NOT NULL	UQ	Hash del código de recuperación.
expira_en	DATETIME	NOT NULL		15 minutos desde la generación.
usado	TINYINT(1)	NOT NULL DEFAULT 0		1 una vez utilizado.
created_at	DATETIME	NOT NULL DEFAULT NOW()		

3.28 Tabla: promocion
Campo	Tipo	Nulidad	Clave	Descripción
promocion_id	BIGINT UNSIGNED	NOT NULL	PK	
nombre	VARCHAR(150)	NOT NULL		Nombre de la promoción.
tipo	ENUM('porcentaje','monto_fijo')	NOT NULL		Tipo de descuento.
valor	DECIMAL(10,2)	NOT NULL	CHECK > 0	Porcentaje (0-100) o monto fijo.
aplica_a	ENUM('venta','categoria','producto')	NOT NULL		Alcance de la promoción.
entidad_id	BIGINT UNSIGNED	NULL		ID de categoría o producto si aplica_a ≠ 'venta'.
fecha_inicio	DATE	NOT NULL		
fecha_fin	DATE	NOT NULL	CHECK ≥ fecha_inicio	
activa	TINYINT(1)	NOT NULL DEFAULT 1		

3.29 Recomendaciones de Mapeo JPA / Hibernate
Las siguientes directrices aplican al mapear este esquema con Spring Data JPA:
•	Usar @Entity, @Table(name='...') en cada clase de dominio. IDs con @GeneratedValue(strategy=GenerationType.IDENTITY).
•	Relaciones N:1: @ManyToOne(fetch=FetchType.LAZY) por defecto. Cargar con JOIN FETCH solo cuando sea necesario en la query JPQL o en el DTO projection.
•	Relaciones N:M (producto_subcategoria, proveedor_material): modelar con @ManyToMany y @JoinTable, o como entidad de enlace si tiene atributos propios.
•	Campos monetarios: @Column(precision=14, scale=2) mapeados a java.math.BigDecimal. Nunca usar double/float para dinero.
•	Fechas: usar java.time.LocalDate para fechas sin hora, java.time.LocalDateTime para timestamps. Anotar con @Column(columnDefinition='DATETIME').
•	Enumerados: @Enumerated(EnumType.STRING) para persistir el nombre del enum, no el ordinal.
•	Control de concurrencia optimista en stock: campo @Version private Integer version en la entidad Producto. Hibernate lanzará OptimisticLockException si hay conflicto.
•	Auditoría automática: usar @CreatedDate y @LastModifiedDate de Spring Data con @EnableJpaAuditing.
•	Cascada: usar CascadeType.PERSIST y CascadeType.MERGE con precaución. Nunca CascadeType.REMOVE en relaciones de negocio (borrado lógico preferido).
•	Desnormalización controlada: la tabla inventario_movimiento guarda stock_antes y stock_despues para evitar recalcular el historial. La tabla detalle_venta guarda precio_unitario histórico. Esto es intencional y no viola la normalización del modelo transaccional.
•	Para reportes complejos con múltiples joins y agregaciones, usar proyecciones SQL nativas (@Query nativeQuery=true) o una vista de BD dedicada. Considerar un esquema de reporting separado si el volumen crece.

 
4. Lineamientos de Migración — Spring Boot + React

4.1 Backend: Estructura de Capas
La arquitectura sigue el patrón estándar de Spring Boot en capas con separación estricta de responsabilidades:

Capa Controller	Recibe peticiones HTTP. Valida con @Valid los DTOs de entrada. Delega al Service. Retorna ApiResponse<T>. No contiene lógica de negocio. Anotado con @RestController y @RequestMapping.
Capa Service	Contiene toda la lógica de negocio. Gestiona transacciones con @Transactional. Llama a uno o más Repositories. Convierte entre Entidades y DTOs. Lanza excepciones de dominio.
Capa Repository	Interfaces JPA que extienden JpaRepository<T, ID> o PagingAndSortingRepository. Métodos JPQL o SQL nativo para consultas complejas. Un Repository por entidad raíz de agregado.
DTOs	Objetos de transferencia separados de las entidades JPA. DTOs de Request (validación Bean Validation), DTOs de Response (proyección de datos). Usar MapStruct para mapeo automático.
Manejo de Excepciones	@ControllerAdvice con @ExceptionHandler para: EntityNotFoundException (404), AccessDeniedException (403), MethodArgumentNotValidException (400), OptimisticLockingFailureException (409). Respuesta estandarizada ApiResponse<T> con campos: success, message, data, errors, timestamp.
Seguridad Spring Security	SecurityFilterChain con configuración JWT. JwtAuthenticationFilter como OncePerRequestFilter. UserDetailsService que carga el usuario desde BD. @PreAuthorize('hasRole(...)') en métodos de servicio o controlador críticos.
Flyway Migraciones	Scripts en resources/db/migration con naming V{version}__{descripcion}.sql. Ejecución automática al arrancar. Nunca modificar migraciones ya aplicadas.

4.2 Frontend: Arquitectura React 18+
Estructura de Carpetas	src/features/{modulo}/{componentes, hooks, api}. src/shared/components (botones, tablas, modales reutilizables). src/shared/hooks (useAuth, useToast, usePagination). src/shared/api (cliente Axios con interceptores).
Gestión de Estado	Zustand para estado global ligero (usuario autenticado, notificaciones). React Query (TanStack Query) para estado del servidor: caché, refetch, mutaciones, invalidación automática. Estado local de formulario con React Hook Form.
Formularios	React Hook Form + Zod para validación tipada en el cliente. Esquemas Zod reutilizables y alineados con las restricciones del backend. Mensajes de error en español, coherentes con los del API.
Routing	React Router v6 con rutas protegidas (PrivateRoute) que verifican el JWT. Lazy loading por módulo con React.lazy() y Suspense. Rutas anidadas por sección.
Autenticación	Al hacer login, almacenar access token en memoria (variable de módulo) y refresh token en httpOnly cookie. Interceptor Axios para adjuntar Bearer token. Interceptor de respuesta para renovar access token con refresh token ante 401.
Consumo de API	Axios como cliente HTTP. Base URL desde variable de entorno REACT_APP_API_URL. Funciones de API organizadas por módulo (ventasApi.ts, productosApi.ts, etc.). Tipado con TypeScript para requests y responses.
UX / Feedback	React Hot Toast o Sonner para notificaciones toast. Componentes Skeleton para estados de carga. Componente ErrorBoundary por sección. Confirmación de acciones destructivas con modal.

4.3 Comunicación API REST — Convenciones

Convención de URLs	Recursos en plural y kebab-case: /api/v1/productos, /api/v1/casos-postventa, /api/v1/creditos-compra/{id}/abonos. Versión en la URL (/api/v1/).
Métodos HTTP	GET: listar o obtener. POST: crear. PUT: actualizar completo. PATCH: actualización parcial de estado. DELETE: borrado lógico (cambio de estado).
Códigos de Estado	200 OK, 201 Created, 204 No Content (DELETE exitoso), 400 Bad Request (validación), 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict (concurrencia/duplicado), 422 Unprocessable Entity (regla de negocio), 500 Internal Server Error.
Formato de Respuesta	{ 'success': true/false, 'message': '...', 'data': {...} o null, 'errors': [...] o null, 'timestamp': '2026-04-13T10:00:00Z', 'pagination': { 'page': 0, 'size': 20, 'totalElements': 150, 'totalPages': 8 } }
Paginación y Filtrado	Parámetros de query: ?page=0&size=20&sort=campo,asc|desc&filtro1=valor1&filtro2=valor2. Respuesta incluye el objeto pagination anidado en ApiResponse.
Idempotencia	PUT y DELETE son idempotentes. POST no lo es; usar campo idempotency-key en header para operaciones críticas (crear venta, registrar pago) si se requiere protección ante reintentos.

4.4 Índices de Base de Datos Recomendados
Índices adicionales a las PKs y UQs definidas en el diccionario de datos:
•	idx_producto_categoria: producto(categoria_id) — búsqueda de productos por categoría.
•	idx_producto_estado: producto(estado) — filtro de catálogo activo.
•	idx_venta_fecha: venta(fecha_emision) — reportes y filtros por período.
•	idx_venta_usuario: venta(usuario_id) — ventas por vendedor.
•	idx_venta_cliente: venta(cliente_id) — historial de cliente.
•	idx_inventario_movimiento_producto_fecha: inventario_movimiento(producto_id, created_at DESC) — historial de movimientos.
•	idx_auditoria_usuario_fecha: auditoria_log(usuario_id, fecha_hora DESC) — auditoría por usuario.
•	idx_caso_venta: caso_postventa(venta_id) — casos por venta.
•	idx_token_usuario: token_recuperacion(usuario_id, usado, expira_en) — lookup eficiente de tokens activos.

4.5 Plan de Migración Tecnológica
Fase 1 — Fundación (Semanas 1-3)	Configurar proyecto Spring Boot (Initializr). Configurar Flyway con migraciones del esquema mejorado. Implementar módulo de Seguridad (JWT, Spring Security, RBAC). Implementar módulo de Usuarios. Configurar proyecto React con estructura de carpetas, router, Zustand, React Query. Implementar pantallas de login y dashboard base.
Fase 2 — Núcleo de Negocio (Semanas 4-7)	Backend y frontend para: Catálogo (categorías, subcategorías, materiales, productos). Proveedores. Compras. Inventario (movimientos, alertas).
Fase 3 — Ventas y Financiero (Semanas 8-11)	Backend y frontend para: Clientes (CRM básico). Ventas (contado, anticipo, layaway, descuentos, fidelidad). Pagos y créditos. Postventa (incluyendo reparaciones).
Fase 4 — Valor Añadido (Semanas 12-14)	Reportes y exportaciones PDF/Excel. Dashboard con KPIs. Certificados de autenticidad. Garantías. Búsqueda avanzada. Notificaciones por correo.
Fase 5 — Calidad y Despliegue (Semanas 15-16)	Pruebas de integración y E2E. Ajustes de rendimiento (caché, índices). Dockerización completa. Pipeline CI/CD. Documentación OpenAPI final. Backups y monitoreo.

