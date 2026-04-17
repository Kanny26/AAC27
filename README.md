# 🏛️ Sistema de Gestión de Joyería AAC27

> **Proyecto de Desarrollo Fullstack |**
> Versión 2.0 | Desarrollado con Spring Boot 3, React 18 y Docker.

## 📖 Descripción
**AAC27** es una aplicación web robusta diseñada para gestionar las operaciones de una joyería física (single-tenant). El sistema cubre el ciclo completo de negocio: desde la gestión de inventarios y proveedores hasta el Punto de Venta (POS), fidelización de clientes y postventa (garantías/reparaciones).

Este proyecto utiliza una arquitectura de capas estricta en el backend y una arquitectura basada en componentes en el frontend, asegurando seguridad, escalabilidad y trazabilidad de datos.

---

## 🚀 Tecnologías Utilizadas

### Backend
- **Lenguaje:** Java 21
- **Framework:** Spring Boot 3.2+
- **Seguridad:** Spring Security + JWT + BCrypt
- **Persistencia:** Spring Data JPA (Hibernate) + Flyway (Migraciones)
- **Base de Datos:** MySQL 8.0+
- **Cache:** Redis
- **Documentación:** OpenAPI 3.0 (Swagger)

### Frontend
- **Librería:** React 18 + TypeScript
- **Build Tool:** Vite
- **Estado:** Zustand (Global) + React Query (Servidor)
- **Formularios:** React Hook Form + Zod
- **UI/UX:** TailwindCSS, React Hot Toast

### Infraestructura & DevOps
- **Contenedores:** Docker & Docker Compose
- **CI/CD:** GitHub Actions (Configurado para Build y Test)
- **Testing:** JUnit 5, Mockito, Testcontainers

---
