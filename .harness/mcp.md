# MCPs aprobados — AAC27

> Servidores MCP que aportan valor al stack. Credenciales **siempre** por variables de entorno,
> nunca en el repositorio (Art. 8). Config técnica en `.mcp.json` (cuando se añadan).

| MCP | Propósito | Rol que lo usa | Permisos | Estado |
|---|---|---|---|---|
| Navegador (Playwright/Chrome DevTools) | Validación e2e y verificación visual de la SPA (fase 7, foco e2e) | reviewer | Solo navegación local de dev | Propuesto (cuando exista frontend, spec 016+) |
| Documentación técnica (p. ej. context7) | Consultar APIs/versiones actuales de librerías (Art. 14) | planner, implementer | Solo lectura | Propuesto |
| Base de datos (MySQL) | Inspección de esquema en dev | reviewer | **Solo lectura**, nunca prod | Propuesto |
| Plataforma del repo (GitHub) | Issues/PRs | coordinator | Según token del usuario | Opcional |

## Notas
- Ninguno aprobado/instalado todavía: se añaden cuando la feature lo requiera y con aprobación (Art. 10).
- Revisar toda skill/MCP de terceros antes de instalar: se ejecutan con tus permisos (G4.5).
- Para headless/cron, los MCP autenticados interactivamente pueden no estar disponibles.
