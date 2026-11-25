# ADR-0002 — Autenticación JWT stateless

- **Estado:** Aceptado (retroactivo)
- **Fecha:** 2026-11-25
- **Contexto:** API REST consumida por una SPA React. Requisitos RNF-SEG01 (access 8h, refresh 7d), RNF-SEG03 (BCrypt≥12), RNF-SEG07 (rate limiting). Sin sesiones de servidor para simplificar escalado.
- **Opciones consideradas:**

| Opción | Pros | Contras | Encaje con RNF |
|---|---|---|---|
| JWT stateless + refresh (elegida) | Sin estado de sesión; escalable; estándar | Revocación requiere lista negra/rotación (Redis) | Cumple SEG01 |
| Sesión de servidor (cookie) | Revocación inmediata | Estado en servidor; menos amigable a SPA/escala | Innecesario |

- **Decisión:** JWT (JJWT) HMAC con access 8h; refresh 7d (Redis/httpOnly cookie). `JwtAuthenticationFilter` (OncePerRequestFilter); `UserDetailsService` carga el usuario desde BD; `@PreAuthorize` para RBAC. `JwtProperties` validado al arrancar (secret ≥32 chars).
- **Consecuencias:** + escalable y estándar; − la revocación/logout necesita infraestructura adicional (lista negra/rotación) que queda pendiente de endurecer (RF04); nunca criptografía propia.
