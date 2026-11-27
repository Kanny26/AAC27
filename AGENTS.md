# AGENTS.md — AAC27

> Código operativo para cualquier agente de IA. Conciso por diseño: ante dudas, consulta los
> documentos enlazados. La Ley Suprema completa está en `docs/governance/GENESIS.md`.

## Proyecto
Sistema de gestión para una joyería física single-tenant (COP). Backend Spring Boot (Java 21) +
frontend React (Vite). Doble propósito: producción real y aprendizaje fullstack. Brownfield con
seguridad y catálogo ya implementados; se adopta Spec-Driven Development hacia adelante.

## Ley Suprema (resumen de docs/governance/GENESIS.md)
1. Sin spec aprobada no hay código de producción (hacia adelante). Plan antes que Build.
2. No inventes: pregunta, o registra `SUP`/`PQ`. Nunca inventes reglas de negocio ni endpoints.
3. Trazabilidad: todo con ID (`RF`/`RNF`/`RN`/`AC`/`T`/`ADR`); cada `AC` con su test.
4. Verifica antes de declarar; aporta evidencia (o declara que no pudiste ejecutar).
5. Cambios mínimos; una tarea, un commit.
6. Seguridad por defecto; secretos nunca en el repo (usa env + `.env.example`).
7. Acciones destructivas o con coste → confirmación humana (Art. 9).
8. Máximo 3 ciclos de autocorrección por tarea; después, escala.
9. Spec-Anchored: si cambia el comportamiento observable, primero la spec.
10. El contexto viaja por archivos; actualiza `MEMORY.md`.

## Orden de lectura al iniciar sesión
`AGENTS.md` → `MEMORY.md` → spec activa (`specs/NNN-*/`) → documentos que esa spec referencie.

## Mapa de documentos
| Necesito… | Leo… |
|---|---|
| Principios innegociables | `docs/constitution.md` |
| Vocabulario y nombres | `docs/glossary.md` |
| Qué hace el sistema | `docs/requirements/` (+ `REQUISITOS_Y_ARQUITECTURA_AAC27.md` para el detalle) |
| Reglas de negocio | `docs/requirements/business-rules.md` |
| Permisos por rol | `docs/requirements/roles-permissions.md` + `docs/matriz-permisos.md` (operativo) |
| Modelo de datos / estados | `docs/domain/model.md` (esquema real = migraciones Flyway) |
| Por qué está construido así | `docs/adr/` |
| Qué sigue | `docs/roadmap.md` (specs NNN ↔ tareas T-xx de `ROADMAP_AAC27.md`) |
| Reglas completas del agente | `docs/governance/GENESIS.md` |

## Stack y versiones
Backend: Spring Boot 3.2.5 · Java 21 · Spring Security + JWT (JJWT) · Spring Data JPA · Flyway ·
MapStruct · Lombok · MySQL 8 · Redis. Frontend: React 18 · Vite · TS · Tailwind · TanStack Query ·
Zustand · React Hook Form · Zod. Paquete base `com.aac27.joyeria`, estructura `module/{...}`.

## Comandos del proyecto
| Acción | Comando |
|---|---|
| Tests backend (todos) | `cd backend && ./mvnw test` |
| Un test | `cd backend && ./mvnw test -Dtest=ClaseTest` |
| Build backend | `cd backend && ./mvnw -DskipTests package` |
| Frontend dev / build | `cd frontend && npm run dev` / `npm run build` |
| Infra local | `docker compose up -d mysql redis` |
> ⚠️ Entorno actual sin `JAVA_HOME` configurado: Maven puede no ejecutar. Si no puedes correr la
> suite, **decláralo** en el informe; nunca afirmes "verde" sin evidencia (Art. 5).

## Estructura del repositorio
`backend/` (Spring Boot, paquete `com.aac27.joyeria`: `config/`, `shared/`, `module/{seguridad,`
`catalogo,compra,venta,postventa,proveedor}`) · `frontend/` (shell Vite, sin páginas) ·
`docs/` (gobernanza, constitución, requisitos, dominio, ADRs, roadmap, trazabilidad) ·
`specs/NNN-slug/` (spec, clarifications, plan, tasks, validation) · `.claude/` (agentes, comandos, skills).

## Convenciones de código
- Dinero `BigDecimal`/`DECIMAL(14,2)`, **nunca** float/double. BCrypt≥12. `@Enumerated(STRING)`.
- DTOs separados + MapStruct; respuestas `ApiResponse<T>`; `/api/v1/...`; paginación server-side (máx 100).
- Migraciones: nueva `V{n}__desc.sql`, **nunca** editar una aplicada. `ddl-auto=validate`.
- Comentarios en español; identificadores y commits en inglés. Conventional Commits con ID de tarea.
- Git: rama por feature (`feat/NNN-slug`), PR a `develop`. Sin commits a principal ni force-push ni `--no-verify`.

## Agentes y comandos
Agentes (`.claude/agents/`): **coordinator** (orquesta, no codea), **planner** (specs/planes/ADRs),
**implementer** (código+tests+commits), **reviewer** (clarifica y valida, no corrige).
Comandos (`.claude/commands/`): `/feature` (orquestador), `/sdd-specify`, `/sdd-clarify`, `/sdd-plan`,
`/sdd-tasks`, `/sdd-implement`, `/sdd-validate`, `/sdd-status`, `/adr`, `/bug`, `/memory`.
Puertas humanas: G-Spec, G-Plan, G-Merge (GENESIS §A.5). Detente en cada una.

## Verificación obligatoria tras cada cambio
Lint + tests afectados; en cambios de permisos actualizar `docs/matriz-permisos.md` y `RbacMatrizTest`;
en cambios de UI, verificación con MCP de navegador (cuando exista frontend).

## Prohibido
Secretos en el repo; editar migraciones aplicadas; exponer entidades JPA; float/double para dinero;
saltarse puertas; afirmar "hecho" sin evidencia; refactors no pedidos.
