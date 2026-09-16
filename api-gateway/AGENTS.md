# api-gateway

API gateway. Single entry point, routes requests, enforces auth, handles retries and circuit breakers.

## Role in architecture

- Spring Cloud Gateway (WebMVC) — routes external traffic to internal services
- JWT validation at gateway level — downstream services receive validated tokens
- Retry + Circuit Breaker per route (participant, event, registration)
- Fallback endpoints when services are down

## Routes

| Path | Target | Rewrite |
|---|---|---|
| `/register`, `/login` | user-service:8085 | — |
| `/participants/**` | participant-service:8081 | `/api/participants/**` |
| `/events/**` | event-service:8082 | `/api/events/**` |
| `/registrations/**` | event-registration-service:8083 | `/api/registrations/**` |

## Commands

- Build: `./gradlew build`
- Test: `./gradlew test`
- Checkstyle: `./gradlew checkstyleMain checkstyleTest`
- Auto-format: `./gradlew spotlessApply`
- Run: `./gradlew bootRun`

## Port

8080

## Dependencies

- Keycloak: JWT validation (`localhost:9090/realms/oauth`)
- Downstream: participant-service (8081), event-service (8082), event-registration-service (8083), user-service (8085)

## K8s (Module 7)

- Deployment: replicas, liveness/readiness probes via `/actuator/health`, resource limits
- Service: ClusterIP, DNS `api-gateway`
- Ingress: NGINX Ingress — external entry point for all traffic
- ConfigMap: downstream service URLs (K8s DNS names), route config
- Secret: Keycloak client secret
- Exposes: `/actuator/prometheus`, `/actuator/circuitbreakers` for Prometheus

## Key patterns

- Circuit Breaker per route with fallback endpoints
- Retry with exponential backoff (4 retries, 100ms→1s)
- JWT decoded once at gateway — downstream services read claims, don't re-validate
- OpenAPI: not applicable (gateway doesn't serve business API docs)
