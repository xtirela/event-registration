package com.eventreg.eventregistrationservice.service.implementation;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import com.eventreg.eventregistrationservice.dto.request.create.EventRegistrationCreateRequest;
import com.eventreg.eventregistrationservice.feign.EventClient;
import com.eventreg.eventregistrationservice.mapper.EventRegistrationMapper;
import com.eventreg.eventregistrationservice.model.EventRegistration;
import com.eventreg.eventregistrationservice.model.enums.EventRegistrationStatus;
import com.eventreg.eventregistrationservice.repository.EventRegistrationRepository;
import com.eventreg.eventregistrationservice.service.EventRegistrationService;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import feign.Feign;
import feign.jackson.JacksonEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

@WireMockTest(httpPort = 8082)
public class EventRegistrationServiceImplFeignTests {

  private EventRegistrationService eventRegistrationService;
  private EventRegistrationRepository eventRegistrationRepository;
  private EventRegistrationMapper eventRegistrationMapper;

  @BeforeEach
  void setUp(WireMockRuntimeInfo wireMockRuntimeInfo) {
    WireMock.reset();

    eventRegistrationRepository = Mockito.mock(EventRegistrationRepository.class);
    eventRegistrationMapper = Mockito.mock(EventRegistrationMapper.class);

    EventClient eventClient =
        Feign.builder()
            .contract(new SpringMvcContract())
            .encoder(new JacksonEncoder())
            .target(EventClient.class, wireMockRuntimeInfo.getHttpBaseUrl());

    eventRegistrationService =
        new EventRegistrationServiceImpl(
            eventRegistrationRepository, eventRegistrationMapper, eventClient);

    Mockito.when(eventRegistrationRepository.save(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
  }

  @Test
  void shouldReturnEventRegistrationAccepted_whenDecisionIsAccepted() {
    stubFor(
        get(urlEqualTo("/internal/events/1/registration-decision"))
            .willReturn(
                ok().withHeader("Content-Type", "application/json")
                    .withBody("ACCEPTED;description")));

    EventRegistrationCreateRequest eventRegistrationCreateRequest =
        EventRegistrationCreateRequest.builder()
            .participantId(1L)
            .eventId(1L)
            .skipPending(false)
            .build();

    EventRegistration eventRegistrationResult =
        eventRegistrationService.createEventRegistration(eventRegistrationCreateRequest);

    assertEquals(
        EventRegistrationStatus.ACCEPTED, eventRegistrationResult.getEventRegistrationStatus());

    verify(getRequestedFor((urlEqualTo("/internal/events/1/registration-decision"))));
  }

  @Test
  void shouldReturnEventRegistrationAccepted_whenDecisionIsPendingAndIsSkipPending() {
    stubFor(
        get(urlEqualTo("/internal/events/1/registration-decision"))
            .willReturn(
                ok().withHeader("Content-Type", "application/json")
                    .withBody("PENDING;description")));

    EventRegistrationCreateRequest eventRegistrationCreateRequest =
        EventRegistrationCreateRequest.builder()
            .participantId(1L)
            .eventId(1L)
            .skipPending(true)
            .build();

    EventRegistration eventRegistrationResult =
        eventRegistrationService.createEventRegistration(eventRegistrationCreateRequest);

    assertEquals(
        EventRegistrationStatus.ACCEPTED, eventRegistrationResult.getEventRegistrationStatus());

    verify(getRequestedFor((urlEqualTo("/internal/events/1/registration-decision"))));
  }
}
