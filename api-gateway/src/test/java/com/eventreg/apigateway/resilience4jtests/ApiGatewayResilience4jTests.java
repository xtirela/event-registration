package com.eventreg.apigateway.resilience4jtests;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eventreg.apigateway.config.SecurityConfig;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.server.mvc.config.FilterProperties;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.config.PredicateProperties;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayResilience4jTests {

  @MockitoBean private SecurityConfig securityConfig;

  @LocalServerPort private int port;

  @Autowired private GatewayMvcProperties gatewayMvcProperties;

  @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().port(9999)).build();

  private RestClient client;

  @BeforeEach
  void setUp() {
    wireMock.resetAll();
    gatewayMvcProperties.getRoutes().stream()
        .map(this::circuitBreakerId)
        .filter(Objects::nonNull)
        .forEach(id -> circuitBreakerRegistry.circuitBreaker(id).reset());
    client = RestClient.builder().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void shouldRetryEventRoute_whenFirstCallFails() {
    ServiceSpec spec = specOf("event");
    stubRetryScenario(spec.stubPath());

    ResponseEntity<String> response = call(spec.requestPath());

    assertEquals(200, response.getStatusCode().value());
    wireMock.verify(2, getRequestedFor(urlEqualTo(spec.stubPath())));
  }

  @Test
  void shouldRetryParticipantRoute_whenFirstCallFails() {
    ServiceSpec spec = specOf("participant");
    stubRetryScenario(spec.stubPath());

    ResponseEntity<String> response = call(spec.requestPath());

    assertEquals(200, response.getStatusCode().value());
    wireMock.verify(2, getRequestedFor(urlEqualTo(spec.stubPath())));
  }

  @Test
  void shouldRetryRegistrationRoute_whenFirstCallFails() {
    ServiceSpec spec = specOf("registration");
    stubRetryScenario(spec.stubPath());

    ResponseEntity<String> response = call(spec.requestPath());

    assertEquals(200, response.getStatusCode().value());
    wireMock.verify(2, getRequestedFor(urlEqualTo(spec.stubPath())));
  }

  @Test
  void shouldFallBackForEventRoute_whenCircuitIsOpen() {
    ServiceSpec spec = specOf("event");
    assertFallbackWhenOpen(spec, "event");
  }

  @Test
  void shouldFallBackForParticipantRoute_whenCircuitIsOpen() {
    ServiceSpec spec = specOf("participant");
    assertFallbackWhenOpen(spec, "participant");
  }

  @Test
  void shouldFallBackForRegistrationRoute_whenCircuitIsOpen() {
    ServiceSpec spec = specOf("registration");
    assertFallbackWhenOpen(spec, "registration");
  }

  private void assertFallbackWhenOpen(ServiceSpec spec, String service) {
    wireMock.stubFor(get(urlEqualTo(spec.stubPath())).willReturn(ok()));
    circuitBreakerRegistry.circuitBreaker(spec.circuitBreakerId()).transitionToOpenState();
    int hitsBefore = wireMock.getAllServeEvents().size();

    ResponseEntity<String> response = call(spec.requestPath());

    assertEquals(503, response.getStatusCode().value());
    assertTrue(response.getBody().contains("\"service\":\"" + service + "\""));
    assertTrue(response.getBody().contains("\"title\":\"service-down\""));
    assertEquals(
        hitsBefore,
        wireMock.getAllServeEvents().size(),
        "downstream must not be hit while the circuit is open");
  }

  private ResponseEntity<String> call(String path) {
    return client
        .get()
        .uri(path)
        .exchange(
            (request, response) ->
                ResponseEntity.status(response.getStatusCode())
                    .body(
                        response.getBody() == null
                            ? null
                            : new String(
                                response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
  }

  private void stubRetryScenario(String stubPath) {
    String scenario = "retry" + stubPath;
    wireMock.stubFor(
        get(urlEqualTo(stubPath))
            .inScenario(scenario)
            .whenScenarioStateIs(STARTED)
            .willReturn(serverError())
            .willSetStateTo("SECOND_ATTEMPT"));
    wireMock.stubFor(
        get(urlEqualTo(stubPath))
            .inScenario(scenario)
            .whenScenarioStateIs("SECOND_ATTEMPT")
            .willReturn(ok()));
  }

  private ServiceSpec specOf(String routeId) {
    RouteProperties route =
        gatewayMvcProperties.getRoutes().stream()
            .filter(r -> routeId.equals(r.getId()))
            .findFirst()
            .orElseThrow();
    String requestPath = pathPattern(route).replace("**", "1");
    return new ServiceSpec(
        requestPath,
        applyRewrite(requestPath, route),
        Objects.requireNonNull(circuitBreakerId(route)));
  }

  private String pathPattern(RouteProperties route) {
    return route.getPredicates().stream()
        .filter(p -> "Path".equals(p.getName()))
        .map(PredicateProperties::getArgs)
        .map(args -> args.values().iterator().next())
        .findFirst()
        .orElseThrow();
  }

  private String applyRewrite(String requestPath, RouteProperties route) {
    FilterProperties rewrite = filter(route, "RewritePath");
    if (rewrite == null) {
      return requestPath;
    }
    return requestPath.replaceAll(
        rewrite.getArgs().get("regexp"), rewrite.getArgs().get("replacement"));
  }

  private String circuitBreakerId(RouteProperties route) {
    FilterProperties circuitBreaker = filter(route, "CircuitBreaker");
    return circuitBreaker == null ? null : circuitBreaker.getArgs().get("id");
  }

  private FilterProperties filter(RouteProperties route, String name) {
    return route.getFilters().stream()
        .filter(f -> name.equals(f.getName()))
        .findFirst()
        .orElse(null);
  }

  private record ServiceSpec(String requestPath, String stubPath, String circuitBreakerId) {}

  @TestConfiguration
  static class PermitAllSecurityTestConfig {

    @Bean
    @Order(0)
    SecurityFilterChain permitAllSecurityFilterChain(HttpSecurity http) throws Exception {
      return http.csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .build();
    }
  }
}
