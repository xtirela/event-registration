package com.eventreg.eventregistrationservice.feign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Feign client for calls to the event service. */
@FeignClient(name = "event-service", url = "${event.service.url:http://localhost:8082}")
public interface EventClient {
  @CircuitBreaker(name = "event-service-circuit-breaker")
  @Retry(name = "event-service-retry")
  @GetMapping("/internal/events/{eventId}/keycloakId")
  String getOrganizerKeycloakId(@PathVariable Long eventId);

  @CircuitBreaker(name = "event-service-circuit-breaker")
  @Retry(name = "event-service-retry")
  @GetMapping("/internal/events/{eventId}/registration-decision")
  String getRegistrationDecision(@PathVariable Long eventId);
}
