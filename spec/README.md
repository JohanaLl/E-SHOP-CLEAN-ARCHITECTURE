# E-SHOP — E-commerce con Microservicios y Arquitectura Hexagonal

> Proyecto educativo: construir un e-commerce mínimo (clientes, catálogo, inventario y órdenes)
> para dominar **Arquitectura Hexagonal (Ports & Adapters)**, **DDD táctico** y **comunicación
> síncrona entre microservicios**, sin sobreingeniería.

Este es el punto de entrada del proyecto. Toda la documentación funcional y técnica vive bajo
[`/spec`](.) siguiendo un enfoque **Specification First**: no se escribe código de un
requerimiento hasta que su especificación existe y ha sido revisada.

---

## Descripción del proyecto

Un cliente arma una orden con varios productos; el sistema valida que el cliente esté activo, que
los productos existan y estén disponibles, reserva el stock, calcula el total y deja la orden
registrada. Ese flujo, aparentemente simple, obliga a resolver los problemas centrales de una
arquitectura de microservicios: cómo modelar un dominio rico por servicio, cuándo un servicio debe
llamar a otro, cómo mantener el desacoplamiento, y qué hacer cuando una dependencia falla a mitad
de una operación.

El objetivo de negocio (vender productos) es secundario. El objetivo real es **pedagógico y
arquitectónico**.

## Objetivo

- Practicar arquitectura hexagonal de punta a punta en 4 microservicios reales.
- Aprender a diseñar comunicación síncrona REST entre servicios, incluyendo manejo de fallos,
  compensación manual (saga primitiva) e idempotencia.
- Mantener una separación estricta de responsabilidades (bounded contexts) con base de datos
  propia por servicio.

## Alcance

**Dentro de la primera versión (4 microservicios):**

| Servicio | Responsabilidad en una frase |
|----------|-------------------------------|
| `customer-service` | Saber quién es cada cliente y si puede comprar (activo/inactivo). |
| `product-service` | Saber qué se vende, su descripción, categoría y **precio**. |
| `inventory-service` | Saber **cuánto hay** de cada producto y controlar reservas/descuentos. |
| `order-service` | **Componer** una compra válida usando a los otros tres y llevar su ciclo de vida. |

**Fuera de alcance de esta versión** (roadmap): Payments, Auth, Audit, Reports, API Gateway,
Service Discovery, Config Server, mensajería asíncrona (Kafka), Event Sourcing, CQRS, Redis,
Circuit Breaker/Resilience4j, Kubernetes, observabilidad.

Detalle completo de objetivos, criterio de decisión y alternativas descartadas en
[`OVERVIEW.md`](./OVERVIEW.md).

## Stack tecnológico

Java 17 · Spring Boot 3.5.x · Spring Web (MVC) · Spring Data JPA · PostgreSQL · MapStruct ·
OpenFeign (llamadas entre servicios, solo en `order-service`) · Docker / Docker Compose ·
JUnit 5 · Mockito · Testcontainers · Lombok.

Explícitamente **fuera** de esta primera versión: Kafka/mensajería asíncrona, Event Sourcing,
CQRS, Saga orquestada, Redis, API Gateway, Config Server, Service Discovery, OAuth2/Keycloak,
observabilidad (Prometheus/Grafana), Circuit Breaker/Resilience4j, Kubernetes, Service Mesh.

## Arquitectura general

- **Estilo:** microservicios con **base de datos por servicio**, comunicación **síncrona
  REST/JSON**, y **cada servicio internamente hexagonal** (dominio, aplicación, adaptadores).
- **Topología:** tres servicios de dominio autónomos (`customer-service`, `product-service`,
  `inventory-service`, sin dependencias salientes) y un servicio **coordinador**
  (`order-service`) que compone una operación de negocio llamando a los otros tres.
