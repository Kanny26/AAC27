# ROADMAP AAC27

## 1. Resumen del Proyecto

**Nombre:** AAC27 - Sistema de Gestión de Joyería

**Tipo:** Sistema single-tenant para operación de una joyería física.

**Propósito dual:**

- **Producto real de producción:** administrar seguridad, catálogo, proveedores, compras, inventario, ventas, apartados, promociones y postventa.
- **Herramienta de aprendizaje académico:** practicar arquitectura fullstack, diseño de APIs, seguridad, transacciones, pruebas, infraestructura y operación productiva.

**Stack tecnológico objetivo:**

- **Backend:** Java 21, Spring Boot 3.2+, Spring Security + JWT, Spring Data JPA, Flyway, MySQL 8.0+, Redis.
- **Frontend:** React 18, TypeScript, Vite, Zustand, TanStack Query, React Hook Form, Zod, TailwindCSS.
- **Infraestructura:** Docker Compose, Dockerfiles, GitHub Actions, MySQL, Redis, Nginx para producción.
- **Testing:** JUnit 5, Mockito, Spring Security Test, Testcontainers, MockMvc, JaCoCo, Playwright.

**Estado inicial diagnosticado:**

| Módulo | Estado | Observación | Acción requerida |
|---|---:|---|---|
| Backend base | ⚠️ | Existe Spring Boot con Java 21, Security, JPA, Flyway, Actuator, MapStruct y JWT. No se confirmó Redis/Testcontainers configurados. | Validar compilación, cerrar dependencias faltantes y estandarizar perfiles. |
| Seguridad | ⚠️ | Hay Auth, JWT, usuarios y roles. | Endurecer RBAC, refresh tokens, seeds y pruebas de seguridad. |
| Catálogo | ✅ | Módulo más completo: entidades, DTOs, repositorios, services, mapper y controller. | Completar cobertura de pruebas y contratos API. |
| Compra | ⚠️ | Tiene controller/service/repos/entities, pero DTOs limitados y sin mappers. | Validar reglas de inventario, transacciones y concurrencia. |
| Venta | ⚠️ | Tiene controllers/services/entities/DTOs para ventas, clientes, promociones y apartados. | Probar flujo crítico: venta → stock → pago → movimiento. |
| Postventa | ⚠️ | Tiene estructura CRUD base. | Completar estados, historial, reglas y pruebas. |
| Proveedor | 🔴 | Solo entidades y repository; sin DTO, service ni controller. | Implementar API completa. |
| Frontend | 🔴 | No hay React real; archivos base están vacíos. | Crear app Vite React TypeScript desde cero. |
| Infra Docker | 🔴 | `docker-compose.yml`, `backend/Dockerfile` y `frontend/Dockerfile` están vacíos. | Definir MySQL, Redis, backend, frontend y healthchecks. |
| Migraciones | ✅ | Existen migraciones Flyway V1–V9. | Validar contra entidades y ejecutar en MySQL limpio. |
| Tests/CI | 🔴 | No se encontraron tests ni workflows CI. | Añadir pruebas unitarias, integración y pipeline CI. |

## 2. Roadmap por Fases

### Fase 0 — Base verificable

**Objetivo principal:** Convertir el repositorio en un entorno ejecutable y testeable antes de construir nuevas features.

**Entregables clave:**

- **Backend:** compilación estable, perfiles alineados, smoke tests.
- **Frontend:** app Vite React TypeScript mínima y compilable.
- **Infra:** Docker Compose, MySQL, Redis, Dockerfiles.
- **Testing:** pruebas smoke y CI inicial.

**Dependencias críticas:** Ninguna.

**Criterios de aceptación:**

- `docker compose up` levanta MySQL y Redis.
- Backend compila correctamente.
- Frontend ejecuta build correctamente.
- Flyway aplica migraciones V1–V9 en base limpia.
- CI ejecuta build básico de backend y frontend.

**Complejidad estimada:** M  
**Tiempo sugerido:** 1–2 semanas

### Fase 1 — Backend core estable

**Objetivo principal:** Cerrar seguridad, errores, contratos REST y reglas críticas de catálogo/proveedor.

**Entregables clave:**

- **Backend:** JWT endurecido, RBAC, Proveedor CRUD, contratos REST normalizados, OpenAPI.
- **Frontend:** preparación de contratos para consumo posterior.
- **Infra:** configuración compatible con ambientes.
- **Testing:** unit tests y MockMvc para seguridad/core.

**Dependencias críticas:** Fase 0.

**Criterios de aceptación:**

- Login protegido y validado.
- Permisos por rol aplicados.
- Proveedor completo.
- Errores API consistentes.
- Cobertura inicial útil en services core.

**Complejidad estimada:** M  
**Tiempo sugerido:** 2 semanas

### Fase 2 — Inventario, compras y ventas

**Objetivo principal:** Consolidar flujos transaccionales que afectan stock y dinero.

**Entregables clave:**

- **Backend:** compra, venta, apartados, promociones, movimientos, locks, auditoría operativa inicial.
- **Frontend:** contratos listos para POS e inventario.
- **Infra:** MySQL validado con transacciones reales.
- **Testing:** integración con Testcontainers/MySQL.

**Dependencias críticas:** Fases 0–1.

**Criterios de aceptación:**

- Stock nunca queda negativo.
- Venta registra pagos y descuenta inventario.
- Compra incrementa inventario.
- Apartados gestionan reserva y abonos.
- Pruebas de integración validan flujos críticos.

**Complejidad estimada:** L  
**Tiempo sugerido:** 3–4 semanas

### Fase 3 — Frontend operativo

**Objetivo principal:** Construir la interfaz real para operación diaria en tienda.

**Entregables clave:**

- **Backend:** endpoints estables consumidos por UI.
- **Frontend:** layout, auth, catálogo, clientes, proveedores, compras, ventas, apartados, postventa.
- **Infra:** configuración de Vite por ambiente.
- **Testing:** validaciones UI y preparación e2e.

**Dependencias críticas:** Fases 0–2 parcialmente.

**Criterios de aceptación:**

- Login funcional.
- Rutas protegidas.
- Formularios validados con Zod.
- Tablas con paginación/filtros.
- Manejo consistente de errores API.

**Complejidad estimada:** L  
**Tiempo sugerido:** 4 semanas

### Fase 4 — Calidad productiva

**Objetivo principal:** Elevar confiabilidad, observabilidad, seguridad y mantenibilidad.

**Entregables clave:**

- **Backend:** Testcontainers, Actuator, logs, correlation id, rate limiting, auditoría.
- **Frontend:** lint/typecheck/e2e básicos.
- **Infra:** backups, restore, quality gates.
- **Testing:** cobertura, integración y e2e.

**Dependencias críticas:** Fases 1–3.

**Criterios de aceptación:**

- CI bloquea fallos.
- Healthchecks útiles.
- Logs trazables.
- Backups documentados y probados.
- Endpoints sensibles protegidos.

**Complejidad estimada:** M/L  
**Tiempo sugerido:** 2–3 semanas

### Fase 5 — Release producción

**Objetivo principal:** Empaquetar, desplegar y dejar operación controlada.

**Entregables clave:**

- **Backend:** perfil productivo y bootstrap seguro.
- **Frontend:** build estático productivo.
- **Infra:** Compose prod, Nginx/TLS, pipeline release, runbook.
- **Testing:** checklist preproducción y ensayo de despliegue.

**Dependencias críticas:** Fases 0–4.

**Criterios de aceptación:**

- Deploy reproducible.
- Rollback documentado.
- Usuario admin inicial seguro.
- Monitoreo básico.
- Manual de operación listo.

**Complejidad estimada:** M  
**Tiempo sugerido:** 1–2 semanas

## 3. Plan de Implementación Granular

### Fase 0 — Base verificable

#### T-01

**ID de tarea:** T-01

**Descripción técnica precisa:** Auditar compilación backend y alinear dependencias declaradas contra el stack objetivo, agregando únicamente faltantes base como Redis/Testcontainers si se confirma su uso inmediato.

**Archivos/capas afectados:** `backend/pom.xml`, `backend/src/main/resources/application.yml`, `backend/src/main/resources/application-dev.yml`

**Stack/librerías a usar:** Maven, Spring Boot 3.2.5, Java 21, Spring Data JPA, Flyway, Spring Security, Spring Boot Test.

**Criterios de éxito medibles:**

- `mvn test` compila.
- Dependencias duplicadas o ausentes quedan documentadas.
- No se agregan librerías sin uso.
- Perfiles `dev` y `prod` arrancan con variables claras.

