# MEMORY — AAC27  (≤ 50 líneas · actualizado: 2026-11-25)

## Estado actual
- Etapa: adopción SDD brownfield (Génesis retroactivo). Arnés recién montado.
- Feature activa: ninguna en ciclo. Próxima: spec `003-proveedor` (CRUD, T-10, RF09/RF10/RF11-E/RF-P01).
- Rama: `fix/t07-cierre` (commit `ae29d04` cerró T-07; pendiente PR → `develop`).
- Última tarea completada: Etapa 2 del plan (arnés: agentes, comandos, AGENTS.md, este archivo).
- Siguiente paso: aprobación humana G-Arnés → cerrar baseline Fase 0 (spec 000) → `/feature proveedor`.

## Decisiones recientes
- Adopción SDD: A-A-A-A (híbrido · subagentes reales · fases ROADMAP→specs NNN · cerrar T-07 primero). Plan: `C:\Users\hervi\.claude\plans\roadmap-...corbato.md`.
- Fuente única del arnés en `.claude/` (no duplicar en `.harness/`) para evitar deriva — ver `.harness/README.md`.
- Requisitos troceados en `docs/requirements/` conservando IDs; `REQUISITOS_Y_ARQUITECTURA_AAC27.md` queda como detalle.

## Aprendizajes y trampas conocidas
- Entorno sin `JAVA_HOME`: no se puede correr `./mvnw` aquí; verificar por inspección y declararlo (Art. 5).
- Se mergea a `develop` vía PR, NO a `main` (local `main` va atrasado). Commits de equipo: Kanny26; git user local: Stephany.
- `superadministrador` NO tiene permisos de negocio (sin jerarquía, a propósito) — `docs/matriz-permisos.md`.
- Este `MEMORY.md` (repo) ≠ auto-memoria del asistente en `~/.claude/...`.

## Bloqueos y preguntas abiertas activas
- DT-01/D1: `GET /ventas/cliente/{id}` sin filtro por vendedor (RF17 vs comentario). Decidir filtrar o excepción.
- DT-02/D2: apartados sin filtro por vendedor. Confirmar regla.
- DT-03/D3: Swagger/`v3/api-docs` público en prod. Cerrar en T-12/T-34.
- Módulos compra/venta/postventa parciales (DTOs/mappers) — DT-04.
