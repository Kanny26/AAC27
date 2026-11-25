---
description: SDD fase 4 — escribe el plan técnico de una spec aprobada
argument-hint: <NNN>
---
Spec: $ARGUMENTS

Actúa como **planner** (requiere spec aprobada en G-Spec):
1. Lee la spec, `docs/architecture/overview.md`, los ADRs y el código existente relevante.
2. Escribe `specs/NNN-*/plan.md` (plantilla D.20): enfoque técnico y alternativas descartadas; archivos exactos a crear/modificar; cambios de datos y **migraciones Flyway** (nueva versión + reversión); contratos API; componentes UI; estrategia de pruebas mapeada a cada `AC`; dependencias nuevas justificadas (Art. 10); riesgos; ADRs nuevos/afectados.
3. Coherencia con los ADRs; si hay decisión de arquitectura nueva, crea un ADR (`/adr`).
4. No escribas código. Recomienda `/sdd-tasks NNN`.
