---
description: SDD fase 6 — implementa la siguiente tarea pendiente (o la indicada)
argument-hint: <NNN> [T-xx]
---
Objetivo: $ARGUMENTS

Actúa como **implementer** (requiere G-Plan). Para la tarea indicada (o la siguiente pendiente de `specs/NNN-*/tasks.md`):
1. Lee la tarea, sus `RF`/`AC` y los archivos del `plan.md`.
2. Test primero (si es viable) → confirma que falla → cambio mínimo → verde.
3. Verifica con el comando de la tarea + suite afectada (ver `AGENTS.md` → Comandos). Si no puedes ejecutar (`JAVA_HOME`), decláralo; no inventes verde.
4. Commit atómico `feat(scope): descripción [T-xx]` en la rama de la feature.
5. Marca la tarea en `tasks.md` con evidencia (hash + resultado del comando).
6. Una tarea por ciclo. Si hay que salir del plan → PARA y reporta (§A.7). Máx 3 ciclos de autocorrección.
