# MEMORY — AAC27  (≤ 50 líneas · actualizado: 2026-11-26)

## Estado actual
- Etapa: adopción GENESIS brownfield. Arnés (`.claude/`) y docs fundacionales ya en `develop` (PR #12).
- Feature activa: ninguna en ciclo. Siguiente: PR B (D1) y luego spec `002-contrato-errores` (T-09).
- Rama base: `develop` (`2ff3586`). `main` está obsoleta; no mergear a ella.
- Última tarea completada: cierre de adopción GENESIS (PR #12 + `docs/genesis-closeout`).
- Siguiente paso: PR B (D1, RF17/RF18) → `/feature` spec 002 (errores 404/405/415 hoy devuelven 500).

## Decisiones recientes
- Fuente única del arnés en `.claude/` (desviación de G4.7, enmienda 1.0.1 de la constitución).
- D1: el vendedor ve solo sus propias ventas, también por cliente (RF17/RF18) → PR B.
- D2: el vendedor ve apartados de otros y esto se respeta (sin filtro por vendedor).
- D3: Swagger cerrado en prod (`denyAll` + springdoc off) → se implementa en T-12.
- Orden acordado: PR B, spec 002 (errores), luego spec 003 (Proveedor).
- Commits en inglés, Conventional Commits con ID de tarea.

## Aprendizajes y trampas conocidas
- Sin `JAVA_HOME` local ni Docker en Git Bash: la verificación real es el CI de GitHub (declararlo, Art. 5).
- Se mergea a `develop` vía PR, no a `main`. Commits de equipo: Kanny26; git user local: Stephany.
- `superadministrador` no tiene permisos de negocio (sin jerarquía, a propósito) — `docs/matriz-permisos.md`.
- Tests van en `src/test`, código en `src/main`; un CI viejo re-ejecutado usa el commit antiguo.
- Este `MEMORY.md` (repo) ≠ auto-memoria del asistente.

## Bloqueos y preguntas abiertas activas
- Módulos compra/venta/postventa parciales (DT-04).
- Pendientes de cierre de T-07 diferidos a propósito: logout con Redis (RF04), política de password configurable (T-34), MAIL_* en prod (T-39/T-42).