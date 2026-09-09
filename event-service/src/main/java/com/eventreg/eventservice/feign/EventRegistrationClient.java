package com.eventreg.eventservice.feign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "event-registration-service",
    url = "${event.registration.service.url:http://localhost:8083}")
public interface EventRegistrationClient {
  @CircuitBreaker(name = "event-registration-service-circuit-breaker")
  @Retry(name = "event-registration-service-retry")
  @GetMapping("/internal/registrations/waiting/{eventId}")
  Long getCurrentParticipantAmountForEventWithStatusWaiting(@PathVariable Long eventId);

  @CircuitBreaker(name = "event-registration-service-circuit-breaker")
  @Retry(name = "event-registration-service-retry")
  @GetMapping("/internal/registrations/accepted/{eventId}")
  Long getCurrentParticipantAmountForEventWithStatusAccepted(@PathVariable Long eventId);

  @CircuitBreaker(name = "event-registration-service-circuit-breaker")
  @Retry(name = "event-registration-service-retry")
  @GetMapping("/internal/registrations/{eventId}/waiting-queue")
  List<Long> getWaitingQueue(
      @PathVariable Long eventId, @RequestParam(value = "limit", defaultValue = "100") int limit);

  @CircuitBreaker(name = "event-registration-service-circuit-breaker")
  @PostMapping("/internal/registrations/promote")
  void promoteWaitingQueue(
      @RequestParam("eventId") Long eventId,
      @RequestParam("status") String status,
      @RequestParam("description") String description,
      @RequestBody List<Long> registrationIds);
}
