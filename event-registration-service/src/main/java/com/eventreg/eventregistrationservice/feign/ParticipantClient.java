package com.eventreg.eventregistrationservice.feign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Feign client for calls to the participant service. */
@FeignClient(name = "participant-service", url = "${participant.service.url:http://localhost:8081}")
public interface ParticipantClient {
  @CircuitBreaker(name = "participant-service-circuit-breaker")
  @Retry(name = "participant-service-retry")
  @GetMapping("/internal/participants/{participantId}/keycloakId")
  String getKeycloakId(@PathVariable Long participantId);
}
