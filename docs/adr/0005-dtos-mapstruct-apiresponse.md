# ADR-0005 — DTOs separados con MapStruct y `ApiResponse<T>` estandarizado

- **Estado:** Aceptado (retroactivo)
- **Fecha:** 2026-11-25
- **Contexto:** La API no debe exponer entidades JPA (acoplamiento, fugas, lazy-loading). Se requieren contratos estables para el frontend y respuestas uniformes (RNF-MAN04/05, convenciones API §4.3).
- **Opciones consideradas:**

| Opción | Pros | Contras | Encaje con RNF |
|---|---|---|---|
| DTOs + MapStruct + ApiResponse (elegida) | Contrato estable; mapeo en tiempo de compilación; respuesta uniforme | Código de mapeo adicional | Cumple MAN04/05 |
| Exponer entidades | Menos código | Fugas, acoplamiento, problemas de serialización | Inaceptable |
| Mapeo manual | Control total | Verboso y propenso a error | Peor que MapStruct |

- **Decisión:** DTOs de Request (con Bean Validation) y Response separados de las entidades; mapeo con MapStruct (`@Mapper(componentModel="spring")`); toda respuesta envuelta en `ApiResponse<T>` (`success, message, data, errors, timestamp, pagination`); errores vía `GlobalExceptionHandler` (`@ControllerAdvice`).
- **Consecuencias:** + contratos estables y respuestas consistentes; − hay que mantener DTOs/mappers por módulo (los módulos compra/venta aún tienen cobertura parcial — DT-04).
