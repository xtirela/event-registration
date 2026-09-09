package com.eventreg.eventservice.feign;

import com.eventreg.eventservice.dto.response.EventResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "notification-service",
    url = "${notification.service.url:http://localhost:8084}")
public interface NotificationClient {
  @CircuitBreaker(name = "notification-service-circuit-breaker")
  @PostMapping("/internal/notifications/event-created")
  void sendEventCreated(
      @RequestBody EventResponse eventResponse, @RequestParam("toEmail") String toEmail);
}
