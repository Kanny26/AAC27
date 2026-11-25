# .harness/ — Arnés SDD de AAC27 (fuente lógica)

Este directorio es el **hogar canónico** del arnés definido en `docs/governance/GENESIS.md` §G4.
La herramienta anfitriona es **Claude Code**, cuyas rutas nativas son `.claude/`.

## Decisión de fuente única (desviación consciente de G4.7)
GENESIS G4.7 plantea `.harness/` como fuente y copiar a las rutas nativas. Para **evitar la deriva
entre dos copias mantenidas a mano**, en este proyecto la fuente operativa vive directamente en
`.claude/` (que es de donde Claude Code lee), y `.harness/` documenta el mapeo y la gobernanza.
Si en el futuro se añade otra herramienta (OpenCode, Cursor…), se materializan copias desde aquí.

## Mapeo de piezas (instaladas en Claude Code)
| Pieza | Ubicación funcional | Contenido |
|---|---|---|
| Instrucciones raíz | `CLAUDE.md` (importa `@AGENTS.md`) + `AGENTS.md` | Código operativo |
| Agentes | `.claude/agents/{coordinator,planner,implementer,reviewer}.md` | Los 4 roles SDD |
| Comandos | `.claude/commands/{feature,sdd-specify,sdd-clarify,sdd-plan,sdd-tasks,sdd-implement,sdd-validate,sdd-status,adr,bug,memory}.md` | Flujo SDD |
| Skills | `.claude/skills/{spring-boot,mysql-flyway,react-vite,docker-compose}.md` | Conocimiento determinista existente |
| MCP | `.mcp.json` + `.harness/mcp.md` | Servidores MCP aprobados |

## Skills candidatas (pendientes, proponer antes de crear — G4.5)
- `dinero-cop` — redondeo/formato COP, `BigDecimal`, prohibición de float (RN-001).
- `concurrencia-stock` — patrón `@Version` + lock pesimista para stock (RN-002/004, ADR-0004).
- `api-response` — envoltura `ApiResponse<T>` y códigos HTTP del proyecto.
- `testing-aac27` — convenciones JUnit/Mockito/MockMvc/Testcontainers y `*Test` vs `*IT`.

## Cómo regenerar / verificar
- Agentes y comandos cargan automáticamente al abrir Claude Code en la raíz del repo.
- `/sdd-status` resume el estado SDD. Los 4 agentes se invocan por nombre o vía `/feature`.
