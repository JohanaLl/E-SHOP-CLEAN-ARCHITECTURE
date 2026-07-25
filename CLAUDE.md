# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Specification-first workflow — read this before implementing anything

This project follows **Spec-Driven Development**. The full functional and technical specification
lives under [`spec/`](spec/README.md) and [`spec/OVERVIEW.md`](spec/OVERVIEW.md). **Do not write
domain code for a service until its `spec/<service>/REQUIREMENT.md` exists and has been reviewed.**
If a requirement doc conflicts with `spec/OVERVIEW.md`, `OVERVIEW.md` wins until a decision is
explicitly recorded there.

Start there, not here, for: business rules, bounded contexts, inter-service communication
contracts, failure-handling rules, hexagonal package layout, and the phased roadmap. This
CLAUDE.md only tracks build mechanics and repo-specific gotchas that aren't spec content.

## Project state

This workspace (`E-SHOP`) hosts multiple Spring Boot microservices for an e-commerce system
(customers, catalog, inventory, orders — see `spec/OVERVIEW.md` for the full picture). Currently
only one module exists:

- `customer-service/` — a freshly generated Spring Boot 3.5.16 (Java 17) skeleton from Spring Initializr. It has no domain code yet: `CustomerServiceApplication` is an empty `@SpringBootApplication` entry point, and the only test is the default `contextLoads()` smoke test.

`product-service/`, `inventory-service/`, `order-service/` don't exist yet (roadmap Fases 2-4 in
`spec/OVERVIEW.md`). There is no root build file and this directory is not yet a git repository.
As more services and shared conventions are added, update this file to describe the actual
cross-service architecture rather than restating what's discoverable from the code or duplicating
`spec/OVERVIEW.md`.

## Naming conventions (binding — see spec/README.md §Convenciones)

- Module/repo folder: singular + `-service` (`customer-service`, `product-service`,
  `inventory-service`, `order-service`) — matches the module already on disk.
- Java root package: `com.shop.<service>.service` (e.g. `com.shop.product.service`) — matches
  `customer-service`'s existing `com.shop.customer.service`. This deviates intentionally from
  `spec/OVERVIEW.md`'s original `com.ecommerce.<service>` reference (see decision D-3 there).
- Business/bounded-context names stay plural in prose (Customers, Products, Inventory, Orders) —
  that's domain language, separate from the technical module name above.

## Build and test (customer-service)

Run all commands from the `customer-service/` directory using the Maven wrapper:

```bash
./mvnw compile          # compile
./mvnw test              # run all tests
./mvnw test -Dtest=CustomerServiceApplicationTests   # run a single test class
./mvnw spring-boot:run   # run the service locally
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

## Notes

- Lombok is on the classpath (annotation processing wired into both `compile` and `test-compile` executions of `maven-compiler-plugin`).
- The Spring Boot Maven plugin explicitly excludes Lombok from the repackaged runtime jar.
- `spring.application.name=customer-service` is the only configured property so far (`src/main/resources/application.properties`).
- The parent POM pins **Spring Boot 3.5.16**. Use the 3.x starter artifact IDs: `spring-boot-starter-web` and `spring-boot-starter-test`. Do NOT use `spring-boot-starter-webmvc` / `spring-boot-starter-webmvc-test` — those are the Spring Boot 4.x renamed starters and don't exist in the 3.5.x BOM, which breaks the build with a "missing dependency version" error. If this project is intentionally upgraded to Spring Boot 4.x later, the parent `<version>` and these starter names must be changed together.