**💡 Concepto de Aprendizaje:** Dependency Management; es como revisar una caja de herramientas antes de construir: si falta una llave crítica, el trabajo se detiene; importa aquí porque el backend ya tiene módulos avanzados, pero producción exige builds reproducibles.

**Prompt listo para IA:** “Audita `backend/pom.xml` y los archivos `application*.yml` del proyecto AAC27. Verifica si las dependencias coinciden con Java 21 + Spring Boot 3.2 + Security + JPA + Flyway + MySQL + Redis + tests. Propón y aplica cambios mínimos para que el backend compile de forma reproducible, sin introducir librerías innecesarias.”

#### T-02

**ID de tarea:** T-02

**Descripción técnica precisa:** Crear `docker-compose.yml` de desarrollo con MySQL 8, Redis, backend opcional y red/volúmenes persistentes.

**Archivos/capas afectados:** `docker-compose.yml`, `.env.example`, `backend/src/main/resources/application-dev.yml`

**Stack/librerías a usar:** Docker Compose, MySQL 8, Redis 7, Spring Boot externalized config.

**Criterios de éxito medibles:**

- `docker compose up -d mysql redis` levanta servicios.
- MySQL expone `3306`.
- Redis expone `6379`.
- Credenciales coinciden con `application-dev.yml`.
- Healthchecks reportan healthy.

**💡 Concepto de Aprendizaje:** Infrastructure as Code; es como tener una receta exacta de cocina en vez de cocinar “a ojo”; importa porque cualquier PC debe levantar la misma base de datos y cache.

**Prompt listo para IA:** “Crea un `docker-compose.yml` para AAC27 con servicios `mysql` y `redis`, volúmenes persistentes, healthchecks, red interna y variables compatibles con `application-dev.yml`. Agrega `.env.example` sin secretos reales y explica cómo levantarlo.”

#### T-03

**ID de tarea:** T-03

**Descripción técnica precisa:** Implementar Dockerfile backend multi-stage para compilar con Maven y ejecutar JAR con JRE 21.

**Archivos/capas afectados:** `backend/Dockerfile`, `backend/.dockerignore`, `docker-compose.yml`

**Stack/librerías a usar:** Eclipse Temurin 21, Maven, Spring Boot executable JAR, Docker multi-stage builds.

**Criterios de éxito medibles:**

- `docker build backend` genera imagen.
- Contenedor expone `8080`.
- App recibe variables por entorno.
- Imagen final no incluye cachés Maven ni código fuente innecesario.

**💡 Concepto de Aprendizaje:** Multi-stage Build; es como cocinar en una cocina grande y servir solo el plato final, no todos los utensilios; importa porque reduce tamaño y superficie de ataque.

**Prompt listo para IA:** “Implementa un Dockerfile multi-stage para `backend` usando Java 21 y Maven. Debe compilar el JAR Spring Boot, producir una imagen runtime liviana, respetar variables de entorno y agregar `.dockerignore`.”

#### T-04

**ID de tarea:** T-04

**Descripción técnica precisa:** Crear frontend real con Vite React TypeScript en la ubicación correcta, incluyendo estructura inicial de rutas, estilos y cliente API base.

**Archivos/capas afectados:** `frontend/package.json`, `frontend/vite.config.ts`, `frontend/src/main.tsx`, `frontend/src/App.tsx`, `frontend/src/index.css`

**Stack/librerías a usar:** React 18, TypeScript, Vite, TailwindCSS, TanStack Query, Zustand, React Hook Form, Zod.

**Criterios de éxito medibles:**

- `npm install` funciona.
- `npm run dev` levanta Vite.
- `npm run build` genera `dist`.
- TypeScript compila sin errores.
- Estructura no queda dentro de `src/public`.

**💡 Concepto de Aprendizaje:** Frontend Application Shell; es como construir el tablero de control antes de conectar todos los botones; importa porque hoy el frontend está vacío y será la interfaz operativa del negocio.

**Prompt listo para IA:** “Inicializa el frontend real de AAC27 con Vite + React 18 + TypeScript. Corrige la estructura actual vacía, instala TailwindCSS, TanStack Query, Zustand, React Hook Form y Zod, y deja una pantalla inicial compilable con cliente API base.”

#### T-05

**ID de tarea:** T-05

**Descripción técnica precisa:** Agregar smoke tests mínimos para backend: contexto Spring, endpoint health y validación de Flyway con base de datos disponible.

**Archivos/capas afectados:** `backend/src/test/java/com/aac27/joyeria/JoyeriaApplicationTests.java`, `backend/src/test/resources/application-test.yml`, `backend/pom.xml`

**Stack/librerías a usar:** JUnit 5, Spring Boot Test, Spring Security Test, Testcontainers MySQL si se habilita.

**Criterios de éxito medibles:**

- Existe al menos un test de carga de contexto.
- `mvn test` ejecuta pruebas.
- Perfil `test` no usa credenciales productivas.
- Fallos de migración rompen el build.

**💡 Concepto de Aprendizaje:** Smoke Testing; es como encender un carro antes de un viaje largo para saber si arranca; importa porque antes de crear features hay que detectar errores estructurales rápido.

**Prompt listo para IA:** “Agrega pruebas smoke al backend AAC27. Crea perfil `test`, prueba de carga de contexto Spring Boot y verificación básica de Actuator/Flyway. Mantén el alcance mínimo para estabilizar la base sin probar reglas de negocio todavía.”

#### T-06

**ID de tarea:** T-06

**Descripción técnica precisa:** Crear pipeline CI inicial que compile backend, instale/build frontend y valide que no haya fallos básicos en cada push/PR.

**Archivos/capas afectados:** `.github/workflows/ci.yml`, `backend/pom.xml`, `frontend/package.json`

**Stack/librerías a usar:** GitHub Actions, setup-java, setup-node, Maven cache, npm cache.

**Criterios de éxito medibles:**

- Workflow corre en push y PR.
- Backend ejecuta `mvn test`.
- Frontend ejecuta `npm ci` y `npm run build`.
- Logs separan claramente backend/frontend.
- CI falla si falla compilación.

**💡 Concepto de Aprendizaje:** Continuous Integration; es como una lista de chequeo automática antes de abrir la tienda; importa porque protege el avance académico y productivo contra regresiones.

**Prompt listo para IA:** “Crea un workflow GitHub Actions para AAC27 que ejecute CI básico: Java 21 + Maven para backend y Node + npm para frontend. Debe correr en push/PR, usar caché y fallar si tests o build fallan.”

### Fase 1 — Backend core estable

#### T-07

**ID de tarea:** T-07

**Descripción técnica precisa:** Revisar y endurecer autenticación JWT: login, emisión de access token, validación del filtro, expiración, errores 401 y carga de usuario por email/username.

**Archivos/capas afectados:** `backend/src/main/java/com/aac27/joyeria/module/seguridad/service/AuthService.java`, `backend/src/main/java/com/aac27/joyeria/config/JwtAuthenticationFilter.java`, `backend/src/main/java/com/aac27/joyeria/config/JwtUtil.java`, `backend/src/main/java/com/aac27/joyeria/config/SecurityConfig.java`

**Stack/librerías a usar:** Spring Security, JJWT, BCrypt, Spring Boot Validation.

**Criterios de éxito medibles:**

- Login válido retorna JWT.
- Login inválido retorna 401.
- Endpoints protegidos rechazan requests sin token.
- Token expirado/inválido no autentica.
- Tests cubren éxito y fallo.

**💡 Concepto de Aprendizaje:** Stateless Authentication; es como una pulsera de acceso temporal en un evento: cada entrada se valida sin preguntar de nuevo en recepción; importa porque AAC27 necesita seguridad simple, escalable y sin sesiones servidor.

**Prompt listo para IA:** “Revisa y endurece el flujo JWT del backend AAC27. Valida `AuthService`, `JwtUtil`, `JwtAuthenticationFilter` y `SecurityConfig`; corrige fallos de seguridad, estandariza respuestas 401 y agrega pruebas de login válido/inválido y endpoint protegido.”

#### T-08

**ID de tarea:** T-08

**Descripción técnica precisa:** Implementar autorización RBAC consistente por roles para módulos críticos: seguridad, catálogo, compras, ventas, proveedores y postventa.

**Archivos/capas afectados:** `backend/src/main/java/com/aac27/joyeria/config/SecurityConfig.java`, `backend/src/main/java/com/aac27/joyeria/shared/constant/RolConstants.java`, controllers en `backend/src/main/java/com/aac27/joyeria/module/**/controller/*.java`

**Stack/librerías a usar:** Spring Security Authorization, `@PreAuthorize`, method security, Spring Security Test.

**Criterios de éxito medibles:**

