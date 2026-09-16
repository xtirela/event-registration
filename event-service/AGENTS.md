# event-service

Event management microservice. Owns event lifecycle, waiting queue, address suggestions via Dadata.

## Role in architecture

- CRUD for events (create, update, delete, search with filtering/pagination)
- Waiting queue: `WaitingQueueScheduler` reconciles registrations when spots open (60s interval)
- Feign calls: `EventRegistrationClient` (counts, waiting queue, promote), `NotificationClient` (email on event creation)
- Internal API: `InternalEventController` for service-to-service calls

## Commands

- Build: `./gradlew build`
- Test: `./gradlew test`
- Single test: `./gradlew test --tests "com.eventreg.eventservice.service.EventServiceImplTest"`
- Checkstyle: `./gradlew checkstyleMain checkstyleTest`
- Auto-format: `./gradlew spotlessApply`
- Run: `./gradlew bootRun`

## Port

8082

## Dependencies

- PostgreSQL: `event_service_db` on port 5435
- Vault: `localhost:8200` (secrets: DB creds, Dadata token)
- Keycloak: JWT validation (`localhost:9090/realms/oauth`)
- Feign: event-registration-service (8083), notification-service (8084)

## K8s (Module 7)

- Deployment: replicas, liveness/readiness probes via `/actuator/health`, resource limits
- Service: ClusterIP, DNS `event-service`
- ConfigMap: service URLs (registration-service, notification-service), Dadata token
- Secret: DB credentials, Vault token
- Exposes: `/actuator/prometheus` for Prometheus scraping

## Key patterns

- `@Idempotent` + `IdempotencyAspect` for write operations
- Circuit Breaker + Retry for Feign calls to registration-service and notification-service
- JPA + Liquibase migrations in `src/main/resources/db/changelog/`
- OpenAPI docs at `/swagger-ui.html`