- **Grafo de dependencias — DAG, dirigido y acíclico:**

  ```
                      ─────────────► customer-service   (valida cliente activo)
                      │
   order-service ─────┼────────────► product-service    (valida productos + obtiene precios)
   (coordinador)      │
                      └────────────► inventory-service  (consulta / reserva / libera / descuenta stock)

   customer-service, product-service, inventory-service  ───► (no dependen de nadie)
  ```

  Solo `order-service` inicia llamadas entre servicios. Los demás nunca se llaman entre sí ni
  llaman a `order-service`. Sin infraestructura de plataforma todavía (sin gateway/discovery/
  config): las URLs se configuran a mano en `application.yml`. Docker Compose levanta los 4
  servicios + 4 instancias de PostgreSQL.

> **Diagramas pendientes:** cuando se entreguen imágenes de arquitectura (diagrama lógico,
> diagrama de secuencia de "crear orden", DAG de dependencias), se insertarán aquí con rutas
> relativas, p. ej. `![Arquitectura](./images/architecture.png)`. Por ahora el grafo anterior es
> una representación en texto equivalente.

## Microservicios

| Servicio | Bounded context | Es dueño de… | Depende de |
|----------|-----------------|--------------|------------|
| `customer-service` | Identidad del cliente | Datos y estado comercial del cliente | Ninguno |
| `product-service` | Catálogo | Descripción y **precio** de cada producto | Ninguno |
| `inventory-service` | Existencias | Cantidad en mano, reservada y disponible; historial de movimientos | Ninguno |
| `order-service` | Venta (coordinador) | La orden y su ciclo de vida (`PENDING → CONFIRMED / CANCELLED / FAILED`) | `customer-service`, `product-service`, `inventory-service` |

Reglas de comunicación (memorizar; gobiernan todo el sistema):

1. Solo `order-service` inicia llamadas entre servicios.
2. Nadie accede a la base de datos de otro servicio. Todo dato ajeno se pide por su API HTTP.
3. El dueño del dato es la única autoridad sobre ese dato (precio → Products, stock → Inventory,
   estado del cliente → Customers).
4. Comunicación síncrona REST/JSON. Sin eventos, sin colas, por ahora.
5. Sin ciclos. Ninguna dependencia inversa hacia `order-service`.

Detalle completo (entidades, casos de uso, endpoints, persistencia, pruebas) vivirá en
`REQUIREMENT.md` y `CHECKLIST.md` de cada servicio a medida que se especifiquen.

## Flujo principal del negocio: crear una orden

```
1. order-service recibe POST /api/orders { customerId, items[] }.
2. order-service → customer-service:  GET /api/customers/{id}        (¿existe? ¿ACTIVE?)
3. order-service → product-service:    GET /api/products?ids=...      (¿existen/activos? precio)
4. order-service → inventory-service:  POST /api/inventory/availability (¿hay stock?)
5. order-service calcula subtotal, impuestos y total (dominio puro, sin salir).
6. order-service persiste la orden en estado PENDING.
7. order-service → inventory-service:  POST /api/inventory/reserve    (reserva stock)
8. order-service responde 201 Created { orderId, total }.
```

Si cualquier validación de solo lectura (2–4) falla, no se escribe nada y se responde 422/409/503
según el tipo de fallo. Si la reserva del paso 7 falla después de persistir la orden (paso 6), se
aplica **compensación**: la orden pasa a `FAILED`. Justificación completa, alternativas
consideradas y tabla de manejo de errores en [`OVERVIEW.md`](./OVERVIEW.md).

## Roadmap

Cada fase se termina y se prueba completa antes de iniciar la siguiente:

| Fase | Contenido | Por qué en ese orden |
|------|-----------|------------------------|
| 0 | Preparación (esqueletos Spring Boot, Docker Compose con 4 PostgreSQL) | Terreno listo sin ruido de negocio |
| 1 | `customer-service` completo y autónomo | Servicio más simple; fija el patrón hexagonal |
| 2 | `product-service` | Consolida el patrón; introduce VO `Money` y endpoint batch |
| 3 | `inventory-service` | Dominio rico, invariantes fuertes, concurrencia (bloqueo optimista) |
| 4 | `order-service` (coordinador) | Necesita a los tres anteriores vivos; integra todo lo aprendido |
| 5 | Docker Compose con el sistema completo + prueba de humo E2E | Cierre de la primera versión |
| 6+ | Extensiones opcionales: Payments, Auth, Audit, Reports, Gateway, eventos… | Solo cuando lo anterior esté sólido |

