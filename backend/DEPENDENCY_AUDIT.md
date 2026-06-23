# Dependency Audit â€” T-01

## Resultado

El backend ya declara las dependencias base para Java 21 + Spring Boot 3.2:

- Web REST: `spring-boot-starter-web`
- Seguridad: `spring-boot-starter-security`
- Persistencia: `spring-boot-starter-data-jpa`
- ValidaciÃ³n: `spring-boot-starter-validation`
- Observabilidad base: `spring-boot-starter-actuator`
- Base de datos: `mysql-connector-j`
- Migraciones: `flyway-core` y `flyway-mysql`
- JWT: `jjwt-api`, `jjwt-impl`, `jjwt-jackson`
- Mapeo: `mapstruct`
- Boilerplate compile-time: `lombok`
- Testing base: `spring-boot-starter-test` y `spring-security-test`

## Ajustes aplicados

- Se configurÃ³ `spring.profiles.active` mediante `${SPRING_PROFILES_ACTIVE:dev}` para permitir override por ambiente.
- Se corrigieron placeholders productivos de base de datos a `DB_USERNAME`, `DB_PASSWORD` y `DB_NAME`.
- Se configurÃ³ `maven-compiler-plugin` con `<release>21</release>`.
- Se agregÃ³ `lombok-mapstruct-binding` como annotation processor para hacer reproducible la generaciÃ³n MapStruct con modelos Lombok.

## Dependencias no agregadas todavÃ­a

- Redis no se agregÃ³ al `pom.xml` porque no existe uso actual de cache/Redis en el cÃ³digo.
- Testcontainers no se agregÃ³ todavÃ­a porque las pruebas de integraciÃ³n se planifican para T-05/T-22.

## Validación

Validación ejecutada correctamente el 2026-06-22 usando Maven local con `JAVA_HOME` apuntando a JDK 23, compilando el proyecto con `<release>21</release>`.

Resultado:

- `mvn.cmd test`: `BUILD SUCCESS`
- Compilación main: sin cambios pendientes, clases actualizadas.
- Compilación test: sin fuentes de test todavía.
- Surefire: `No tests to run.`

Comando usado:

```powershell
cd backend
$env:JAVA_HOME='C:\Program Files\Java\jdk-23'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn.cmd test
```

