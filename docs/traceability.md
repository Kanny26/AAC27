# Matriz de trazabilidad — AAC27

> RF → spec → AC → test(s). Se actualiza en cada feature (Art. 4). Sembrada con lo ya implementado y
> testeado (baseline). Los tests viven en `backend/src/test/java/com/aac27/joyeria/`.
> `AC-` definitivos se detallan en el `spec.md` de cada feature; aquí se enlaza lo verificable hoy.

| RF | RN | Spec | AC (resumen) | Test(s) | Estado |
|---|---|---|---|---|---|
| RF02 | RN-013 | 001 | Login válido → JWT; inválido → 401 | `config/AuthFlujoTest`, `config/AuthSecurityTest` | ✅ |
| RF02 | RN-013 | 001 | Clave provisional bloquea API hasta cambiarla | `config/PasswordProvisionalBloqueoTest` | ✅ |
| RF05 | RN-012 | 001 | Cuenta inactiva no autentica (tras verificar password) | `config/AuthFlujoTest` | ✅ |
| RF06 | RN-012 | 001 | Alta solo admin; no crea superadministrador; envía correo | `config/AuthFlujoTest`, `config/RegistroCorreoTest` | ✅ |
| RF-S02 | RN-013 | 001 | Política de contraseña + no reutilizar últimas 5 | `config/CambioPasswordTest`, `config/HistorialPasswordTest` | ✅ |
| RF01 | RN-012 | 001 | RBAC: sin token 401, rol incorrecto 403, 13 endpoints × 3 roles | `config/RbacMatrizTest` | ✅ |
| RF25 | — | 001 | Cambio de estado postventa restringido a admin | `config/RbacMatrizTest` | ✅ |
| RF26 | — | 001 | Vendedor busca casos solo de sus ventas | `module/postventa/...PostventaServiceBusquedaTest` | ✅ |
| RNF-SEG01 | — | 001 | Config JWT (secret ≥32, expiraciones) validada al arrancar | `config/JwtPropertiesTest`, `config/JwtUtilTest` | ✅ |
| RNF-SEG03 | RN-013 | 001 | No se loguea la contraseña provisional fuera de dev | `config/ContrasenaProvisionalLogTest` | ✅ |
| RF09, RF10, RF11-E, RF-P01 | RN-012 | 003 | Proveedor CRUD + validaciones (pendiente) | — (se crean en spec 003) | ⏳ |
| RF15 | RN-002 | 008 | Stock nunca negativo | — (pendiente) | ⏳ |
| RF20 | RN-009 | 010 | Venta recalcula total y descuenta stock | — (pendiente) | ⏳ |

> ⏳ = requisito con spec futura; sin test aún. El objetivo de cada feature SDD es mover su fila a ✅
> con al menos un test por AC.
