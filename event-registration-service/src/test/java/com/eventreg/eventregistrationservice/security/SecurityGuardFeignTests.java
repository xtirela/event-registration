package com.eventreg.eventregistrationservice.security;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

import com.eventreg.eventregistrationservice.feign.EventClient;
import com.eventreg.eventregistrationservice.feign.ParticipantClient;
import com.eventreg.eventregistrationservice.model.EventRegistration;
import com.eventreg.eventregistrationservice.repository.EventRegistrationRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import feign.Feign;
import feign.jackson.JacksonEncoder;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class SecurityGuardFeignTests {
  private SecurityGuard securityGuard;
  private EventRegistrationRepository eventRegistrationRepository;

  @RegisterExtension
  static WireMockExtension eventService =
      WireMockExtension.newInstance().options(wireMockConfig().port(8082)).build();

  @RegisterExtension
  static WireMockExtension participantService =
      WireMockExtension.newInstance().options(wireMockConfig().port(8081)).build();

  @BeforeEach
  void setUp() {

    eventService.resetAll();
    participantService.resetAll();

    Jwt jwt =
        Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .subject("a1b2c3d4-e5f6-7890-abcd-ef1234567890") // ← keycloakId
            .claim("preferred_username", "kudzip")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
    SecurityContextHolder.getContext().setAuthentication(authentication);

    EventClient eventClient =
        Feign.builder()
            .contract(new SpringMvcContract())
            .encoder(new JacksonEncoder())
            .target(EventClient.class, "http://localhost:8082");

    ParticipantClient participantClient =
        Feign.builder()
            .contract(new SpringMvcContract())
            .encoder(new JacksonEncoder())
            .target(ParticipantClient.class, "http://localhost:8081");

    eventRegistrationRepository = Mockito.mock(EventRegistrationRepository.class);

    securityGuard = new SecurityGuard(eventClient, participantClient, eventRegistrationRepository);
  }

  @Test
  void shouldReturnTrue_whenParticipantIsOwnerOfRegistration() {
    participantService.stubFor(
        get(urlEqualTo("/internal/participants/1/keycloakId"))
            .willReturn(ok().withBody("a1b2c3d4-e5f6-7890-abcd-ef1234567890")));

    boolean result = securityGuard.isParticipantOwner(1L);

    assertTrue(result);
    participantService.verify(getRequestedFor(urlEqualTo("/internal/participants/1/keycloakId")));
  }

  @Test
  void shouldReturnTrue_whenIsOwnerForRegistration() {

    EventRegistration registration =
        EventRegistration.builder().id(1L).eventId(10L).participantId(1L).build();

    Mockito.when(eventRegistrationRepository.findById(1L))
        .thenReturn(java.util.Optional.of(registration));

    participantService.stubFor(
        get(urlEqualTo("/internal/participants/1/keycloakId"))
            .willReturn(ok().withBody("a1b2c3d4-e5f6-7890-abcd-ef1234567890")));

    boolean result = securityGuard.isOwnerForRegistration(1L);

    assertTrue(result);
    participantService.verify(getRequestedFor(urlEqualTo("/internal/participants/1/keycloakId")));
  }

  @Test
  void shouldReturnFalse_whenIsNotOwnerForRegistration() {

    EventRegistration registration =
        EventRegistration.builder().id(1L).eventId(10L).participantId(1L).build();

    Mockito.when(eventRegistrationRepository.findById(1L))
        .thenReturn(java.util.Optional.of(registration));

    participantService.stubFor(
        get(urlEqualTo("/internal/participants/1/keycloakId"))
            .willReturn(ok().withBody("z2y3x4w5-e5f6-7890-abcd-ef1234567890")));

    boolean result = securityGuard.isOwnerForRegistration(1L);

    assertFalse(result);
    participantService.verify(getRequestedFor(urlEqualTo("/internal/participants/1/keycloakId")));
  }

  @Test
  void shouldReturnTrue_whenIsEventOrganizerForRegistration() {

    EventRegistration registration =
        EventRegistration.builder().id(1L).eventId(10L).participantId(1L).build();

    Mockito.when(eventRegistrationRepository.findById(1L))
        .thenReturn(java.util.Optional.of(registration));

    eventService.stubFor(
        get(urlEqualTo("/internal/events/10/keycloakId"))
            .willReturn(ok().withBody("a1b2c3d4-e5f6-7890-abcd-ef1234567890")));

    boolean result = securityGuard.isEventOrganizerForRegistration(1L);

    assertTrue(result);
    eventService.verify(getRequestedFor(urlEqualTo("/internal/events/10/keycloakId")));
  }

  @Test
  void shouldReturnFalse_whenIsNotEventOrganizerForRegistration() {

    EventRegistration registration =
        EventRegistration.builder().id(1L).eventId(10L).participantId(1L).build();

    Mockito.when(eventRegistrationRepository.findById(1L))
        .thenReturn(java.util.Optional.of(registration));

    eventService.stubFor(
        get(urlEqualTo("/internal/events/10/keycloakId"))
            .willReturn(ok().withBody("z2y3x4w5-e5f6-7890-abcd-ef1234567890")));

    boolean result = securityGuard.isEventOrganizerForRegistration(1L);

    assertFalse(result);
    eventService.verify(getRequestedFor(urlEqualTo("/internal/events/10/keycloakId")));
  }
}