Roadmap completo, con checklist de "fase terminada", en [`OVERVIEW.md`](./OVERVIEW.md).

## Estructura del repositorio

```
E-SHOP/
├── CLAUDE.md
├── customer-service/        # módulo Spring Boot (Fase 1)
├── product-service/         # módulo Spring Boot (Fase 2, aún no creado)
├── inventory-service/       # módulo Spring Boot (Fase 3, aún no creado)
├── order-service/           # módulo Spring Boot (Fase 4, aún no creado)
└── spec/                    # especificación funcional y técnica (este árbol)
    ├── README.md            # este archivo
    ├── OVERVIEW.md           # visión global, arquitectura completa, roadmap detallado
    ├── customer-service/
    │   ├── REQUIREMENT.md
    │   └── CHECKLIST.md
    ├── product-service/
    │   ├── REQUIREMENT.md
    │   └── CHECKLIST.md
    ├── inventory-service/
    │   ├── REQUIREMENT.md
    │   └── CHECKLIST.md
    └── order-service/
        ├── REQUIREMENT.md
        └── CHECKLIST.md
```

Cada carpeta bajo `spec/` corresponde 1:1 al nombre del módulo de código que implementa ese
servicio. Un nuevo requerimiento que no encaje en los 4 servicios anteriores crea su propia
carpeta bajo `spec/` (p. ej. `spec/payment-service/` cuando se aborde el roadmap Fase 6+).

## Convenciones

- **Nombres de módulo:** singular + sufijo `-service` (`customer-service`, no `customers`).
  Coincide con el módulo ya existente en el repositorio.
- **Paquete raíz Java:** `com.shop.<servicio>.service` (p. ej. `com.shop.product.service`),
  siguiendo el patrón ya establecido en `customer-service` (`com.shop.customer.service`).
- **Lenguaje de dominio (bounded context):** en la documentación de negocio los servicios se
  nombran en plural y en el idioma del dominio — *Customers*, *Products*, *Inventory*, *Orders* —
  independientemente del nombre técnico del módulo. Son dos capas de nombres distintas y no deben
  confundirse.
- **Paquetes internos por servicio (arquitectura hexagonal):**
  ```
  com.shop.<servicio>.service/
  ├── domain/            # model/, event/, exception/  — sin imports de Spring ni JPA
  ├── application/
  │   ├── port/in/        # casos de uso como interfaces
  │   ├── port/out/        # repositorios y clientes externos, como interfaces
  │   └── usecase/         # implementaciones @Service
  ├── adapter/
  │   ├── in/web/          # @RestController, DTOs, GlobalExceptionHandler, mappers
  │   └── out/
  │       ├── persistence/ # @Entity JPA, JpaRepository, PersistenceAdapter, mappers
  │       └── feign/       # clientes Feign hacia otros servicios (solo order-service)
  └── config/              # BeanConfiguration, OpenApiConfig
  ```
- **Endpoints REST:** recurso en plural (`/api/customers`, `/api/products`, `/api/inventory`,
  `/api/orders`), independientemente de que el módulo se llame en singular.
- **Regla de oro contra la sobreingeniería:** antes de añadir cualquier cosa al proyecto, pasarla
  por el filtro *"¿esto ayuda de verdad a aprender Arquitectura Hexagonal? Si la respuesta es no,
  se elimina."*

## Enlaces internos

- [`OVERVIEW.md`](./OVERVIEW.md) — visión global, arquitectura completa, comunicación entre
  microservicios, principios hexagonales, estrategia de pruebas y roadmap detallado.
- `customer-service/REQUIREMENT.md` y `CHECKLIST.md` — *(pendiente de especificar)*.
- `product-service/REQUIREMENT.md` y `CHECKLIST.md` — *(pendiente de especificar)*.
- `inventory-service/REQUIREMENT.md` y `CHECKLIST.md` — *(pendiente de especificar)*.
- `order-service/REQUIREMENT.md` y `CHECKLIST.md` — *(pendiente de especificar)*.
