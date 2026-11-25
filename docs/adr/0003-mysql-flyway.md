# ADR-0003 — MySQL 8 + Flyway (migraciones versionadas inmutables)

- **Estado:** Aceptado (retroactivo)
- **Fecha:** 2026-11-25
- **Contexto:** Datos relacionales con fuertes invariantes (dinero `DECIMAL(14,2)`, FKs, constraints de stock, auditoría). Necesidad de esquema reproducible en dev/staging/prod (RNF-MAN06, RNF-INF01).
- **Opciones consideradas:**

| Opción | Pros | Contras | Encaje con RNF |
|---|---|---|---|
| MySQL + Flyway (elegida) | Relacional maduro; migraciones versionadas y auditables; `ddl-auto=validate` | Migraciones manuales | Cumple MAN06 |
| Hibernate `ddl-auto=update` | Rápido en dev | No reproducible, peligroso en prod | No apto |
| NoSQL | Flexibilidad | No encaja con transacciones/constraints de dinero | No apto |

- **Decisión:** MySQL 8 (InnoDB); esquema solo por Flyway en `resources/db/migration` (`V1…V10`). Una migración aplicada **nunca** se modifica; cambios → nueva migración. `ddl-auto=validate`.
- **Consecuencias:** + esquema reproducible y auditable; − disciplina de migraciones (no editar aplicadas); entidades JPA deben coincidir con el esquema (validación T-14).
