# Requisitos no funcionales — AAC27

> Derivado de `REQUISITOS_Y_ARQUITECTURA_AAC27.md` §2. IDs canónicos. Regla: si no se puede medir,
> no es un requisito. Método de verificación explícito por fila.

## Seguridad
| ID | Requisito | Métrica / umbral | Verificación | Prioridad |
|---|---|---|---|---|
| RNF-SEG01 | JWT stateless | access 8h, refresh 7d (httpOnly cookie o Redis) | Test + inspección config | Must |
| RNF-SEG02 | RBAC con Spring Security | `@PreAuthorize` por endpoint; 403 sin rol | `RbacMatrizTest` | Must |
| RNF-SEG03 | BCrypt contraseñas | cost ≥ 12; nunca texto plano ni logs | Revisión + test | Must |
| RNF-SEG04 | CSRF/CORS | CORS por entorno (no `*` en prod) | Test CORS (T-34) | Must |
| RNF-SEG05 | XSS | Bean Validation back + CSP front | Revisión | Must |
| RNF-SEG06 | TLS en tránsito | TLS 1.2+; credenciales por env | Config Nginx (T-40) | Must |
| RNF-SEG07 | Rate limiting auth | ≤10 intentos/IP en 15 min | Test (T-34) | Must |
| RNF-SEG08 | Tokens de recuperación | uso único, 15 min, hash en BD | Test | Must |

## Rendimiento y Escalabilidad
| ID | Requisito | Métrica / umbral | Verificación | Prioridad |
|---|---|---|---|---|
| RNF-REN01 | Latencia API | p95 < 300ms con ≤50 usuarios concurrentes | Test de carga en staging | Must |
| RNF-REN02 | Caché 2º nivel | Caffeine/Redis catálogos TTL 10 min | Inspección + métricas | Should |
| RNF-REN03 | Paginación | obligatoria; page size máx 100 | Test de controller | Must |
| RNF-REN04 | Concurrencia de stock | REPEATABLE_READ + `@Version` | Test concurrente (T-18) | Must |
| RNF-REN05 | PDF asíncrono | `@Async`; jobs en reportes pesados | Inspección | Should |
| RNF-REN06 | Índices BD | en `usuario.correo`, `producto.codigo`, `venta.fecha_emision`, `cliente.documento` | Revisión migraciones (T-14) | Must |
| RNF-REN07 | Frontend | lazy loading, code splitting, WebP/caché | Lighthouse | Should |

## Mantenibilidad y Calidad
| ID | Requisito | Métrica / umbral | Verificación | Prioridad |
|---|---|---|---|---|
| RNF-MAN01 | Cobertura | ≥80% en services; integración en endpoints críticos | JaCoCo (T-32) | Must |
| RNF-MAN02 | Logging | JSON en prod; INFO/ERROR; DEBUG off en prod | Inspección (T-33) | Must |
| RNF-MAN03 | OpenAPI/Swagger | cada endpoint documentado | `/v3/api-docs` (T-12) | Must |
| RNF-MAN04 | Bean Validation | mensajes en español, sin stacktrace al cliente | Test MockMvc (T-09) | Must |
| RNF-MAN05 | Excepciones centralizadas | `@ControllerAdvice` + `ApiResponse<T>` | Test (T-09) | Must |
| RNF-MAN06 | Flyway | migraciones SQL versionadas | CI | Must |
| RNF-MAN07 | Monitoreo | Actuator + Micrometer (Prometheus en prod) | Inspección (T-33) | Should |

## Usabilidad y UX
| ID | Requisito | Métrica / umbral | Verificación |
|---|---|---|---|
| RNF-UX01 | Responsive mobile-first | funcional desde 360px; Chrome/Firefox/Edge/Safari | e2e visual |
| RNF-UX02 | Accesibilidad WCAG 2.1 AA | contraste ≥4.5:1, teclado, ARIA | Auditoría |
| RNF-UX03 | Feedback inmediato | spinners, toasts, validación en vivo | e2e |
| RNF-UX04 | Estados asíncronos | loading/error/success + Skeleton | Revisión |
| RNF-UX05 | ≤3 pasos por operación | con confirmación visual | Revisión UX |
| RNF-UX06 | i18n básica | español CO; fecha dd/mm/yyyy; COP con miles | Revisión |

## Infraestructura y Despliegue
| ID | Requisito | Métrica / umbral | Verificación |
|---|---|---|---|
| RNF-INF01 | Dockerizado | compose: api/web/db/cache | `docker compose up` (T-02) |
| RNF-INF02 | Config por env | credenciales/secretos/SMTP por variables | Inspección |
| RNF-INF03 | CI/CD | build + tests + imagen + staging | GitHub Actions (T-06/T-43) |
| RNF-INF04 | Backups MySQL | diarios, retención 30d, restore mensual | Script + doc (T-35) |
| RNF-INF05 | Frontend por Nginx | gzip/brotli, caché assets, SPA fallback | Config (T-40) |
| RNF-INF06 | RTO/RPO | RTO <4h, RPO <24h | Ensayo (T-46) |
