# Dependency Audit — T-01

## Resultado

El backend ya declara las dependencias base para Java 21 + Spring Boot 3.2:

- Web REST: `spring-boot-starter-web`
- Seguridad: `spring-boot-starter-security`
- Persistencia: `spring-boot-starter-data-jpa`
- Validación: `spring-boot-starter-validation`
- Observabilidad base: `spring-boot-starter-actuator`
- Base de datos: `mysql-connector-j`
- Migraciones: `flyway-core` y `flyway-mysql`
- JWT: `jjwt-api`, `jjwt-impl`, `jjwt-jackson`
- Mapeo: `mapstruct`
- Boilerplate compile-time: `lombok`
- Testing base: `spring-boot-starter-test` y `spring-security-test`

## Ajustes aplicados

- Se configuró `spring.profiles.active` mediante `${SPRING_PROFILES_ACTIVE:dev}` para permitir override por ambiente.
- Se corrigieron placeholders productivos de base de datos a `DB_USERNAME`, `DB_PASSWORD` y `DB_NAME`.
- Se configuró `maven-compiler-plugin` con `<release>21</release>`.
- Se agregó `lombok-mapstruct-binding` como annotation processor para hacer reproducible la generación MapStruct con modelos Lombok.

## Dependencias no agregadas todavía

- Redis no se agregó al `pom.xml` porque no existe uso actual de cache/Redis en el código.
- Testcontainers no se agregó todavía porque las pruebas de integración se planifican para T-05/T-22.

## Validación pendiente

No fue posible ejecutar `mvn test` en este entorno porque Maven no está instalado y el proyecto no incluye Maven Wrapper.

Comando esperado en un entorno con Maven:

```powershell
cd backend
mvn test
```
