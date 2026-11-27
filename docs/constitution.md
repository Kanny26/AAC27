# Constitución — AAC27

Versión: 1.0.0 · Ratificada: 2026-11-25 · Subordinada a: `docs/governance/GENESIS.md`

> Destilada de `.claude/CLAUDE.md` y `REQUISITOS_Y_ARQUITECTURA_AAC27.md`. Son los principios
> innegociables del proyecto. Enmendable solo con aprobación humana (GENESIS §A.10).

## Preámbulo

AAC27 es un sistema de gestión para una joyería física single-tenant (Colombia, COP). Administra
seguridad, catálogo, proveedores, compras, inventario, ventas, apartados, promociones, pagos y
postventa. Tiene un doble propósito: producto real de producción y herramienta de aprendizaje
fullstack para el desarrollador.

## Título I — Principios de producto (priorizados)
1. **La exactitud de datos financieros y de inventario prevalece sobre la velocidad de entrega.**
2. **Trazabilidad y auditoría sobre conveniencia:** toda acción crítica queda registrada e inmutable.
3. **Seguridad por defecto:** mínimo privilegio, validación de toda entrada, secretos fuera del repo.
4. **Mantenibilidad sobre atajos:** separación de capas y contratos estables aunque cuesten más.

## Título II — Principios técnicos
- **Arquitectura:** monolito modular por dominio (`module/{seguridad,catalogo,compra,venta,postventa,proveedor}`). Capas estrictas Controller → Service → Repository → DTO. Ver ADR-0001.
- **Stack:** Spring Boot 3.2.5 · Java 21 · Spring Security + JWT · Spring Data JPA/Hibernate · Flyway · MapStruct · Lombok · MySQL 8 · Redis (cache/refresh). Frontend: React 18 · Vite · TS · Tailwind · TanStack Query · Zustand · RHF · Zod. Ver ADR-0002..0005.
- **Testing:** pirámide — unitarios (JUnit 5 + Mockito) mayoría, integración (Testcontainers/MySQL) en flujos críticos, e2e (Playwright) sobre caminos felices. Objetivo RNF-MAN01: ≥80% en services.
- **Errores y logging:** `@ControllerAdvice` + `ApiResponse<T>` estandarizado. Logs sin secretos ni JWT completos; JSON en prod (RNF-MAN02/05).

## Título III — Reglas innegociables
- **Dinero:** `DECIMAL(14,2)` / `BigDecimal` siempre. **Nunca** `float`/`double`.
- **Contraseñas:** BCrypt cost ≥ 12. Nunca en texto plano ni en logs.
- **JWT:** access 8h, refresh 7d (Redis/httpOnly cookie). Stateless.
- **Auditoría:** tabla `auditoria_log` inmutable; logs nunca editables ni borrables.
- **Migraciones:** solo Flyway en `resources/db/migration`. **Nunca** modificar una migración aplicada.
- **Concurrencia de stock:** `@Version` (optimistic) y/o locking + `REPEATABLE_READ`. Stock nunca negativo.
- **Paginación server-side** obligatoria en todo listado (page/size/sort, máx 100).
- **DTOs** separados de entidades JPA; mapeo con MapStruct. Las entidades no se exponen en la API.
- **Secretos** vía variables de entorno y `.env.example`. Nunca datos reales de producción en dev/test.

## Título IV — Convenciones
- **Idiomas:** documentos y comentarios en español; identificadores de código y commits en inglés. Los nombres de dominio ya fijados en español en la BD se conservan.
- **API:** `/api/v1/...`, recursos en plural/kebab-case; respuesta `{ success, message, data, errors, timestamp, pagination }`; códigos 200/201/204/400/401/403/404/409/422/500.
- **Git:** rama principal protegida; una rama por feature (`feat/NNN-slug`, `fix/…`, `chore/…`); Conventional Commits con ID de tarea; se mergea a `develop` vía PR. Sin commits directos a principal ni force-push.
- **Formatos:** fecha `dd/mm/yyyy`, moneda COP con separador de miles, zona horaria America/Bogota.

## Título V — Calidad
Definition of Ready y Definition of Done según GENESIS §C.5–C.6. Además: cada `RF`/`RN` tocado se
refleja en `docs/traceability.md`; cada cambio de permisos actualiza `docs/matriz-permisos.md` y
`RbacMatrizTest`.

## Título VI — Colaboración con agentes
Modo mentor: **activo** (explicar el porqué en 1–3 frases y la alternativa descartada). Paradas
obligatorias de GENESIS §A.7. Sin spec aprobada no hay código de producción (hacia adelante).

## Registro de enmiendas
| Versión | Fecha | Cambio | Motivo | Aprobado por |
|---|---|---|---|---|
| 1.0.0 | 2026-11-25 | Versión inicial destilada de CLAUDE.md + REQUISITOS | Adopción SDD brownfield | Humano |
| 1.0.1 | 2026-11-26 | Fuente del arnés en `.claude/` (desviación de G4.7) | Evitar deriva entre dos copias | Humano |