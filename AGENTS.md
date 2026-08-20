# AGENTS.md

Консольное приложение — сервис записи на мероприятие (консольный интерфейс для организатора).
roadmap java_roadmap пройден до «Модуля 3» (Docker, GHCR, SLF4J+Logback). Текущий этап —
«Модуль 4»: перевести хранилище с in-memory на PostgreSQL (JDBC + Liquibase + Testcontainers)
без перехода на Spring. Интерфейс репозитория менять нельзя — меняется только реализация.

## Команды
- Сборка / тесты:            `./gradlew build`, `./gradlew test`
- Один тест:                 `./gradlew test --tests "service.EventServiceImplTest"`
- Стиль:                     `./gradlew checkstyleMain checkstyleTest`
- Формат (авто):             `./gradlew spotlessApply`   (Spotless googleJavaFormat)
- Запуск меню (локально):    `./gradlew run`   (mainClass = view.Main; standardInput = System.in)

## Docker
- Собрать: `docker compose build`; поднять: `docker compose up -d`.
- Приложение ИНТЕРАКТИВНОЕ, а `docker compose up` не пробрасывает stdin. Для ввода с клавиатуры
  запускать через `docker compose run --rm eventreg` (выделяет TTY + stdin).
- Dockerfile: multi-stage + jlink (jlink обязан иметь `--add-modules`, иначе падает).
- Named volume `eventreg-data` монтируется в `/app/data` — туда приложение пишет CSV
  (application.properties). Менять таргет нельзя.
- Образ публикуется в GHCR через `.github/workflows/docker-build-push.yaml` (push в main/dev,
  логин через `secrets.GITHUB_TOKEN`, packages: write).

## Архитектура (состояние «Модуль 3» — цель «Модуля 4»)
- Слои: view (ConsoleView, Main) → service (EventService / EventServiceImpl) → repository (интерфейсы
  + impl). Хранилище пока in-memory: `SimpleHashMap`/`SimpleLinkedList`/`SimpleArrayList` из пакета
  `collection` (свои реализации стандартных коллекций).
- 3 доменные сущности: `Event`, `Participant`, `EventRegistration` (+ enum-статусы в `model.enums`).
- Цель «Модуля 4» — заменить in-memory репозитории на `JdbcXxxRepository` (PreparedStatement, без
  ORM), схему — через Liquibase (уже лежит в `src/main/resources/db/changelog/db.changelog.sql`),
  интеграционные тесты — на Testcontainers (реальная PostgreSQL, без H2).
- Иерархия исключений: `EventRegException` (RuntimeException) + `EventNotFoundException`,
  `ParticipantNotFoundException`, `DuplicateException`, `EventCapacityExceededException`,
  `RegistrationNotFoundException`, `IllegalArgumentEventRegException`.
- Валидация бросает исключения (не возвращает DTO с текстом ошибки).

## Особенности настройки
- Toolchain в build.gradle — Java 25, а не 21 из roadmap (и в CI тоже 25).
- gradle.properties в .gitignore: задаёт машинозависимый tmpdir (/home/spiteset/tmp)
  и включает configuration-cache. Не коммить его; на другой машине создаётся свой.
- Файлы в CRLF + core.autocrlf=true. Перед большими правками запускай spotlessApply,
  чтобы Spotless/Checkstyle не тонули в переносах строк.
- `data/` (CSV из модуля 2) в .gitignore — не коммитить рантайм-состояние.

## Postgres MCP
- MCP-сервер `postgres-mcp` (crystaldba/postgres-mcp, запущен как Docker-контейнер) подключён к
  live-БД приложения: контейнер `eventreg-postgres`, БД `eventreg_db`, localhost:5432,
  postgres/postgres.
- Инструменты: `execute_sql`, `explain_query`, `analyze_query_indexes`, `analyze_workload_indexes`,
  `analyze_db_health`, `get_top_queries`, `get_object_details`, `list_objects`, `list_schemas`,
  `list_mcp_resources`.
- Назначение — ad-hoc инспекция схемы/данных, анализ планов запросов и индексов, поиск корня
  проблем (id-последовательности, FK, состояние таблиц). НЕ использовать для проверки тестов:
  тесты гоняют свежий Testcontainers `postgres:18`, а не эту БД.

## Соглашения
- Маппинг model → DTO через рукописные builder-методы (eventToEventResponse,
  participantToParticipantResponse); DTO — Lombok @Builder. Сервисы зависят от интерфейса,
  реализация держит состояние (для модуля 4 — через репозиторий).
- Логирование через SLF4J (logback.xml, уровни INFO/WARN/ERROR) — `System.out` в коде не допускается.
- Тесты: JUnit 5 (junit-bom 5.10); unit-тесты в src/test/java/{service,view,collection,repository};
  ConsoleViewTest гоняет Scanner через ByteArrayInputStream.