package com.eventreg.eventregistrationservice.service.implementation;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.eventreg.eventregistrationservice.feign.EventClient;
import com.eventreg.eventregistrationservice.feign.ParticipantClient;
import com.eventreg.eventregistrationservice.repository.EventRegistrationRepository;
import com.eventreg.eventregistrationservice.repository.IdempotencyRepository;
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
class EventRegistrationServiceImplFeignResilience4jTests {

  @MockitoBean private IdempotencyRepository idempotencyRepository;

  @MockitoBean private EventRegistrationRepository eventRegistrationRepository;

  @MockitoBean private JwtDecoder jwtDecoder;

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().port(9999)).build();

  @Autowired private EventClient eventClient;

  @Autowired private ParticipantClient participantClient;

  @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;

  @BeforeEach
  void setUp() {
    wireMock.resetAll();
    circuitBreakerRegistry.circuitBreaker("event-service-circuit-breaker").reset();
    circuitBreakerRegistry.circuitBreaker("participant-service-circuit-breaker").reset();
  }

  @Test
  void shouldRetryEventClient_whenFirstCallFails() {

    wireMock.stubFor(
        get(urlEqualTo("/internal/events/1/registration-decision"))
            .inScenario("retry-event")
            .whenScenarioStateIs(STARTED)
            .willReturn(serverError())
            .willSetStateTo("SECOND_ATTEMPT"));

    wireMock.stubFor(
        get(urlEqualTo("/internal/events/1/registration-decision"))
            .inScenario("retry-event")
            .whenScenarioStateIs("SECOND_ATTEMPT")
            .willReturn(ok().withBody("ACCEPTED;description")));

    String result = eventClient.getRegistrationDecision(1L);

    assertEquals("ACCEPTED;description", result);
    wireMock.verify(2, getRequestedFor(urlEqualTo("/internal/events/1/registration-decision")));
  }

  @Test
  void shouldRetryParticipantClient_whenFirstCallFails() {
    wireMock.stubFor(
        get(urlEqualTo("/internal/participants/1/keycloakId"))
            .inScenario("retry-participant")
            .whenScenarioStateIs(STARTED)
            .willReturn(serverError())
            .willSetStateTo("SECOND_ATTEMPT"));

    wireMock.stubFor(
        get(urlEqualTo("/internal/participants/1/keycloakId"))
            .inScenario("retry-participant")
            .whenScenarioStateIs("SECOND_ATTEMPT")
            .willReturn(ok().withBody("a1b2c3d4-e5f6-7890-abcd-ef1234567890")));

    String result = participantClient.getKeycloakId(1L);

    assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", result);
    wireMock.verify(2, getRequestedFor(urlEqualTo("/internal/participants/1/keycloakId")));
  }

  @Test
  void shouldOpenEventCircuitBreaker_whenFailuresExceedThreshold() {
    wireMock.stubFor(
        get(urlEqualTo("/internal/events/1/registration-decision")).willReturn(serverError()));

    CircuitBreaker circuitBreaker =
        circuitBreakerRegistry.circuitBreaker("event-service-circuit-breaker");

    assertCircuitOpensAfterMinFailures(
        circuitBreaker, () -> eventClient.getRegistrationDecision(1L));
  }

  @Test
  void shouldOpenParticipantCircuitBreaker_whenFailuresExceedThreshold() {
    wireMock.stubFor(
        get(urlEqualTo("/internal/participants/1/keycloakId")).willReturn(serverError()));

    CircuitBreaker circuitBreaker =
        circuitBreakerRegistry.circuitBreaker("participant-service-circuit-breaker");

    assertCircuitOpensAfterMinFailures(circuitBreaker, () -> participantClient.getKeycloakId(1L));
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