- Roles tienen permisos explícitos.
- Usuario sin rol correcto recibe 403.
- Admin puede operar módulos administrativos.
- Cajero/vendedor solo accede a ventas/catálogo permitido.
- Tests cubren al menos 3 roles.

**💡 Concepto de Aprendizaje:** Role-Based Access Control; es como entregar llaves distintas al cajero, administrador y técnico; importa porque una joyería maneja dinero, inventario y datos sensibles.

**Prompt listo para IA:** “Diseña e implementa RBAC en AAC27 usando roles existentes. Define permisos por módulo en `SecurityConfig` o `@PreAuthorize`, actualiza constantes de roles y agrega pruebas con `@WithMockUser` para validar 401/403/200.”

#### T-09

**ID de tarea:** T-09

**Descripción técnica precisa:** Estandarizar modelo de errores API con códigos, mensajes, timestamp, path y detalles de validación para Bean Validation.

**Archivos/capas afectados:** `backend/src/main/java/com/aac27/joyeria/shared/exception/GlobalExceptionHandler.java`, `backend/src/main/java/com/aac27/joyeria/shared/response/ApiResponse.java`, `backend/src/main/java/com/aac27/joyeria/shared/exception/*.java`

**Stack/librerías a usar:** Spring MVC Exception Handling, Bean Validation, Jackson.

**Criterios de éxito medibles:**

- `@Valid` inválido retorna 400 con campos.
- Recurso inexistente retorna 404.
- Regla de negocio retorna 409/422 según caso.
- Formato JSON es consistente.
- Tests cubren 400, 404 y 409.

**💡 Concepto de Aprendizaje:** Consistent Error Contract; es como usar el mismo formato de recibo para todas las compras; importa porque frontend y soporte necesitan interpretar errores sin lógica especial por endpoint.

**Prompt listo para IA:** “Estandariza las respuestas de error del backend AAC27. Revisa `GlobalExceptionHandler`, excepciones compartidas y `ApiResponse`; implementa un contrato JSON consistente para validación, 404, conflictos y errores inesperados, con pruebas MockMvc.”

#### T-10

**ID de tarea:** T-10

**Descripción técnica precisa:** Implementar módulo Proveedor completo: DTOs, mapper, service transaccional, controller REST y validaciones de contacto.

**Archivos/capas afectados:** `backend/src/main/java/com/aac27/joyeria/module/proveedor/entity/*.java`, `backend/src/main/java/com/aac27/joyeria/module/proveedor/repository/ProveedorRepository.java`, `backend/src/main/java/com/aac27/joyeria/module/proveedor/dto/*.java`, `backend/src/main/java/com/aac27/joyeria/module/proveedor/service/ProveedorService.java`, `backend/src/main/java/com/aac27/joyeria/module/proveedor/controller/ProveedorController.java`

**Stack/librerías a usar:** Spring Data JPA, MapStruct, Bean Validation, Lombok, Spring MVC.

**Criterios de éxito medibles:**

- CRUD proveedor funcional.
- Contactos email/teléfono se persisten.
- Datos inválidos retornan 400.
- Búsqueda por nombre/NIT funciona.
- Pruebas cubren crear, listar, actualizar y desactivar.

**💡 Concepto de Aprendizaje:** Layered Architecture; es como separar mostrador, bodega y caja: cada parte tiene una responsabilidad; importa porque Proveedor hoy está incompleto y debe seguir el mismo patrón del resto del backend.

**Prompt listo para IA:** “Completa el módulo Proveedor de AAC27 siguiendo la arquitectura existente. Agrega DTOs, MapStruct mapper, service transaccional, controller REST, validaciones y pruebas. Respeta entidades y migraciones existentes.”

#### T-11

**ID de tarea:** T-11

**Descripción técnica precisa:** Revisar contrato REST de Catálogo y normalizar endpoints, paginación, filtros, ordenamiento y respuestas.

**Archivos/capas afectados:** `backend/src/main/java/com/aac27/joyeria/module/catalogo/controller/CategoriaController.java`, `backend/src/main/java/com/aac27/joyeria/module/catalogo/service/*.java`, `backend/src/main/java/com/aac27/joyeria/module/catalogo/repository/*.java`, `backend/src/main/java/com/aac27/joyeria/module/catalogo/dto/*.java`

**Stack/librerías a usar:** Spring MVC, Spring Data Pageable, Bean Validation, MapStruct.

**Criterios de éxito medibles:**

- Listados soportan `page`, `size`, `sort`.
- Filtros básicos funcionan para productos/categorías.
- Respuestas no exponen entidades JPA.
- Endpoints siguen `/api/v1/...`.
- Tests cubren al menos listado y creación.

**💡 Concepto de Aprendizaje:** API Contract Design; es como definir un menú claro para que todos sepan qué pedir y qué recibir; importa porque el frontend dependerá directamente de contratos estables.

**Prompt listo para IA:** “Audita y normaliza los endpoints REST del módulo Catálogo en AAC27. Asegura DTOs, paginación con Pageable, filtros básicos, rutas `/api/v1`, respuestas consistentes y pruebas de controller/service.”

#### T-12

**ID de tarea:** T-12

**Descripción técnica precisa:** Agregar documentación OpenAPI/Swagger para exponer contratos de autenticación, catálogo, proveedor y health.

**Archivos/capas afectados:** `backend/pom.xml`, `backend/src/main/java/com/aac27/joyeria/config/OpenApiConfig.java`, controllers en `backend/src/main/java/com/aac27/joyeria/module/**/controller/*.java`

**Stack/librerías a usar:** springdoc-openapi, Swagger UI, Spring Boot.

**Criterios de éxito medibles:**

- `/swagger-ui.html` o `/swagger-ui/index.html` carga.
- `/v3/api-docs` responde JSON.
- Endpoints protegidos muestran esquema Bearer JWT.
- DTOs principales tienen descripción mínima.
- CI compila con la nueva dependencia.

**💡 Concepto de Aprendizaje:** API Documentation as Contract; es como tener un catálogo de productos actualizado para clientes y vendedores; importa porque acelera frontend, testing manual y revisión académica.

**Prompt listo para IA:** “Agrega documentación OpenAPI al backend AAC27 con springdoc-openapi. Configura título, versión, Bearer JWT, agrupación básica y anotaciones útiles en Auth, Catálogo y Proveedor. Verifica `/v3/api-docs` y Swagger UI.”

#### T-13

**ID de tarea:** T-13

**Descripción técnica precisa:** Crear suite de pruebas unitarias inicial para services core usando Mockito y AssertJ, priorizando Auth, Catálogo y Proveedor.

**Archivos/capas afectados:** `backend/src/test/java/com/aac27/joyeria/module/seguridad/service/AuthServiceTest.java`, `backend/src/test/java/com/aac27/joyeria/module/catalogo/service/*.java`, `backend/src/test/java/com/aac27/joyeria/module/proveedor/service/ProveedorServiceTest.java`

**Stack/librerías a usar:** JUnit 5, Mockito, AssertJ, Spring Boot Test.

**Criterios de éxito medibles:**

- Tests no dependen de MySQL real.
- Casos felices y errores cubiertos.
- Mocks verifican interacciones críticas.
- `mvn test` ejecuta suite.
- Cobertura de services core sube desde cero a una base útil.

**💡 Concepto de Aprendizaje:** Unit Testing with Mocks; es como practicar una venta con billetes falsos antes de abrir caja real; importa porque permite validar reglas sin levantar toda la infraestructura.

**Prompt listo para IA:** “Crea pruebas unitarias con JUnit 5, Mockito y AssertJ para services core de AAC27: AuthService, servicios de Catálogo y ProveedorService. Cubre casos exitosos, recurso no encontrado, conflicto y validaciones de reglas.”

#### T-14

**ID de tarea:** T-14

**Descripción técnica precisa:** Validar consistencia entidad-migración entre JPA y Flyway para módulos seguridad, catálogo y proveedor.

**Archivos/capas afectados:** `backend/src/main/resources/db/migration/V1__seguridad_y_usuarios.sql`, `backend/src/main/resources/db/migration/V2__catalogo_base.sql`, `backend/src/main/resources/db/migration/V3__productos.sql`, `backend/src/main/resources/db/migration/V4__proveedores_evaluacion.sql`, entidades en `backend/src/main/java/com/aac27/joyeria/module/**/entity/*.java`

**Stack/librerías a usar:** Hibernate schema validation, Flyway, MySQL/Testcontainers.

**Criterios de éxito medibles:**

- `ddl-auto=validate` no falla contra migraciones limpias.
- Nombres de columnas coinciden.
- Constraints críticas existen.
- Índices de búsqueda básicos están presentes.
- Inconsistencias quedan corregidas o documentadas.

