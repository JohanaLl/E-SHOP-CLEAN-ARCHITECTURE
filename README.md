# E-SHOP

E-commerce educativo construido como microservicios con **Arquitectura Hexagonal**, siguiendo un
enfoque **Specification First**: cada servicio se documenta por completo en [`/spec`](./spec)
antes de escribir una sola línea de código.

## Documentación completa

- **[`spec/README.md`](./spec/README.md)** — punto de entrada: objetivo, alcance, stack,
  microservicios, flujo de negocio, roadmap y convenciones.
- **[`spec/OVERVIEW.md`](./spec/OVERVIEW.md)** — visión global, arquitectura completa, comunicación
  entre servicios, principios hexagonales, estrategia de pruebas y decisiones de arquitectura.
- **[`CLAUDE.md`](./CLAUDE.md)** — mecánica de build/test y notas específicas del repositorio para
  trabajar con Claude Code.

## Estado actual

| Servicio | Especificación (`spec/`) | Código |
|----------|---------------------------|--------|
| `customer-service` | ✅ Completa | Esqueleto Spring Boot, sin dominio implementado aún |
| `product-service` | ⏳ Pendiente | No creado |
| `inventory-service` | ⏳ Pendiente | No creado |
| `order-service` | ⏳ Pendiente | No creado |

## Stack

Java 17 · Spring Boot 3.5.x · Spring Data JPA · PostgreSQL · MapStruct · OpenFeign · Docker ·
JUnit 5 · Mockito · Testcontainers.

Detalle completo del stack, arquitectura y roadmap en [`spec/README.md`](./spec/README.md).
