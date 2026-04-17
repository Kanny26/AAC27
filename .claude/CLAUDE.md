# 🏛️ Contexto del Proyecto: Joyería AAC27

## 🎯 Objetivo
Sistema de gestión de joyería single-tenant para tienda física. Moneda base: COP. Roles: Administrador, Vendedor, Superadministrador.

## 🛠️ Stack Tecnológico
- Backend: Spring Boot 3.2+ (Java 21), Spring Security, JWT, JPA/Hibernate, Flyway, MapStruct
- Frontend: React 18, Vite, TypeScript, React Query, Zustand, React Hook Form + Zod, React Router v6
- BD: MySQL 8.0+
- Infra: Docker & Docker Compose (dev local), Redis (cache/refresh tokens)
- Moneda: DECIMAL(14,2) para todo campo monetario. Nunca `float` o `double`.

## 📐 Arquitectura & Reglas Estrictas
- Separación estricta de capas: Controller → Service → Repository → DTOs
- DTOs separados de entidades JPA. Usar MapStruct para mapeo.
- Validación con Bean Validation en backend + Zod en frontend.
- Paginación server-side obligatoria en todos los listados.
- Concurrencia en stock: `@Version` + `REPEATABLE_READ` o transacciones optimistas.
- Auditoría inmutable: tabla `auditoria_log`. Nunca logs editables/borrables.
- Contraseñas: BCrypt cost ≥ 12. Tokens JWT 8h, refresh 7d (Redis/httpOnly cookie).
- Migraciones DB: Solo Flyway (`resources/db/migration`). Nunca modificar migraciones aplicadas.
- Respuestas API estandarizadas: `{ success, message, data, errors, timestamp, pagination }`
- Códigos HTTP: 200, 201, 204, 400, 401, 403, 404, 409, 422, 500.

## 🤖 Cómo Debes Asistirme (Reglas de Interacción)
1. Explica brevemente el "porqué" antes de generar código.
2. Divide tareas complejas en pasos pequeños. Pide confirmación antes de avanzar.
3. Si falta información, pregúntala. Nunca asumas reglas de negocio.
4. Usa nombres de variables/funciones en inglés, pero comentarios en español.
5. Prioriza seguridad, trazabilidad y mantenibilidad sobre velocidad.
6. Si sugieres un cambio de arquitectura, explica el trade-off.

## 📚 Referencias Técnicas
- Requisitos completos: `REQUISITOS_Y_ARQUITECTURA_AAC27.md` (RF01-RF31, RNF-SEG01 a RNF-INF06)
- Skills detallados: `.claude/skills/`
- Modelo de datos: Diccionario en requisitos (tablas `usuario`, `producto`, `venta`, etc.)