**💡 Concepto de Aprendizaje:** Schema-First Discipline; es como que el plano del local coincida con la construcción real; importa porque si entidades y migraciones divergen, producción falla al arrancar.

**Prompt listo para IA:** “Valida que las entidades JPA de seguridad, catálogo y proveedor coincidan con las migraciones Flyway V1–V4. Usa `ddl-auto=validate`, identifica divergencias de tablas/columnas/constraints y aplica correcciones mínimas preservando datos.”

### Fase 2 — Inventario, compras y ventas

#### T-15

**ID de tarea:** T-15

**Descripción técnica precisa:** Auditar el modelo de inventario actual y definir una regla única de stock basada en productos, movimientos y trazabilidad.

**Archivos/capas afectados:** `backend/src/main/java/com/aac27/joyeria/module/catalogo/entity/Producto.java`, `backend/src/main/java/com/aac27/joyeria/module/catalogo/entity/ProductoTrazabilidad.java`, `backend/src/main/java/com/aac27/joyeria/module/compra/entity/InventarioMovimiento.java`, `backend/src/main/java/com/aac27/joyeria/module/compra/repository/InventarioMovimientoRepository.java`, migraciones en `backend/src/main/resources/db/migration/*.sql`

**Stack/librerías a usar:** Spring Data JPA, Flyway, MySQL constraints, Bean Validation.

**Criterios de éxito medibles:**

- Existe una fuente clara para stock disponible.
- Movimientos tienen tipo, cantidad, referencia y fecha.
- No se permite stock negativo.
- Entidades coinciden con migraciones.
- Reglas quedan cubiertas por tests.

**💡 Concepto de Aprendizaje:** Inventory Ledger; es como una libreta de entradas y salidas de caja, donde no basta saber el saldo: necesitas saber cada movimiento; importa porque joyería maneja productos de alto valor y trazabilidad.

**Prompt listo para IA:** “Audita el modelo de inventario de AAC27. Revisa Producto, ProductoTrazabilidad, InventarioMovimiento y migraciones. Define y aplica una regla consistente de stock disponible, movimientos de entrada/salida/ajuste, validaciones para evitar stock negativo y pruebas de service.”

#### T-16

**ID de tarea:** T-16

**Descripción técnica precisa:** Endurecer flujo de compras para registrar encabezado, detalles, proveedor, método de pago, crédito si aplica y movimientos de entrada de inventario en una transacción.

**Archivos/capas afectados:** `backend/src/main/java/com/aac27/joyeria/module/compra/service/CompraService.java`, `backend/src/main/java/com/aac27/joyeria/module/compra/controller/CompraController.java`, `backend/src/main/java/com/aac27/joyeria/module/compra/dto/CompraRequest.java`, `backend/src/main/java/com/aac27/joyeria/module/compra/dto/CompraResponse.java`, `backend/src/main/java/com/aac27/joyeria/module/compra/entity/*.java`

**Stack/librerías a usar:** Spring `@Transactional`, Spring Data JPA, Bean Validation, MapStruct/manual mapper, MySQL.

**Criterios de éxito medibles:**

- Crear compra incrementa stock.
- Compra con proveedor inexistente falla 404.
- Totales se recalculan en backend.
- Rollback revierte stock si falla un detalle.
- Test de integración cubre compra completa.

**💡 Concepto de Aprendizaje:** Transaction Boundary; es como vender un set de joyas: o se registra todo junto, o no se registra nada; importa porque compra y stock no pueden quedar inconsistentes.

**Prompt listo para IA:** “Endurece el flujo de compras en AAC27. Revisa CompraService y DTOs para que una compra registre encabezado, detalles, proveedor, método de pago/crédito y movimientos de entrada de inventario dentro de una única transacción con rollback y tests.”

#### T-17

**ID de tarea:** T-17

**Descripción técnica precisa:** Endurecer flujo de ventas para registrar cliente opcional, detalles, pagos, descuentos/promociones, salida de inventario y validación de stock.

**Archivos/capas afectados:** `backend/src/main/java/com/aac27/joyeria/module/venta/service/VentaService.java`, `backend/src/main/java/com/aac27/joyeria/module/venta/controller/VentaController.java`, `backend/src/main/java/com/aac27/joyeria/module/venta/dto/VentaRequest.java`, `backend/src/main/java/com/aac27/joyeria/module/venta/dto/VentaResponse.java`, `backend/src/main/java/com/aac27/joyeria/module/venta/entity/*.java`

**Stack/librerías a usar:** Spring `@Transactional`, JPA pessimistic locking, Bean Validation, MySQL constraints.

**Criterios de éxito medibles:**

- Venta válida descuenta stock.
- Venta sin stock suficiente falla 409.
- Total se calcula en backend.
- Pagos no superan reglas definidas.
- Rollback revierte inventario si falla la venta.

**💡 Concepto de Aprendizaje:** Domain Invariant; es como una regla de caja que nadie puede saltarse: no puedes vender una pieza que no existe; importa porque protege dinero, inventario y confianza operativa.

**Prompt listo para IA:** “Endurece VentaService en AAC27 para que la venta calcule totales en backend, valide stock, aplique descuentos/promociones permitidas, registre pagos, descuente inventario con movimiento de salida y haga rollback completo ante errores. Agrega pruebas de stock suficiente e insuficiente.”

#### T-18

**ID de tarea:** T-18

**Descripción técnica precisa:** Implementar control de concurrencia para compras/ventas/apartados sobre productos usando locking explícito y pruebas de carrera.

**Archivos/capas afectados:** `backend/src/main/java/com/aac27/joyeria/module/compra/repository/ProductoLockRepository.java`, `backend/src/main/java/com/aac27/joyeria/module/venta/service/VentaService.java`, `backend/src/main/java/com/aac27/joyeria/module/compra/service/CompraService.java`, `backend/src/test/java/com/aac27/joyeria/module/venta/*.java`

**Stack/librerías a usar:** JPA `@Lock`, `LockModeType.PESSIMISTIC_WRITE`, Spring transactions, Testcontainers/MySQL.

**Criterios de éxito medibles:**

- Dos ventas simultáneas no generan stock negativo.
- Lock se toma antes de modificar stock.
- Timeout/errores se manejan limpiamente.
- Test concurrente reproduce y valida la protección.

**💡 Concepto de Aprendizaje:** Pessimistic Locking; es como apartar físicamente una joya mientras se procesa el pago para que nadie más la venda; importa porque stock de joyería suele ser bajo y unitario.

**Prompt listo para IA:** “Implementa control de concurrencia en AAC27 para modificaciones de stock. Usa `ProductoLockRepository` con locking pesimista, aplica locks en ventas/compras/apartados y agrega una prueba concurrente que demuestre que no se produce stock negativo.”

#### T-19

**ID de tarea:** T-19

**Descripción técnica precisa:** Consolidar módulo de apartados: creación, abonos, vencimiento, cancelación, liberación o conversión a venta.

**Archivos/capas afectados:** `backend/src/main/java/com/aac27/joyeria/module/venta/service/ApartadoService.java`, `backend/src/main/java/com/aac27/joyeria/module/venta/controller/ApartadoController.java`, `backend/src/main/java/com/aac27/joyeria/module/venta/dto/AbonoApartadoRequest.java`, `backend/src/main/java/com/aac27/joyeria/module/venta/dto/ApartadoResponse.java`, `backend/src/main/java/com/aac27/joyeria/module/venta/entity/Apartado.java`, `backend/src/main/java/com/aac27/joyeria/module/venta/entity/AbonoApartado.java`

**Stack/librerías a usar:** Spring transactions, JPA, Bean Validation, scheduled jobs opcional.

**Criterios de éxito medibles:**

- Apartado reserva stock o cambia disponibilidad.
- Abonos actualizan saldo.
- Cancelación libera producto.
- Conversión a venta respeta pagos.
- Tests cubren crear, abonar, cancelar y completar.

**💡 Concepto de Aprendizaje:** State Machine; es como el estado de un pedido: reservado, pagado, cancelado o vencido; importa porque apartados tienen reglas temporales y financieras que no caben en un booleano.

**Prompt listo para IA:** “Consolida el flujo de apartados en AAC27. Define estados claros, creación con reserva, abonos, cancelación, vencimiento y conversión a venta. Implementa reglas transaccionales y pruebas para cada transición válida e inválida.”

#### T-20

**ID de tarea:** T-20

**Descripción técnica precisa:** Consolidar promociones/descuentos aplicables a venta con fechas, elegibilidad, prioridad y validación de totales.

