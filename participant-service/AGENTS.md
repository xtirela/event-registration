# participant-service

Participant management microservice. Owns participant profiles and gender-based filtering.

## Role in architecture

- CRUD for participants (create, update, delete, list with pagination)
- Internal API: `InternalParticipantController` for service-to-service calls (e.g., event-service queries participant details)

## Commands

- Build: `./gradlew build`
- Test: `./gradlew test`
- Single test: `./gradlew test --tests "com.eventreg.participantservice.service.ParticipantServiceImplTest"`
- Checkstyle: `./gradlew checkstyleMain checkstyleTest`
- Auto-format: `./gradlew spotlessApply`
- Run: `./gradlew bootRun`

## Port

8081

## Dependencies

- PostgreSQL: `participant_service_db` on port 5434
- Vault: `localhost:8200` (secrets: DB creds)
- Keycloak: JWT validation (`localhost:9090/realms/oauth`)

## K8s (Module 7)

- Deployment: replicas, liveness/readiness probes via `/actuator/health`, resource limits
- Service: ClusterIP, DNS `participant-service`
- ConfigMap: feature flags
- Secret: DB credentials, Vault token
- Exposes: `/actuator/prometheus` for Prometheus scraping

## Key patterns

- `@Idempotent` + `IdempotencyAspect` for write operations
- JPA + Liquibase migrations in `src/main/resources/db/changelog/`
- OpenAPI docs at `/swagger-ui.html`
