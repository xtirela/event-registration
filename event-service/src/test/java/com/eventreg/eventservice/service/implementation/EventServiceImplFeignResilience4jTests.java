package com.eventreg.eventservice.service.implementation;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.eventreg.eventservice.dto.response.EventResponse;
import com.eventreg.eventservice.feign.EventRegistrationClient;
import com.eventreg.eventservice.feign.NotificationClient;
import com.eventreg.eventservice.repository.EventRepository;
import com.eventreg.eventservice.repository.IdempotencyRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@EnableFeignClients
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableAutoConfiguration(
    exclude = {
      DataSourceAutoConfiguration.class,
      HibernateJpaAutoConfiguration.class,
      LiquibaseAutoConfiguration.class,
      SecurityAutoConfiguration.class
    })
class EventServiceImplFeignResilience4jTests {

  @MockitoBean private IdempotencyRepository idempotencyRepository;

  @MockitoBean private EventRepository eventRepository;

  @MockitoBean private JwtDecoder jwtDecoder;

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().port(9999)).build();

  @Autowired private EventRegistrationClient eventRegistrationClient;

  @Autowired private NotificationClient notificationClient;

  @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;

  @BeforeEach
  void setUp() {
    wireMock.resetAll();
    circuitBreakerRegistry.circuitBreaker("event-registration-service-circuit-breaker").reset();
    circuitBreakerRegistry.circuitBreaker("notification-service-circuit-breaker").reset();
  }

  @Test
  void shouldRetryEventRegistrationClient_whenFirstCallFails() {
    wireMock.stubFor(
        get(urlEqualTo("/internal/registrations/accepted/1"))
            .inScenario("retry-event-registration")
            .whenScenarioStateIs(STARTED)
            .willReturn(serverError())
            .willSetStateTo("SECOND_ATTEMPT"));

    wireMock.stubFor(
        get(urlEqualTo("/internal/registrations/accepted/1"))
            .inScenario("retry-event-registration")
            .whenScenarioStateIs("SECOND_ATTEMPT")
            .willReturn(ok().withHeader("Content-Type", "application/json").withBody("5")));

    Long result = eventRegistrationClient.getCurrentParticipantAmountForEventWithStatusAccepted(1L);

    assertEquals(5L, result);
    wireMock.verify(2, getRequestedFor(urlEqualTo("/internal/registrations/accepted/1")));
  }

  @Test
  void shouldOpenEventRegistrationCircuitBreaker_whenFailuresExceedThreshold() {
    wireMock.stubFor(
        get(urlEqualTo("/internal/registrations/accepted/1")).willReturn(serverError()));

    CircuitBreaker circuitBreaker =
        circuitBreakerRegistry.circuitBreaker("event-registration-service-circuit-breaker");

    assertCircuitOpensAfterMinFailures(
        circuitBreaker,
        () -> eventRegistrationClient.getCurrentParticipantAmountForEventWithStatusAccepted(1L));
  }

  @Test
  void shouldOpenNotificationCircuitBreaker_whenFailuresExceedThreshold() {
    wireMock.stubFor(
        post(urlEqualTo("/internal/notifications/event-created?toEmail=test%40test.test"))
            .willReturn(serverError()));

    CircuitBreaker circuitBreaker =
        circuitBreakerRegistry.circuitBreaker("notification-service-circuit-breaker");

    assertCircuitOpensAfterMinFailures(
        circuitBreaker,
        () ->
            notificationClient.sendEventCreated(EventResponse.builder().build(), "test@test.test"));
  }

  private void assertCircuitOpensAfterMinFailures(CircuitBreaker circuitBreaker, Executable call) {
    int minCalls = circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls();
    for (int i = 0; i < minCalls; i++) {
      assertThrows(RuntimeException.class, call);
    }

    assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

    int servedBeforeBlocked = wireMock.getAllServeEvents().size();
    assertThrows(CallNotPermittedException.class, call);
    assertEquals(servedBeforeBlocked, wireMock.getAllServeEvents().size());
  }
}