**Archivos/capas afectados:** `backend/src/main/java/com/aac27/joyeria/module/venta/service/PromocionService.java`, `backend/src/main/java/com/aac27/joyeria/module/venta/controller/PromocionController.java`, `backend/src/main/java/com/aac27/joyeria/module/venta/entity/Promocion.java`, `backend/src/main/java/com/aac27/joyeria/module/venta/dto/PromocionRequest.java`, `backend/src/main/java/com/aac27/joyeria/module/venta/dto/PromocionResponse.java`

**Stack/librerías a usar:** Spring Data JPA Specifications o queries derivadas, Bean Validation, Java Time API.

**Criterios de éxito medibles:**

- Promoción activa solo aplica dentro de fechas.
- Descuento máximo validado.
- Ventas no aceptan descuentos arbitrarios desde frontend.
- Tests cubren promoción vigente, vencida e inválida.

**💡 Concepto de Aprendizaje:** Business Rule Encapsulation; es como que el cajero no invente descuentos: consulta una lista autorizada; importa porque protege márgenes y evita fraude operativo.

**Prompt listo para IA:** “Revisa y consolida promociones en AAC27. Implementa reglas de fechas, estado activo, tipo de descuento, límites y aplicación desde VentaService sin confiar en el frontend. Agrega pruebas de promociones vigentes, vencidas e inválidas.”

#### T-21

**ID de tarea:** T-21

**Descripción técnica precisa:** Implementar endpoints de consulta operativa para inventario: kardex/movimientos, bajo stock, disponibilidad por producto y trazabilidad.

**Archivos/capas afectados:** `backend/src/main/java/com/aac27/joyeria/module/catalogo/controller/*.java`, `backend/src/main/java/com/aac27/joyeria/module/catalogo/service/ProductoService.java`, `backend/src/main/java/com/aac27/joyeria/module/compra/repository/InventarioMovimientoRepository.java`, nuevos DTOs en `backend/src/main/java/com/aac27/joyeria/module/catalogo/dto/*.java`

**Stack/librerías a usar:** Spring Data JPA projections, Pageable, REST DTOs, OpenAPI.

**Criterios de éxito medibles:**

- Endpoint lista movimientos paginados.
- Endpoint bajo stock filtra correctamente.
- Endpoint disponibilidad responde stock/estado.
- Consultas no exponen entidades.
- Pruebas MockMvc validan contratos.

**💡 Concepto de Aprendizaje:** Read Model; es como tener reportes de bodega separados del proceso de compra/venta; importa porque operación diaria necesita consultar rápido sin tocar reglas transaccionales.

**Prompt listo para IA:** “Agrega consultas operativas de inventario a AAC27: movimientos/kardex paginado, productos bajo stock, disponibilidad por producto y trazabilidad. Usa DTOs/projections, no expongas entidades, documenta con OpenAPI y agrega pruebas de controller.”

#### T-22

**ID de tarea:** T-22

**Descripción técnica precisa:** Crear pruebas de integración para flujos completos compra → inventario → venta → movimiento usando base MySQL real con Testcontainers.

**Archivos/capas afectados:** `backend/src/test/java/com/aac27/joyeria/integration/CompraVentaIntegrationTest.java`, `backend/src/test/resources/application-test.yml`, `backend/pom.xml`, migraciones Flyway.

**Stack/librerías a usar:** Testcontainers MySQL, JUnit 5, Spring Boot Test, Flyway, AssertJ.

**Criterios de éxito medibles:**

- Test levanta MySQL aislado.
- Flyway aplica migraciones.
- Compra incrementa stock.
- Venta descuenta stock.
- Movimientos quedan registrados.
- Pipeline CI puede ejecutar la suite o dejarla separada como perfil de integración.

**💡 Concepto de Aprendizaje:** Integration Testing; es como probar toda la ruta de una joya desde proveedor hasta cliente, no solo cada mostrador por separado; importa porque los errores graves aparecen entre módulos.

**Prompt listo para IA:** “Crea pruebas de integración en AAC27 con Testcontainers MySQL para el flujo compra → inventario → venta → movimientos. Configura perfil `test`, aplica Flyway, prepara datos mínimos y valida stock final y registros generados.”

### Fase 3 — Frontend operativo

#### T-23

**ID de tarea:** T-23

**Descripción técnica precisa:** Definir arquitectura frontend base con rutas protegidas, layout principal, providers globales y estructura modular por dominio.

**Archivos/capas afectados:** `frontend/src/main.tsx`, `frontend/src/App.tsx`, `frontend/src/app/router.tsx`, `frontend/src/app/providers.tsx`, `frontend/src/layouts/MainLayout.tsx`, `frontend/src/features/*`

**Stack/librerías a usar:** React 18, TypeScript, Vite, React Router, TanStack Query, Zustand, TailwindCSS.

**Criterios de éxito medibles:**

- App renderiza sin errores.
- Rutas públicas y privadas existen.
- Layout contiene navegación base.
- Providers de Query/Auth están conectados.
- Build TypeScript pasa.

**💡 Concepto de Aprendizaje:** Feature-Based Architecture; es como organizar una tienda por secciones —ventas, inventario, proveedores— en vez de mezclar todo en una sola bodega; importa porque el frontend crecerá por módulos del negocio.

**Prompt listo para IA:** “Diseña la arquitectura base del frontend AAC27 con React 18 + TypeScript. Crea rutas protegidas, providers globales, layout principal, navegación y estructura por features. Usa TanStack Query, Zustand y TailwindCSS sin implementar aún pantallas complejas.”

#### T-24

**ID de tarea:** T-24

**Descripción técnica precisa:** Implementar cliente HTTP tipado con manejo centralizado de JWT, refresh/error handling básico y contrato de respuesta API.

**Archivos/capas afectados:** `frontend/src/lib/api/client.ts`, `frontend/src/lib/api/types.ts`, `frontend/src/features/auth/auth.store.ts`, `frontend/src/features/auth/auth.api.ts`, `frontend/src/shared/errors/api-error.ts`

**Stack/librerías a usar:** Fetch API o Axios, Zustand, TanStack Query, TypeScript discriminated unions.

**Criterios de éxito medibles:**

- Requests agregan `Authorization`.
- 401 limpia sesión o redirige login.
- Errores backend se parsean consistentemente.
- Base URL viene de `.env`.
- Types evitan `any` en responses principales.

**💡 Concepto de Aprendizaje:** API Client Boundary; es como tener una caja registradora única para todas las ventas, no una distinta por empleado; importa porque evita duplicar manejo de token y errores en cada pantalla.

**Prompt listo para IA:** “Implementa el cliente HTTP del frontend AAC27. Debe leer `VITE_API_URL`, adjuntar JWT, manejar 401, parsear errores estándar del backend, definir tipos comunes y exponer helpers para queries/mutations con TanStack Query.”

#### T-25

**ID de tarea:** T-25

**Descripción técnica precisa:** Construir flujo de autenticación frontend: login, persistencia segura razonable, logout, protección de rutas y carga de perfil/roles.

**Archivos/capas afectados:** `frontend/src/features/auth/LoginPage.tsx`, `frontend/src/features/auth/auth.store.ts`, `frontend/src/features/auth/auth.api.ts`, `frontend/src/app/ProtectedRoute.tsx`, `frontend/src/layouts/MainLayout.tsx`

**Stack/librerías a usar:** React Hook Form, Zod, Zustand persist, React Router, TailwindCSS.

**Criterios de éxito medibles:**

- Login válido navega al dashboard.
- Login inválido muestra error.
- Logout borra token.
- Rutas privadas bloquean usuario anónimo.
- Roles quedan disponibles para ocultar navegación.

**💡 Concepto de Aprendizaje:** Client-Side Auth Guard; es como un recepcionista visual que oculta puertas no autorizadas, aunque la cerradura real sigue estando en backend; importa para UX, pero no reemplaza RBAC servidor.

**Prompt listo para IA:** “Construye el login del frontend AAC27 con React Hook Form + Zod, Zustand para sesión, rutas protegidas y logout. Conecta contra `/auth/login`, muestra errores del backend y prepara roles para navegación condicional.”

#### T-26

**ID de tarea:** T-26

**Descripción técnica precisa:** Implementar módulo Catálogo UI para categorías, subcategorías, materiales y productos con tablas, formularios, filtros y estados de carga/error.

**Archivos/capas afectados:** `frontend/src/features/catalogo/pages/*.tsx`, `frontend/src/features/catalogo/components/*.tsx`, `frontend/src/features/catalogo/catalogo.api.ts`, `frontend/src/features/catalogo/catalogo.schemas.ts`, `frontend/src/features/catalogo/catalogo.types.ts`

