# AGENTS.md

Консольное приложение — сервис записи на мероприятие. Этап «Модуль 1» roadmap
java_roadmap (DTO Baeldung-стиля, Lombok @Builder, JUnit 5). Следующий этап — «Модуль 2»:
Repository Layer, хранение O(1) по ключу, лист ожидания (Queue), отмена последнего
действия (Stack), Stream API, иерархия исключений, GitHub Actions, JaCoCo.

## Команды
- Сборка / тесты:            `./gradlew build`, `./gradlew test`
- Один тест:                 `./gradlew test --tests "service.EventServiceImplTest"`
- Стиль:                     `./gradlew checkstyleMain checkstyleTest`
- Формат (авто):             `./gradlew spotlessApply`   (Spotless googleJavaFormat)
- Запуск меню:               `./gradlew run`   (mainClass = view.Main; standardInput = System.in)

## Особенности настройки
- Toolchain в build.gradle — Java 25, а не 21 из roadmap.
- gradle.properties в .gitignore: задаёт машинозависимый tmpdir (/home/spiteset/tmp)
  и включает configuration-cache. Не коммить его; на другой машине создаётся свой.
- Файлы в CRLF + core.autocrlf=true. Перед большими правками запускай spotlessApply,
  чтобы Spotless/Checkstyle не тонули в переносах строк.

## Архитектура (состояние «Модуль 1» — цель «Модуля 2»)
- Слои: view (ConsoleView, Main) → service (EventService / EventServiceImpl) → in-memory.
- Хранилища пока НЕТ: EventServiceImpl держит SimpleHashMap<Integer,...> напрямую. Цель
  «Модуля 2» — вынести Repository Layer, чтобы сервисы работали только через него.
- GitHub Actions workflow и плагин JaCoCo ещё не настроены (их добавляет «Модуль 2»:
  CI-джобы checkstyle/build/test + отчёт по покрытию). Checkstyle берёт
  google_checks.xml из архива checkstyle-плагина (локального конфиг-файла нет).
- Валидация возвращает DTO с текстом ошибки, а не исключения; «Модуль 2» вводит
  иерархию исключений (EntityNotFound / Duplicate / CapacityExceeded).

## Соглашения
- Маппинг model → DTO через рукописные builder-методы (eventToEventResponse и т.п.);
  DTO — Lombok @Builder. Сервисы зависят от интерфейса; реализация хранит состояние.
- Тесты: JUnit 5 (junit-bom 5.10); unit-тесты в src/test/java/service и view
  (ConsoleViewTest гоняет Scanner через ByteArrayInputStream).