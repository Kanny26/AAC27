# Roadmap — AAC27 (reconciliado SDD)

> Reconcilia `ROADMAP_AAC27.md` (46 tareas T-01…T-46 en Fases 0–5) con el modelo de specs de GENESIS.
> **Cada spec `NNN` es una rebanada vertical**; las tareas `T-xx` del ROADMAP se conservan como
> tareas dentro del `tasks.md` de su spec. `ROADMAP_AAC27.md` sigue siendo la vista detallada con los
> "conceptos de aprendizaje" y prompts. Este archivo es el índice de ejecución SDD.

Estados: `Pendiente` · `Implementada (baseline)` = ya existe en el código, se documenta · `En curso` · `Hecha`.

| Spec | Feature | Fase ROADMAP | Tareas T-xx | RF cubiertos | Depende de | Estado |
|---|---|---|---|---|---|---|
| 000 | Base verificable (walking skeleton brownfield) | 0 | T-01…T-06 | — (infra) | — | Pendiente (cerrar) |
| 001 | Seguridad y Auth (JWT, RBAC, password) | 1 | T-07, T-08 | RF01–RF08, RF-S01/02 | 000 | Implementada (baseline) |
| 002 | Contrato de errores API | 1 | T-09 | RNF-MAN04/05 | 000 | Parcial (baseline) |
| 003 | Proveedor CRUD | 1 | T-10 | RF09, RF10, RF11-E, RF-P01 | 001 | **Siguiente (primera feature SDD)** |
| 004 | Catálogo: contrato REST + paginación | 1 | T-11 | RF12–RF14, RF-CAT01 | 001 | Parcial (baseline) |
| 005 | OpenAPI/Swagger | 1 | T-12 | RNF-MAN03 | 003, 004 | Pendiente |
| 006 | Suite de pruebas services core | 1 | T-13 | RNF-MAN01 | 001, 003, 004 | Pendiente |
| 007 | Validación entidad↔migración | 1 | T-14 | RNF-REN06 | 000 | Pendiente |
| 008 | Modelo de inventario (ledger) | 2 | T-15 | RF15, RF16 | 004 | Parcial (baseline) |
| 009 | Compras transaccional | 2 | T-16 | RF11, RF-C01/02/03 | 003, 008 | Parcial (baseline) |
| 010 | Ventas transaccional | 2 | T-17 | RF17–RF21, RF-V02/03 | 008 | Parcial (baseline) |
| 011 | Concurrencia de stock | 2 | T-18 | RNF-REN04 | 009, 010 | Parcial (baseline) |
| 012 | Apartados | 2 | T-19 | RF-V01 | 010 | Parcial (baseline) |
| 013 | Promociones | 2 | T-20 | RF-V02 | 010 | Parcial (baseline) |
| 014 | Consultas de inventario (kardex) | 2 | T-21 | RF16, RF-INV01 | 008 | Pendiente |
| 015 | Integración compra→venta (Testcontainers) | 2 | T-22 | RNF-MAN01 | 009, 010 | Pendiente |
| 016 | Frontend: arquitectura base | 3 | T-23 | — | 000 | Pendiente |
| 017 | Frontend: cliente HTTP + auth | 3 | T-24, T-25 | RF02, RF04 | 016, 001 | Pendiente |
| 018 | Frontend: Catálogo | 3 | T-26 | RF-CAT01 | 017, 004 | Pendiente |
| 019 | Frontend: Clientes y Proveedores | 3 | T-27 | RF-U02, RF09 | 017, 003 | Pendiente |
| 020 | Frontend: Compras | 3 | T-28 | RF11 | 017, 009 | Pendiente |
| 021 | Frontend: POS/Ventas | 3 | T-29 | RF20 | 017, 010 | Pendiente |
| 022 | Frontend: Inventario/Apartados/Postventa | 3 | T-30 | RF16, RF-V01, RF24 | 017, 012 | Pendiente |
| 023 | Separar unit/integration (Maven) | 4 | T-31 | RNF-MAN01 | 015 | Pendiente |
| 024 | Cobertura JaCoCo | 4 | T-32 | RNF-MAN01 | 006 | Pendiente |
| 025 | Observabilidad (health, logs, correlation id) | 4 | T-33 | RNF-MAN02/07 | 000 | Pendiente |
| 026 | Seguridad productiva (CORS/headers/rate limit) | 4 | T-34 | RNF-SEG04/06/07 | 001 | Pendiente |
| 027 | Backup/restore MySQL | 4 | T-35 | RNF-INF04 | 000 | Pendiente |
| 028 | Lint/format back+front | 4 | T-36 | RNF-MAN | 000 | Pendiente |
| 029 | Auditoría funcional | 4 | T-37 | RF-S01 | 001 | Pendiente |
| 030 | e2e Playwright (login + POS) | 4 | T-38 | RNF-UX | 021 | Pendiente |
| 031 | Config productiva (compose.prod) | 5 | T-39 | RNF-INF01/02 | 000 | Pendiente |
| 032 | Nginx TLS reverse proxy | 5 | T-40 | RNF-INF05, RNF-SEG06 | 031 | Pendiente |
| 033 | Bootstrap seguro (admin inicial) | 5 | T-41 | RF06, RNF-SEG | 001 | Pendiente |
| 034 | Runbook operativo | 5 | T-42 | RNF-INF06 | 031 | Pendiente |
| 035 | Pipeline de release | 5 | T-43 | RNF-INF03 | 023, 024 | Pendiente |
| 036 | Checklist preproducción | 5 | T-44 | — | 030 | Pendiente |
| 037 | Documentación final (ADRs/diagramas) | 5 | T-45 | — | todas | Pendiente |
| 038 | Ensayo de despliegue | 5 | T-46 | RNF-INF06 | 031, 032, 033 | Pendiente |

> **Nota de reconciliación:** la numeración `NNN` es la de ejecución SDD; el mapeo T-xx→spec es
> orientativo y puede agruparse/dividirse al especificar. Lo "Implementada/Parcial (baseline)" existe
> en el código y se documenta como línea base; se le hará spec solo si se vuelve a tocar (Spec-Anchored).

## Deuda técnica
| ID | Descripción | Impacto | Propuesta | Estado |
|---|---|---|---|---|
| DT-01 | `GET /ventas/cliente/{id}` sin filtro por vendedor (D1 matriz) | Vendedor ve ventas ajenas | Decidir filtrar o excepción documentada | Abierta |
| DT-02 | Apartados sin filtro por vendedor (D2 matriz) | Visibilidad amplia | Confirmar regla deseada | Abierta |
| DT-03 | Swagger/`v3/api-docs` público en prod (D3) | Exposición de contrato | Cerrar en perfil prod (T-12/T-34) | Abierta |
| DT-04 | Módulos compra/venta/postventa parciales (DTOs/mappers incompletos) | Contratos inestables | Completar en specs 009–013 | Abierta |
