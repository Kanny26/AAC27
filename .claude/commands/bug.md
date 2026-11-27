---
description: Corrige un bug reproduciéndolo con un test que falla (GENESIS §C.7)
argument-hint: <descripción del bug>
---
Bug: $ARGUMENTS

Actúa como **coordinator** y coordina el flujo de bug:
1. **reviewer/implementer:** reproduce con un test que falle (materializa el comportamiento incorrecto).
2. Si el bug revela un hueco de la spec, **primero** enmienda la spec (Art. 13, Spec-Anchored) con aprobación humana.
3. **implementer:** corrige con el cambio mínimo → test en verde → commit `fix(scope): descripción`.
4. Actualiza `docs/traceability.md` y `MEMORY.md` (aprendizaje si aplica).
No improvises correcciones sin test que falle primero.
