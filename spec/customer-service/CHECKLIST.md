# Checklist técnico — `customer-service`

> Orden lógico de implementación (de adentro hacia afuera, dominio primero). Basado en
> `REQUIREMENT.md` y en el criterio de "fase terminada" de `../OVERVIEW.md §7`. Marcar cada casilla
> solo cuando el ítem compila, tiene pruebas verdes y fue verificado — no antes.

## 1. Dominio (sin Spring, sin JPA)

- [x] Crear value object `Email` (record, trim + minúsculas + validación de formato en el
      constructor compacto) — ver `REQUIREMENT.md §6.3`
- [x] Crear enum `CustomerStatus` (`ACTIVE`, `INACTIVE`) — ver `§6.2`
- [x] Crear enum `DocumentType` (`CC`, `NIT`, `CE`, `PASSPORT`) — ver `§6.2`
- [x] Crear excepciones de dominio: `InvalidEmailException`, `CustomerNotFoundException`,
      `DuplicateEmailException`, `DuplicateDocumentException` — ver `§6.4`
- [x] Crear agregado `Customer` con dos factory methods: `create(...)` (nace `ACTIVE`, genera
      `id`/timestamps) y `reconstruct(...)` (usado solo por el mapper de persistencia) — ver `§6.1`
- [x] Implementar métodos de dominio: `activate()`, `deactivate()`, `updateContact(...)`. Nota: la
      idempotencia "sin `save` de más" para `activate()`/`deactivate()` es responsabilidad del
      futuro `ChangeCustomerStatusUseCaseImpl` (§6.2), no del método de dominio en sí — eso sigue
      pendiente en la sección 3.
- [ ] Pruebas unitarias de `Customer` — implementar los 16 casos de `REQUIREMENT.md §15.1`
      (invariantes de longitud, `create()` vs `reconstruct()`, idempotencia de
      `activate()`/`deactivate()` sin cambio de `updatedAt`, inmutabilidad de `id`/`createdAt`)
- [ ] Pruebas unitarias de `Email` — implementar los 8 casos de `§15.1` (normalización,
      igualdad por valor, formatos inválidos: sin `@`, sin parte local, sin dominio, vacío/null)
