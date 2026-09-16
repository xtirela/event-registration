# notification-service

Notification microservice. Sends emails via Resend, generates ICS calendar files.

## Role in architecture

- Email notifications (event creation, registration status changes)
- ICS file generation for calendar invites
- Stateless: no database, no JPA
- Internal API: `InternalNotificationController` called by other services via Feign

## Commands

- Build: `./gradlew build`
- Test: `./gradlew test`
- Checkstyle: `./gradlew checkstyleMain checkstyleTest`
- Auto-format: `./gradlew spotlessApply`
- Run: `./gradlew bootRun`

## Port

8084

## Dependencies

- Vault: `localhost:8200` (secrets: Resend API key)
- No database (stateless)
- Resend API for email delivery

## K8s (Module 7)

- Deployment: replicas, liveness/readiness probes via `/actuator/health`, resource limits
- Service: ClusterIP, DNS `notification-service`
- ConfigMap: SMTP config, feature flags
- Secret: Resend API key
- Exposes: `/actuator/prometheus` for Prometheus scraping

## Key patterns

- Circuit Breaker for outbound email calls (Resend API)
- No JPA — no Liquibase migrations
- OpenAPI docs at `/swagger-ui.html`
