# user-service

User authentication microservice. Keycloak integration for registration and login.

## Role in architecture

- `POST /register` — creates user in Keycloak, returns JWT
- `POST /login` — authenticates via Keycloak, returns JWT
- No database — all user data lives in Keycloak
- Circuit Breaker wraps Keycloak calls

## Commands

- Build: `./gradlew build`
- Test: `./gradlew test`
- Checkstyle: `./gradlew checkstyleMain checkstyleTest`
- Auto-format: `./gradlew spotlessApply`
- Run: `./gradlew bootRun`

## Port

8085

## Dependencies

- Keycloak: `localhost:9090` (realm: `oauth`, client: `myclient`)
- Vault: `localhost:8200` (secrets: Keycloak client secret)
- No database (stateless)

## K8s (Module 7)

- Deployment: replicas, liveness/readiness probes via `/actuator/health`, resource limits
- Service: ClusterIP, DNS `user-service`
- ConfigMap: Keycloak base URL, realm, client ID
- Secret: Keycloak client secret
- Exposes: `/actuator/prometheus` for Prometheus scraping

## Key patterns

- Circuit Breaker for Keycloak Admin API calls
- No JPA — no Liquibase migrations
- OpenAPI docs at `/swagger-ui.html`
