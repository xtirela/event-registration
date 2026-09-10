# AGENTS.md

Spring Boot monolith for event registration. PostgreSQL + Liquibase, Spring Security with JWT, OpenAPI/Swagger.

## Commands

- Build: `./gradlew build`
- Tests: `./gradlew test`
- Single test: `./gradlew test --tests "com.eventreg.service.EventServiceImplTest"`
- Checkstyle: `./gradlew checkstyleMain checkstyleTest`
- Auto-format: `./gradlew spotlessApply` (Google Java Format)
- Run locally: `./gradlew bootRun`

## CI Order

checkstyle → compile → test → jacoco report

## Docker

- Services: `docker compose up -d` (PostgreSQL on 5432, pgAdmin on 5050)
- App: `docker compose run --rm eventreg` (interactive, not `up`)
- Build image: `docker compose build`

## Key Facts

- Java 25 toolchain (not 21)
- `gradle.properties` in .gitignore — machine-specific, don't commit
- `.gitattributes` enforces LF on Java/Gradle/XML, but git core.autocrlf=true may cause issues — run `spotlessApply` before large changes
- `data/` directory in .gitignore — runtime state, never commit
- Liquibase migrations in `src/main/resources/db/changelog/changes/` — numbered SQL files
- Main class: `com.eventreg.PartyApplication`

## Architecture

- Controllers → Services (interface + impl) → JPA Repositories
- DTOs: MapStruct mappers, Lombok @Builder
- Security: JWT filter, roles/permissions in `model.enums.RBAC`
- Idempotency: `@Idempotent` annotation + AOP aspect
- Waiting queue: `WaitingQueueScheduler` (scheduled task)
- Swagger annotations in `annotation/swagger/` package

## Testing

- JUnit 5 + Testcontainers (PostgreSQL)
- Integration tests: `src/test/java/com/eventreg/integration/`
- Unit tests: `src/test/java/com/eventreg/controller/unit/`
- Load tests: `src/test/java/com/eventreg/integration/load/`
- Concurrency tests: `src/test/java/com/eventreg/integration/concurrency/`

## Conventions

- No `System.out` — use SLF4J logger
- Exceptions extend `EventRegException` (RuntimeException)
- Validation throws exceptions, not DTOs
- Keep service interfaces clean — implementations hold state
