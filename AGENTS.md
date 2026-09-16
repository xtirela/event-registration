# AGENTS.md

<<<<<<< HEAD
<<<<<<< HEAD
Микросервисный проект «Модуль 6». Legacy-монолит из корня **удалён** — нет `src/`, корневых
`build.gradle`/`settings.gradle`/`gradlew`/`gradle.properties`. Репозиторий — 6 независимых
Spring Boot сервисов. Работаем строго внутри каталога каждого сервиса.

## Ключевой факт: НЕ Gradle multi-module билд

- Общего многопроектного билда **нет**. Каждый сервис — самостоятельный Gradle-проект со своим
  `build.gradle`, wrapper'ом и `gradlew`. Сборка только изнутри каталога:
  `(cd event-service && ./gradlew build)`. Корневого `./gradlew` не существует.
- Исключения: `notification-service` не поднимает Keycloak; auth у всех — через Keycloak
  (см. ниже).

## Сервисы и их настройка

| Сервис | Порт | БД (PostgreSQL из compose) | Роль |
|---|---|---|---|
| `api-gateway` | 8080 | — | маршруты → participant/event/registration, Retry+CircuitBreaker |
| `participant-service` | 8081 | `5434/participant_service_db` | участники |
| `event-service` | 8082 | `5435/event_service_db` | события, принятие решений, waiting-queue |
| `event-registration-service` | 8083 | `5436/event_registration_service_db` | регистрации, promotion |
| `notification-service` | 8084 | — | email через Resend |
| `user-service` | 8085 | — | auth-фасад над Keycloak: `POST /register`, `POST /login` |

Все сервисы подключают Vault (localhost:8200, dev-mode `vault server -dev`, корневой токен
`${VAULT_TOKEN:-root-token}` из `.env`) и, кроме `notification-service`, Keycloak (issuer
`http://localhost:9090/realms/oauth`), БД — через datasource + liquibase + `ddl-auto: validate`.
Данные для datasource (`spring.datasource.username/password`), API-ключи и клиентские секреты тоже
приходят из Vault. `user-service` — **без БД и без хранения паролей**: register/login
делегируются Keycloak (клиент `myclient`: Direct Access Grants для `password` grant, service
account с ролью `manage-users` для Admin API; секрет — `secret/user-service/keycloak.client-secret`
в Vault). Рега → Keycloak создаёт юзера и назначает роль `ROLE_PARTICIPANT` или `ROLE_ORGANISER`
(composite в реалме).

### Vault: provisioning и секреты

- Vault в compose — dev (KV v2 на `secret/` включён, unsealed, in-memory). Корневой токен —
  `VAULT_TOKEN` из `.env` (дефолт `root-token`).
- One-shot `vault-seed` (compose, `depends_on: vault: service_healthy`) пишет секреты из `.env` в
  KV v2 `secret/<application.name>` на каждый `up`. Приложения ждут его через
  `service_completed_successfully`.
- Пути: `secret/notification-service/resend.api-key`, `secret/user-service/keycloak.client-secret`,
  `secret/event-service` (`dadata.token`, `spring.datasource.username/password`),
  `secret/event-registration-service` и `secret/participant-service` (datasource creds).
- `.env` в .gitignore (не коммитить). `KEYCLOAK_CLIENT_SECRET` и `DADATA_TOKEN` заполняются вручную.

## Состояние сборки (проверено)

- `event-service`, `participant-service`, `event-registration-service`, `user-service`,
  `api-gateway`: **компилируются**, `spotlessApply` + `checkstyleMain` зелёные (полная сборка
  user-service — `./gradlew build`).
- Ранее `user-service` «не компилировался» (старые пакеты `com.eventreg.security.*`) — заменён
  новым auth-фасадом (см. выше). Были чинимы: у `api-gateway` отсутствовала декларация
  `checkstyleConfig(...)` в `build.gradle` (checkstyle падал с «Expected file collection to contain
  exactly one file») — добавлена.
- Тесты — smoke `*ApplicationTests` (поднятие контекста; требуют поднятого стека compose) и
  api-контрактные тесты с WireMock (`EventServiceImplFeignTests`,
  `EventRegistrationServiceImplFeignTests`, `EventRegistrationServiceImplFeignResilience4jTests`,
  `SecurityGuardFeignTests`, `ApiGatewayResilience4jTests`).

## Идемпотентность (важно, новый паттерн)