**Stack/librerías a usar:** TanStack Query, React Hook Form, Zod, TailwindCSS, React Router.

**Criterios de éxito medibles:**

- Listados consumen API paginada.
- Crear/editar invalida queries.
- Formularios validan antes de enviar.
- Errores backend se muestran.
- Productos muestran stock/disponibilidad.

**💡 Concepto de Aprendizaje:** Server State Management; es como consultar inventario real en bodega en vez de memorizarlo en una libreta local; importa porque TanStack Query evita datos stale después de compras/ventas.

**Prompt listo para IA:** “Implementa el módulo UI de Catálogo en AAC27. Crea páginas para listar y gestionar categorías, subcategorías, materiales y productos usando TanStack Query, formularios React Hook Form + Zod, filtros, paginación y manejo de errores.”

#### T-27

**ID de tarea:** T-27

**Descripción técnica precisa:** Implementar módulo Clientes y Proveedores UI con CRUD, búsqueda, formularios de contacto y validaciones.

**Archivos/capas afectados:** `frontend/src/features/clientes/*`, `frontend/src/features/proveedores/*`, `frontend/src/app/router.tsx`, `frontend/src/layouts/MainLayout.tsx`

**Stack/librerías a usar:** TanStack Query, React Hook Form, Zod, TailwindCSS, TypeScript.

**Criterios de éxito medibles:**

- Clientes se listan/crean/editan.
- Proveedores se listan/crean/editan.
- Búsqueda por nombre/documento funciona.
- Emails/teléfonos validan formato.
- Mutaciones refrescan tablas.

**💡 Concepto de Aprendizaje:** Reusable Form Patterns; es como usar el mismo formato de ficha para clientes y proveedores con campos específicos; importa porque reduce duplicación y errores en formularios repetitivos.

**Prompt listo para IA:** “Crea las pantallas de Clientes y Proveedores en el frontend AAC27. Implementa CRUD, búsqueda, formularios con React Hook Form + Zod, manejo de contactos, estados de carga/error y navegación desde el layout.”

#### T-28

**ID de tarea:** T-28

**Descripción técnica precisa:** Implementar flujo UI de Compras: seleccionar proveedor, agregar productos/detalles, método de pago/crédito, resumen y confirmación.

**Archivos/capas afectados:** `frontend/src/features/compras/pages/CompraCreatePage.tsx`, `frontend/src/features/compras/components/*.tsx`, `frontend/src/features/compras/compras.api.ts`, `frontend/src/features/compras/compras.schemas.ts`, `frontend/src/features/inventario/*`

**Stack/librerías a usar:** React Hook Form, Zod, TanStack Query mutations, TailwindCSS.

**Criterios de éxito medibles:**

- Usuario puede crear compra completa.
- Totales se muestran pero backend recalcula.
- Proveedor/productos se seleccionan desde API.
- Errores transaccionales se muestran.
- Inventario se actualiza tras éxito.

**💡 Concepto de Aprendizaje:** Optimistic vs Confirmed UI; es como anotar una compra en borrador hasta que caja confirma el pago; importa porque inventario no debe actualizarse visualmente como definitivo hasta respuesta del backend.

**Prompt listo para IA:** “Implementa el flujo de creación de compras en el frontend AAC27. Incluye selección de proveedor, productos, cantidades/costos, método de pago o crédito, resumen, envío al backend, manejo de errores y refresco de inventario.”

#### T-29

**ID de tarea:** T-29

**Descripción técnica precisa:** Implementar flujo UI de Ventas/POS básico con búsqueda de producto, carrito, cliente opcional, pagos, promociones y confirmación.

**Archivos/capas afectados:** `frontend/src/features/ventas/pages/POSPage.tsx`, `frontend/src/features/ventas/components/*.tsx`, `frontend/src/features/ventas/ventas.api.ts`, `frontend/src/features/ventas/ventas.store.ts`, `frontend/src/features/ventas/ventas.schemas.ts`

**Stack/librerías a usar:** Zustand para carrito local, TanStack Query, React Hook Form, Zod, TailwindCSS.

**Criterios de éxito medibles:**

- Búsqueda agrega productos al carrito.
- Cantidades respetan stock mostrado.
- Total/pagos se calculan en UI y backend confirma.
- Venta exitosa limpia carrito.
- Stock insuficiente muestra error 409.

**💡 Concepto de Aprendizaje:** Local UI State vs Server State; es como tener una bandeja temporal del vendedor antes de registrar la venta en caja; importa porque el carrito vive en frontend, pero stock real vive en backend.

**Prompt listo para IA:** “Construye un POS básico para AAC27. Implementa búsqueda de productos, carrito con Zustand, cliente opcional, promociones/pagos, validación con Zod, envío de venta al backend, manejo de stock insuficiente y limpieza del carrito al confirmar.”

#### T-30

**ID de tarea:** T-30

**Descripción técnica precisa:** Implementar pantallas operativas de Inventario, Apartados y Postventa con consultas, estados y acciones principales.

**Archivos/capas afectados:** `frontend/src/features/inventario/*`, `frontend/src/features/apartados/*`, `frontend/src/features/postventa/*`, `frontend/src/app/router.tsx`, `frontend/src/layouts/MainLayout.tsx`

**Stack/librerías a usar:** TanStack Query, React Hook Form, Zod, TailwindCSS, React Router.

**Criterios de éxito medibles:**

- Inventario muestra movimientos y bajo stock.
- Apartado permite crear/abonar/cancelar según API.
- Postventa permite registrar y consultar casos.
- Acciones muestran feedback.
- Rutas respetan roles.

**💡 Concepto de Aprendizaje:** Workflow UI; es como guiar al empleado paso a paso en procesos que tienen estados; importa porque apartados y postventa no son simples CRUD, son flujos operativos.

**Prompt listo para IA:** “Implementa pantallas operativas para Inventario, Apartados y Postventa en AAC27. Usa endpoints existentes, TanStack Query, formularios con Zod y acciones principales: movimientos/bajo stock, crear-abonar-cancelar apartados y registrar-consultar postventa.”

### Fase 4 — Calidad productiva

#### T-31

**ID de tarea:** T-31

**Descripción técnica precisa:** Separar pruebas unitarias e integración en Maven, habilitando ejecución controlada de Testcontainers en CI.

**Archivos/capas afectados:** `backend/pom.xml`, `backend/src/test/java/com/aac27/joyeria/**/*.java`, `.github/workflows/ci.yml`

**Stack/librerías a usar:** Maven Surefire, Maven Failsafe, JUnit 5, Testcontainers, GitHub Actions services/cache.

**Criterios de éxito medibles:**

- `mvn test` ejecuta unitarias.
- `mvn verify` ejecuta integración.
- Tests IT usan sufijo `IT`.
- CI distingue ambos pasos.
- Fallos de integración bloquean merge.

**💡 Concepto de Aprendizaje:** Test Pyramid; es como revisar joyas con lupa rápida primero y laboratorio después: no todo requiere el mismo costo; importa porque AAC27 necesita feedback rápido y confianza real en flujos críticos.

**Prompt listo para IA:** “Configura Maven en AAC27 para separar pruebas unitarias e integración. Usa Surefire para `*Test` y Failsafe para `*IT`, ajusta CI para ejecutar `mvn test` y `mvn verify`, y documenta cómo correr cada suite localmente.”

#### T-32

**ID de tarea:** T-32

**Descripción técnica precisa:** Añadir cobertura de pruebas y umbrales mínimos con JaCoCo para services y controllers críticos.

**Archivos/capas afectados:** `backend/pom.xml`, `backend/src/test/java/com/aac27/joyeria/module/**/*.java`, `.github/workflows/ci.yml`

**Stack/librerías a usar:** JaCoCo Maven Plugin, JUnit 5, Mockito, MockMvc.

**Criterios de éxito medibles:**

- Reporte HTML/XML generado.
- CI publica o conserva artifact de cobertura.
- Umbral inicial acordado no rompe por módulos vacíos.
- Auth/Catálogo/Compra/Venta tienen cobertura prioritaria.
- Build falla si baja del umbral.

**💡 Concepto de Aprendizaje:** Coverage as Signal; es como un mapa de zonas revisadas en inventario: no garantiza que todo esté perfecto, pero muestra huecos; importa para enfocar aprendizaje y evitar regresiones invisibles.

**Prompt listo para IA:** “Integra JaCoCo en el backend AAC27. Genera reportes de cobertura, define un umbral inicial realista, excluye solo código generado/configuración si corresponde, y ajusta CI para fallar cuando la cobertura baje.”

#### T-33

**ID de tarea:** T-33