- [ ] Pruebas unitarias de enums — `DocumentType.values()` y `CustomerStatus.values()` exactos
      (`§15.1` #25-26)

## 2. Puertos

- [ ] Definir `CustomerRepositoryPort` (out): `save`, `findById`, `findByDocument`, `findAll`
      paginado, `existsByEmail`, **`existsByEmailExcludingId`** (necesario para CU-5, ver
      `§6.1`/`§12`), `existsByDocument`
- [ ] Definir puertos de entrada (in): `CreateCustomerUseCase`, `GetCustomerUseCase`,
      `UpdateCustomerUseCase`, `ListCustomersUseCase`, `ChangeCustomerStatusUseCase`

## 3. Casos de uso (application/usecase)

- [ ] Implementar `CreateCustomerUseCaseImpl` (CU-1): valida **primero email, luego documento** (orden
      fijado en `§7`) + los 6 escenarios unitarios de `§15.2` (feliz, email duplicado, documento
      duplicado, ambos duplicados, normalización antes de comprobar, propagación de error de
      dominio)
- [ ] Implementar `GetCustomerUseCaseImpl` (CU-2, CU-3) + los 4 escenarios de `§15.2` (encontrado/no
      encontrado por id y por documento)
- [ ] Implementar `ListCustomersUseCaseImpl` (CU-4) + los 7 escenarios de `§15.2` (feliz, filtro por
      status, `page` negativo, `size=0`, `size=101`, `size=100` borde válido, página vacía por
      estar fuera de rango)
- [ ] Implementar `UpdateCustomerUseCaseImpl` (CU-5): la comprobación de unicidad de email debe
      **excluir el propio id** del cliente + los 5 escenarios de `§15.2` (incluye el caso de borde
      "email igual al que ya tenía")
- [ ] Implementar `ChangeCustomerStatusUseCaseImpl` (CU-6, CU-7): debe verificar el estado actual
      **antes** de invocar el método de dominio y **omitir el `save`** si no hay cambio real + los
      5 escenarios de `§15.2` (activar/desactivar con y sin cambio real, no encontrado)

## 4. Adaptador de persistencia

- [ ] Crear `CustomerJpaEntity` (`@Entity`, tabla `customers`)
- [ ] Definir restricciones `UNIQUE(email)` y `UNIQUE(document_type, document_number)` en el
      esquema (migración SQL o `schema.sql`/Flyway/Liquibase, según se decida)
- [ ] Crear `CustomerJpaRepository` (extends `JpaRepository`)
- [ ] Crear `CustomerPersistenceMapper` (MapStruct: JPA ↔ dominio; dirección BD→dominio usa
      `Customer.reconstruct(...)`, nunca `create(...)`)
- [ ] Crear `CustomerPersistenceAdapter` (implementa `CustomerRepositoryPort`; traduce violación de
      `UNIQUE` a `DuplicateEmailException`/`DuplicateDocumentException`; implementa
      `existsByEmailExcludingId`)
- [ ] Pruebas de integración con Testcontainers + PostgreSQL — implementar los 12 casos de
      `REQUIREMENT.md §15.3`, incluida la **prueba de condición de carrera real** (dos
      transacciones concurrentes insertando el mismo email, #10) y `existsByEmailExcludingId`
      (#7). **No usar H2.**

## 5. Adaptador web

- [ ] Crear DTOs de request/response (`CreateCustomerRequest`, `UpdateCustomerRequest`,
      `CustomerResponse`, página de resultados) con Bean Validation reflejando los límites de
      `REQUIREMENT.md §13` (`fullName` 2–120, `documentNumber` 5–20, `phone` 7–20, `address`
      máx. 200, `page>=0`, `1<=size<=100`)
- [ ] Crear `CustomerWebMapper` (MapStruct: DTO ↔ dominio)
- [ ] Crear `CustomerController` con los 7 endpoints de `REQUIREMENT.md §8`
- [ ] Crear `GlobalExceptionHandler` (`@RestControllerAdvice`): mapear cada excepción de dominio a
      su código HTTP (400/404/409)
- [ ] Pruebas de integración web (`@WebMvcTest` o `@SpringBootTest`+`MockMvc`) — implementar los 27
      casos de `REQUIREMENT.md §15.4`, uno por endpoint × escenario (feliz, cada validación 400,
      404, 409, y los bordes de paginación)

## 6. Configuración

- [ ] `BeanConfiguration`: cableado explícito de casos de uso con sus puertos de salida (si no se
      usa `@Service`+inyección directa)
- [ ] `OpenApiConfig`: metadata básica de OpenAPI/Swagger para el servicio

## 7. Verificación manual

- [ ] Levantar el servicio contra PostgreSQL (Docker Compose) y probar con curl/Postman: crear,
      consultar por id, buscar por documento, listar paginado, actualizar, inactivar, activar
- [ ] Probar a mano los caminos de error y bordes: email duplicado, documento duplicado, id
      inexistente, paginación inválida (`page` negativo, `size=0`, `size=101`), actualizar
      manteniendo el mismo email propio, inactivar/activar dos veces seguidas (verificar que
      `updatedAt` no cambia la segunda vez)

## 8. Cierre de fase

- [ ] Cobertura dominio + casos de uso ≥ 90%
- [ ] Cobertura global del servicio ~80%
- [ ] Todas las pruebas (unitarias + integración) en verde, incluida la prueba de concurrencia de
      `§15.3` #10
- [ ] `REQUIREMENT.md §18` (decisiones pendientes) resuelto o conscientemente diferido antes de dar
      la fase por cerrada
