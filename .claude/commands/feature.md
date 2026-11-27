---
description: Orquesta el ciclo SDD completo de una feature con puertas humanas
argument-hint: <descripción de la feature>
---
Feature: $ARGUMENTS

Actúa como **coordinator** y ejecuta GENESIS Parte C de principio a fin, deteniéndote en cada 🚦:

1. **Verificación constitucional** → encaja en `docs/roadmap.md`, asigna `NNN` y rama `feat/NNN-slug`.
2. Delega al subagente **planner** `/sdd-specify` → al subagente **reviewer** `/sdd-clarify` → planner actualiza la spec → 🚦 **G-Spec**.
3. Planner `/sdd-plan` y `/sdd-tasks` → 🚦 **G-Plan**.
4. Subagente **implementer** `/sdd-implement` tarea a tarea (las `[P]` pueden ir en paralelo si no comparten archivos).
5. Reviewer ×3 en paralelo `/sdd-validate` (cumplimiento | e2e | calidad-seguridad) → consolidar `validation.md`.
6. CAMBIOS → volver a 4 (máx. 3 ciclos). APROBADO → 🚦 **G-Merge**.
7. Actualiza `docs/traceability.md`, `docs/roadmap.md` y `MEMORY.md`; haz la retrospectiva (§C.9).

Detente en cada 🚦 hasta recibir aprobación humana explícita.