**Descripción técnica precisa:** Implementar observabilidad básica: healthchecks útiles, métricas Actuator, logs estructurados y correlación por request.

**Archivos/capas afectados:** `backend/src/main/resources/application.yml`, `backend/src/main/resources/application-prod.yml`, `backend/src/main/java/com/aac27/joyeria/config/*.java`, `backend/src/main/java/com/aac27/joyeria/shared/**/*.java`

**Stack/librerías a usar:** Spring Boot Actuator, Micrometer, Logback, MDC, servlet filter.

**Criterios de éxito medibles:**

- `/actuator/health` reporta DB.
- Prod expone solo health/info/metrics necesarios.
- Cada request tiene correlation id en logs.
- Errores incluyen correlation id.
- Logs no imprimen secretos ni JWT completos.

**💡 Concepto de Aprendizaje:** Observability; es como cámaras y bitácora en una tienda: no evitan todos los problemas, pero permiten reconstruir qué pasó; importa porque producción exige diagnosticar fallos sin depurar en vivo.

**Prompt listo para IA:** “Agrega observabilidad básica al backend AAC27. Configura Actuator para dev/prod, health de base de datos, métricas, logs seguros y un correlation id por request usando MDC. Asegura que no se registren secretos.”

#### T-34

**ID de tarea:** T-34

**Descripción técnica precisa:** Endurecer configuración de seguridad productiva: CORS por ambiente, headers HTTP, políticas de contraseñas, rate limiting básico para login.

**Archivos/capas afectados:** `backend/src/main/java/com/aac27/joyeria/config/SecurityConfig.java`, `backend/src/main/java/com/aac27/joyeria/config/CorsConfig.java`, `backend/src/main/resources/application-prod.yml`, `backend/src/main/java/com/aac27/joyeria/module/seguridad/**/*.java`

**Stack/librerías a usar:** Spring Security, Bucket4j o filtro propio simple, Bean Validation, BCrypt.

**Criterios de éxito medibles:**

- CORS prod no permite `*`.
- Headers de seguridad activos.
- Contraseñas cumplen política mínima.
- Login limita intentos por IP/usuario.
- Pruebas cubren CORS y bloqueo de intentos excesivos.

**💡 Concepto de Aprendizaje:** Defense in Depth; es como usar vitrina, llave y cámara, no una sola protección; importa porque JWT no basta para proteger credenciales y endpoints sensibles.

**Prompt listo para IA:** “Endurece seguridad productiva en AAC27. Configura CORS por ambiente, headers HTTP seguros, política mínima de contraseñas y rate limiting básico para login. Agrega pruebas para CORS permitido/rechazado e intentos excesivos.”

#### T-35

**ID de tarea:** T-35

**Descripción técnica precisa:** Implementar backup/restore documentado para MySQL y estrategia de recuperación local.

**Archivos/capas afectados:** `scripts/backup-mysql.ps1`, `scripts/restore-mysql.ps1`, `docs/operacion-backups.md`, `docker-compose.yml`

**Stack/librerías a usar:** MySQL `mysqldump`, Docker Compose exec, PowerShell, documentación operativa.

**Criterios de éxito medibles:**

- Script genera dump con timestamp.
- Restore carga dump en base limpia.
- Documentación incluye frecuencia, retención y prueba de restore.
- No se versionan dumps reales.
- Comandos funcionan con compose dev.

**💡 Concepto de Aprendizaje:** Disaster Recovery; es como tener copia de llaves y póliza del inventario: el backup que no se prueba no existe; importa porque una joyería no puede perder ventas, stock ni cuentas por cobrar.

**Prompt listo para IA:** “Crea estrategia local de backup y restore para MySQL en AAC27. Agrega scripts PowerShell usando Docker Compose/mysqldump, documentación de operación, retención sugerida y prueba de restauración en base limpia.”

#### T-36

**ID de tarea:** T-36

**Descripción técnica precisa:** Añadir validación estática y formato para backend/frontend sin introducir complejidad excesiva.

**Archivos/capas afectados:** `backend/pom.xml`, `frontend/package.json`, `frontend/eslint.config.js`, `frontend/prettier.config.js`, `.github/workflows/ci.yml`

**Stack/librerías a usar:** Maven Checkstyle o Spotless, ESLint, Prettier, TypeScript `tsc`.

**Criterios de éxito medibles:**

- Backend tiene comando de formato/verificación.
- Frontend ejecuta `npm run lint` y `npm run typecheck`.
- CI ejecuta checks.
- Reglas son realistas para código existente.
- No se formatea masivamente sin necesidad.

**💡 Concepto de Aprendizaje:** Automated Code Quality Gates; es como una báscula calibrada en caja: evita discutir manualmente cada peso; importa para mantener consistencia mientras el proyecto crece.

**Prompt listo para IA:** “Agrega validación estática y formato a AAC27. Configura checks mínimos para backend y frontend, scripts npm de lint/typecheck, integración en CI y reglas pragmáticas que no generen ruido innecesario.”

#### T-37

**ID de tarea:** T-37

**Descripción técnica precisa:** Implementar auditoría funcional para operaciones sensibles: login, compras, ventas, cambios de stock, promociones y usuarios.

**Archivos/capas afectados:** `backend/src/main/java/com/aac27/joyeria/shared/audit/*.java`, `backend/src/main/java/com/aac27/joyeria/module/**/service/*.java`, `backend/src/main/resources/db/migration/V10__audit_log.sql`, `backend/src/main/java/com/aac27/joyeria/module/seguridad/entity/Usuario.java`

**Stack/librerías a usar:** Spring AOP o eventos de dominio, JPA, Flyway, Spring Security context.

**Criterios de éxito medibles:**

- Operaciones sensibles generan audit log.
- Audit incluye usuario, acción, entidad, id, fecha y resultado.
- Fallos críticos también quedan registrados.
- Endpoint de consulta solo admin.
- Tests validan al menos compra y venta.

**💡 Concepto de Aprendizaje:** Audit Trail; es como el libro de novedades de una tienda: no solo interesa el estado final, sino quién hizo qué y cuándo; importa por control interno y trazabilidad académica/productiva.

**Prompt listo para IA:** “Implementa auditoría funcional en AAC27 para operaciones sensibles. Crea migración `audit_log`, entidad/repository/service, registra usuario/acción/entidad/resultado usando AOP o eventos, agrega endpoint admin de consulta y pruebas básicas.”

#### T-38

**ID de tarea:** T-38

**Descripción técnica precisa:** Añadir pruebas e2e mínimas frontend-backend para login y flujo POS feliz usando ambiente local/CI.

**Archivos/capas afectados:** `frontend/playwright.config.ts`, `frontend/e2e/login.spec.ts`, `frontend/e2e/pos.spec.ts`, `frontend/package.json`, `.github/workflows/ci.yml`

**Stack/librerías a usar:** Playwright, Docker Compose, Vite preview, GitHub Actions.

**Criterios de éxito medibles:**

- Playwright ejecuta login exitoso.
- POS crea una venta con datos seed.
- Screenshots/videos se guardan solo en fallo.
- CI puede correr e2e en job separado.
- Pruebas tienen datos determinísticos.

**💡 Concepto de Aprendizaje:** End-to-End Testing; es como simular una venta real desde que el empleado entra al sistema hasta que cobra; importa porque valida integración visible, no solo código aislado.

**Prompt listo para IA:** “Agrega e2e mínimos a AAC27 con Playwright. Cubre login y flujo POS feliz contra backend/frontend levantados localmente o por CI. Configura scripts, datos seed determinísticos y artifacts solo en fallos.”

### Fase 5 — Release producción

#### T-39

**ID de tarea:** T-39

**Descripción técnica precisa:** Crear configuración productiva reproducible con `docker-compose.prod.yml`, variables obligatorias y perfiles seguros.

**Archivos/capas afectados:** `docker-compose.prod.yml`, `.env.prod.example`, `backend/src/main/resources/application-prod.yml`, `frontend/.env.production.example`

**Stack/librerías a usar:** Docker Compose, Spring profiles, Vite env vars, MySQL, Redis.

**Criterios de éxito medibles:**

- Compose prod levanta backend/frontend/db/redis.
- Secretos no quedan hardcodeados.
- App usa `SPRING_PROFILES_ACTIVE=prod`.
- Frontend consume `VITE_API_URL`.
- Variables faltantes fallan de forma explícita.

**💡 Concepto de Aprendizaje:** Environment Parity; es como ensayar la apertura de tienda con la misma distribución que producción; importa porque diferencias dev/prod causan fallos tardíos.

