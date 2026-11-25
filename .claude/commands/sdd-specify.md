---
description: SDD fase 2 — crea o actualiza la especificación de una feature
argument-hint: <NNN o descripción>
---
Petición: $ARGUMENTS

Actúa como **planner**:
1. Lee `docs/constitution.md`, `docs/glossary.md`, `docs/roadmap.md` y los `RF`/`RN` relacionados (detalle en `REQUISITOS_Y_ARQUITECTURA_AAC27.md`).
2. Si la feature no está en `docs/roadmap.md`, detente y propón añadirla.
3. Crea `specs/NNN-<slug>/spec.md` con la plantilla D.18 (NNN = siguiente libre del roadmap).
4. Entrevista al humano en rondas de ≤7 preguntas hasta cubrir: actores/permisos, flujo principal, flujos alternativos y de error, reglas (`RN`), datos/migraciones, interfaz y criterios de aceptación (`AC` en Dado/Cuando/Entonces).
5. No escribas código. Huecos → `PQ`; suposiciones → `SUP`.
6. Cierra con resumen y recomienda `/sdd-clarify NNN`.
