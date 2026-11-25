# ADR-0001 — Monolito modular por dominio

- **Estado:** Aceptado (retroactivo)
- **Fecha:** 2026-11-25 (decisión original previa; documentada ahora)
- **Contexto:** Single-tenant, un equipo pequeño, doble propósito (producción + aprendizaje). RNF de rendimiento moderados (p95<300ms, ≤50 concurrentes). Se necesita simplicidad operativa y despliegue sencillo con Docker.
- **Opciones consideradas:**

| Opción | Pros | Contras | Encaje con RNF |
|---|---|---|---|
| Monolito modular (elegida) | Simple de desplegar/operar; transacciones locales (stock/dinero); fácil de aprender | Escala vertical; acoplamiento si no se cuida | Suficiente para ≤50 usuarios |
| Microservicios | Escala independiente | Complejidad de red, transacciones distribuidas, sobreingeniería para 1 tienda | Excesivo |

- **Decisión:** Monolito Spring Boot con módulos por dominio (`module/{seguridad,catalogo,compra,venta,postventa,proveedor}`) y capas estrictas Controller→Service→Repository→DTO.
- **Consecuencias:** + simplicidad y consistencia transaccional; − disciplina de módulos necesaria para no acoplar; se reconsideraría si el volumen multi-tienda creciera (fuera de alcance v1).
