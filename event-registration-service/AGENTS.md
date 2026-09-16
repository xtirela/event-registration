# event-registration-service

Event registration microservice. Owns registration lifecycle, status transitions, and fan-out coordination.

## Role in architecture

- CRUD for registrations (create, update status, cancel)
- Feign calls: `EventClient` (check event capacity, get event details), `ParticipantClient` (validate participant)
- Status machine: PENDING → ACCEPTED / REJECTED / WAITLIST / CANCELLED
- Internal API: `InternalEventRegistrationController` for service-to-service calls

## Commands

- Build: `./gradlew build`
- Test: `./gradlew test`
- Single test: `./gradlew test --tests "com.eventreg.eventregistrationservice.service.EventRegistrationServiceImplTest"`
- Checkstyle: `./gradlew checkstyleMain checkstyleTest`
- Auto-format: `./gradlew spotlessApply`
- Run: `./gradlew bootRun`

## Port

8083

## Dependencies

- PostgreSQL: `event_registration_service_db` on port 5436
- Vault: `localhost:8200` (secrets: DB creds)
- Keycloak: JWT validation (`localhost:9090/realms/oauth`)
- Feign: event-service (8082), participant-service (8081)

## K8s (Module 7)

- Deployment: replicas, liveness/readiness probes via `/actuator/health`, resource limits
- Service: ClusterIP, DNS `registration-service`
- ConfigMap: service URLs (event-service, participant-service), timeouts
- Secret: DB credentials, Vault token
- Exposes: `/actuator/prometheus` for Prometheus scraping

## Key patterns

- `@Idempotent` + `IdempotencyAspect` for write operations
- Circuit Breaker + Retry for Feign calls to event-service and participant-service
- Fan-out: registration creation triggers parallel calls to event-service and participant-service — visible in Jaeger traces
- JPA + Liquibase migrations in `src/main/resources/db/changelog/`
- OpenAPI docs at `/swagger-ui.html`