В 3 сервисах (`event`, `participant`, `event-registration`) — одинаковый аспект
`IdempotencyAspect` + `@Idempotent` + таблица `idempotency_keys` (ключ = PK).

Паттерн **claim-first** (исправляет TOCTOU):

1. `IdempotencyRepository.claim(key, method, path)` — нативный
   `INSERT ... ON CONFLICT (key) DO NOTHING` (`http_status` пишется плейсхолдером `'OK'`,
   `response_body` = NULL).
2. Выигравший выполняет бизнес-логику, затем `findById` + проставляет `http_status`/`response_body`
   и `save()`. Если бизнес-логика бросила — claim удаляется (`deleteById`), ретрай перезапустится.
3. Проигравший (`claim()` вернул 0) опрашивает строку каждые 100 мс до 3 с и возвращает сохранённый
   ответ. `response_body == NULL` ⟺ обработка ещё идёт. Таймаут / несовпадение method+path → 409.

Известный потолок (marked `ponytail:` в коде): если победитель умрёт в процессе, claim без
`response_body` зависнет и ретраи получат 409 (апгрейд — TTL-джоба); sleep-poll — busy-wait
(апгрейд — DB WAITFOR/outbox).

## Кросс-сервисные потоки и решения

- `event-registration-service` → Feign к `event-service` (`getRegistrationDecision`,
  `getOrganizerKeycloakId`) и `participant-service` (`getKeycloakId`, для SecurityGuard).
  Решение по регистрации принимает **event-service** (CAS `tryAcquireSeat`).
- `event-service` → Feign к `event-registration-service` (счётчики, `getWaitingQueue`,
  `promoteWaitingQueue`) и к `notification-service` (email при создании события).
- Сходимость состояния (`места ↔ регистрации`, promotion WAITING→ACCEPTED) — через 60-сек
  `WaitingQueueScheduler` (reconcile-поллер). **Outbox осознанно НЕ нужен** — один сайд-эффект
  (email) и уже существующий руками сделанный поллер; см. анализ.
- Email при создании события — fire-and-forget `CompletableFuture` в `EventServiceImpl.createEvent`
  (задеференшено комментарием `ponytail:`; при нагрузке — executor + retry).

## Что устарело/сломано (не спотыкайся)

- **CI `.github/workflows/build.yaml` и `docker-build-push.yaml`** гоняют корневой `./gradlew` и
  корневой контекст — сломаны (в корне нет wrapper). Не проверяют сервисы.
- **`docker-compose.yaml`**: приложения собираются в контейнеры из per-service Dockerfile
  (`build: ./<service>`); БД трёх сервисов (5434/5435/5436), keycloak (9090, БД 5433), vault (8200,
  dev + one-shot `vault-seed`), pgadmin (5050). `user-service` в compose отсутствует — поднимается
  локально из IDE/`./gradlew bootRun` и не маршрутизируется из шлюза.
- `user-service` — auth-фасад без БД; `notification-service` не поднимает Keycloak.

## Настройка / особенности

- `gradle.properties` (корневой и per-service) в .gitignore — машинозависимый tmpdir +
  configuration-cache. Не коммить.
- `.gitattributes` задаёт `eol=lf` для java/gradle/xml/properties.
- Перед большими правками Java запускай `./gradlew spotlessApply` (googleJavaFormat), затем
  `checkstyleMain checkstyleTest` — иначе Checkstyle тонет в переносах строк.
- Валидация бросает исключения, а не возвращает DTO с текстом ошибки.
- Секреты — только в Vault (см. раздел «Vault: provisioning и секреты») или env из `.env`; в
  application.yml — плейсхолдеры `${...}`, без хардкода значений. Не дублировать, не выносить в логи.

## Postgres MCP / базы

- `postgres-mcp` подключён к live-БД в compose: контейнеры `*-postgres`, БД
  `*_service_db` на 5434/5435/5436, postgres/postgres, localhost.
- Назначение — ad-hoc инспекция схемы/данных/планов/индексов. Тесты к этим live-БД не подключаются.
=======
Spring Boot monolith for event registration. PostgreSQL + Liquibase, Spring Security with JWT, OpenAPI/Swagger.

=======
Spring Boot monolith for event registration. PostgreSQL + Liquibase, Spring Security with JWT, OpenAPI/Swagger.

>>>>>>> develop
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
<<<<<<< HEAD
>>>>>>> e940c8e (feat(infra): fixed agents md)
=======
>>>>>>> develop
