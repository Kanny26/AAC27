---
description: SDD fase 7 — valida la feature con el foco indicado y escribe validation.md
argument-hint: <NNN> <cumplimiento|e2e|calidad-seguridad>
---
Objetivo: $ARGUMENTS

Actúa como **reviewer** en modo Validación con el foco indicado:
- **cumplimiento:** Constitución, spec, `RN`, glosario, trazabilidad `AC`→test.
- **e2e:** flujos en navegador real (MCP); consola/red/estados/responsive/accesibilidad; capturas.
- **calidad-seguridad:** tests, cobertura, linters, OWASP, secretos, dependencias, rendimiento vs `RNF`; RBAC vs `docs/matriz-permisos.md` y `RbacMatrizTest`.

Ejecuta de verdad; adjunta evidencia. Si no puedes ejecutar algo, decláralo como limitación (no como aprobado). Escribe/actualiza `specs/NNN-*/validation.md` (D.22), clasifica hallazgos (BLOQUEANTE|MAYOR|MENOR|SUGERENCIA, archivo:línea) y da veredicto. APROBADO solo sin bloqueantes ni mayores. No corrijas código.
