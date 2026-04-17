# MySQL 8 + Flyway - Gestión de Base de Datos
- Motor: InnoDB. Charset: `utf8mb4`. Collation: `utf8mb4_0900_ai_ci`
- IDs: `BIGINT UNSIGNED AUTO_INCREMENT` (excepto llaves compuestas)
- Moneda: `DECIMAL(14,2)`. NUNCA `FLOAT/DOUBLE`.
- Fechas: `DATETIME` con `DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`
- Auditoría: `created_at`, `updated_at` automáticos. Nunca actualizarlos manualmente.
- Índices: PK, UNIQUE, y secundarios por columnas de búsqueda frecuente.
- Flyway: scripts en `V1__init.sql`, `V2__...sql`. Nunca modificar migraciones aplicadas.
- Migrar tu SQL actual: conviértelo a `V1__init_schema.sql` y elimina `DROP TABLE IF EXISTS` o `INSERT` de datos demo (se manejarán con Data SQL o fixtures aparte).
- Concurrencia stock: `@Version` en JPA. Transacciones `REPEATABLE_READ`.