# ADR-0004 — Concurrencia de stock (optimistic `@Version` + locking)

- **Estado:** Aceptado (retroactivo, en consolidación)
- **Fecha:** 2026-11-25
- **Contexto:** Stock de joyería suele ser bajo y unitario. Ventas/compras/apartados concurrentes no pueden dejar stock negativo (RN-002, RNF-REN04). Existe `ProductoLockRepository` en `module/compra`.
- **Opciones consideradas:**

| Opción | Pros | Contras | Encaje con RNF |
|---|---|---|---|
| Optimistic `@Version` | Sin bloqueo; buen rendimiento en baja contención | Reintentos ante conflicto | Bien para la mayoría |
| Pessimistic write lock (`SELECT FOR UPDATE`) | Garantiza exclusividad en el ítem caliente | Menos throughput; riesgo de deadlock | Bien para ítems únicos |
| Sin control | Simple | Stock negativo / overselling | Inaceptable |

- **Decisión:** Combinación: `@Version` (optimistic) en `Producto` + lock pesimista (`ProductoLockRepository`, `PESSIMISTIC_WRITE`) en las secciones críticas que modifican stock, bajo transacción `REPEATABLE_READ`. Pruebas concurrentes lo validan (T-18).
- **Consecuencias:** + stock nunca negativo; − complejidad transaccional y manejo de conflictos/timeout; requiere test concurrente real (Testcontainers) como evidencia.