**Prompt listo para IA:** “Crea configuración productiva para AAC27 con `docker-compose.prod.yml`, `.env.prod.example` y variables obligatorias. Asegura perfiles Spring prod, conexión MySQL/Redis, frontend Vite apuntando al backend y sin secretos versionados.”

#### T-40

**ID de tarea:** T-40

**Descripción técnica precisa:** Configurar reverse proxy con TLS, compresión, routing `/api` al backend y frontend estático.

**Archivos/capas afectados:** `infra/nginx/nginx.conf`, `infra/nginx/conf.d/aac27.conf`, `docker-compose.prod.yml`, `frontend/Dockerfile`

**Stack/librerías a usar:** Nginx, TLS certificates, Docker Compose, Vite static build.

**Criterios de éxito medibles:**

- HTTPS sirve frontend.
- `/api` proxy al backend.
- Headers básicos de seguridad presentes.
- Compresión habilitada.
- Renovación/carga de certificados documentada.

**💡 Concepto de Aprendizaje:** Reverse Proxy; es como una recepción única que dirige clientes al mostrador correcto; importa porque producción no debe exponer directamente cada servicio interno.

**Prompt listo para IA:** “Agrega Nginx como reverse proxy de producción para AAC27. Sirve frontend estático, enruta `/api` al backend, configura TLS, compresión y headers básicos. Integra Nginx en `docker-compose.prod.yml` y documenta certificados.”

#### T-41

**ID de tarea:** T-41

**Descripción técnica precisa:** Preparar estrategia de inicialización productiva: usuario admin inicial, roles base, datos mínimos y rotación segura de credenciales.

**Archivos/capas afectados:** `backend/src/main/resources/db/migration/V1__seguridad_y_usuarios.sql`, nueva migración si aplica `backend/src/main/resources/db/migration/V11__production_seed_controls.sql`, `backend/src/main/java/com/aac27/joyeria/module/seguridad/**/*.java`, `docs/runbook-produccion.md`

**Stack/librerías a usar:** Flyway, Spring Boot startup runner opcional, BCrypt, MySQL.

**Criterios de éxito medibles:**

- Roles base existen.
- Admin inicial no usa contraseña hardcodeada insegura.
- Contraseña se fuerza a cambiar o se define por variable segura.
- Seed es idempotente.
- Runbook explica alta inicial.

**💡 Concepto de Aprendizaje:** Secure Bootstrapping; es como entregar la primera llave de la tienda sin dejar copias debajo del tapete; importa porque un sistema seguro puede quedar comprometido por un admin inicial mal gestionado.

**Prompt listo para IA:** “Diseña la inicialización productiva de AAC27. Asegura roles base, usuario admin inicial idempotente y sin contraseña insegura hardcodeada. Usa Flyway o startup runner, variables de entorno y documenta el proceso en el runbook.”

#### T-42

**ID de tarea:** T-42

**Descripción técnica precisa:** Crear runbook operativo para despliegue, backup, restore, actualización, rollback y diagnóstico básico.

**Archivos/capas afectados:** `docs/runbook-produccion.md`, `docs/operacion-backups.md`, `README.md`, `scripts/*.ps1`

**Stack/librerías a usar:** Markdown, Docker Compose CLI, MySQL dump/restore, GitHub Actions artifacts.

**Criterios de éxito medibles:**

- Incluye prerequisitos.
- Despliegue paso a paso.
- Rollback documentado.
- Backup/restore probado.
- Diagnóstico de logs/health incluido.
- Comandos son copiables.

**💡 Concepto de Aprendizaje:** Operational Runbook; es como el manual de apertura/cierre de tienda: reduce improvisación cuando algo falla; importa porque producción necesita operación repetible, no memoria del desarrollador.

**Prompt listo para IA:** “Crea el runbook productivo de AAC27. Documenta instalación, variables, despliegue con Docker Compose, backup, restore, actualización, rollback, healthchecks, logs y diagnóstico básico con comandos copiables.”

#### T-43

**ID de tarea:** T-43

**Descripción técnica precisa:** Implementar pipeline de release que construya imágenes versionadas, ejecute quality gates y publique artifacts o imágenes.

**Archivos/capas afectados:** `.github/workflows/release.yml`, `backend/pom.xml`, `frontend/package.json`, `docker-compose.prod.yml`

**Stack/librerías a usar:** GitHub Actions, Docker Buildx, Git tags, GitHub Container Registry, Maven, npm.

**Criterios de éxito medibles:**

- Release corre por tag.
- Ejecuta tests/lint/build.
- Construye imágenes backend/frontend.
- Etiqueta imágenes con versión y `latest` opcional.
- No publica si quality gates fallan.

**💡 Concepto de Aprendizaje:** Release Pipeline; es como una revisión final antes de poner joyas en vitrina: solo pasa lo que cumple control de calidad; importa para que producción sea trazable y repetible.

**Prompt listo para IA:** “Crea pipeline de release para AAC27 en GitHub Actions. Debe activarse por tag, correr quality gates, construir imágenes Docker backend/frontend con Buildx, versionarlas y publicarlas en GHCR o dejar artifacts listos.”

#### T-44

**ID de tarea:** T-44

**Descripción técnica precisa:** Definir checklist preproducción y pruebas de aceptación funcionales para tienda física.

**Archivos/capas afectados:** `docs/checklist-preproduccion.md`, `docs/criterios-aceptacion-negocio.md`, `frontend/e2e/*.spec.ts`, `backend/src/test/java/**/*.java`

**Stack/librerías a usar:** Markdown, Playwright, JUnit/Testcontainers.

**Criterios de éxito medibles:**

- Checklist cubre login, roles, catálogo, compras, ventas, apartados, postventa, backup y restore.
- Cada flujo tiene resultado esperado.
- Criterios pueden ejecutarse manualmente.
- Gaps quedan marcados.

**💡 Concepto de Aprendizaje:** Acceptance Testing; es como probar una jornada real de tienda antes de abrir al público; importa porque producción se valida por flujos de negocio, no solo por builds verdes.

**Prompt listo para IA:** “Crea checklist preproducción para AAC27 orientado a tienda física. Incluye pruebas manuales y automatizables para login, roles, catálogo, compras, ventas, apartados, postventa, backup/restore y criterios claros de aceptación.”

#### T-45

**ID de tarea:** T-45

**Descripción técnica precisa:** Preparar documentación académica/técnica final con arquitectura, decisiones, diagramas, pruebas y aprendizajes.

**Archivos/capas afectados:** `docs/arquitectura.md`, `docs/adr/*.md`, `docs/testing.md`, `README.md`, `REQUISITOS_Y_ARQUITECTURA_AAC27.md`

**Stack/librerías a usar:** Markdown, Mermaid diagrams, ADR templates, OpenAPI export.

**Criterios de éxito medibles:**

- README explica stack y ejecución.
- Arquitectura incluye diagrama de componentes.
- ADRs registran decisiones clave.
- Testing documenta pirámide y comandos.
- Documentación separa valor académico y productivo.

**💡 Concepto de Aprendizaje:** Architecture Decision Records; es como dejar constancia de por qué se eligió una vitrina, una caja fuerte o una distribución; importa porque un proyecto académico debe demostrar razonamiento, no solo código.

**Prompt listo para IA:** “Prepara documentación técnica/académica final de AAC27. Crea README actualizado, arquitectura con diagramas Mermaid, ADRs de decisiones clave, guía de testing y relación entre requisitos, módulos y entregables.”

#### T-46

**ID de tarea:** T-46

**Descripción técnica precisa:** Ejecutar ensayo de despliegue completo en ambiente limpio y registrar hallazgos/correcciones.

**Archivos/capas afectados:** `docs/reporte-ensayo-despliegue.md`, `docker-compose.prod.yml`, `.env.prod.example`, `docs/runbook-produccion.md`, scripts de backup/restore.

**Stack/librerías a usar:** Docker Compose, MySQL, Redis, Nginx, Spring Boot, React build.

**Criterios de éxito medibles:**

- Ambiente limpio levanta desde cero.
- Migraciones aplican.
- Admin inicial accede.
- Venta de prueba funciona.
- Backup/restore se ejecuta.
- Reporte lista tiempos, errores y acciones correctivas.

**💡 Concepto de Aprendizaje:** Production Rehearsal; es como hacer apertura simulada de tienda antes del primer día real; importa porque revela fallos de documentación, variables y dependencias que CI no ve.

**Prompt listo para IA:** “Guíame para ejecutar un ensayo de despliegue productivo de AAC27 en ambiente limpio. Verifica compose prod, migraciones, admin inicial, login, venta de prueba, backup/restore y genera un reporte de hallazgos con correcciones necesarias.”
