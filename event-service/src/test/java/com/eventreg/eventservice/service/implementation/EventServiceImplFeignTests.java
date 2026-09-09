package com.eventreg.eventservice.service.implementation;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.eventreg.eventservice.dto.request.create.EventCreateRequest;
import com.eventreg.eventservice.dto.response.EventResponse;
import com.eventreg.eventservice.feign.EventRegistrationClient;
import com.eventreg.eventservice.feign.NotificationClient;
import com.eventreg.eventservice.mapper.EventMapper;
import com.eventreg.eventservice.model.Event;
import com.eventreg.eventservice.model.enums.EventGenderRequirement;
import com.eventreg.eventservice.model.enums.EventReservationStatus;
import com.eventreg.eventservice.model.enums.EventStatus;
import com.eventreg.eventservice.repository.EventRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

public class EventServiceImplFeignTests {

  private EventServiceImpl eventService;
  private EventRepository eventRepository;
  private EventMapper eventMapper;

  @RegisterExtension
  static WireMockExtension eventRegistrationService =
      WireMockExtension.newInstance().options(wireMockConfig().port(8083)).build();

  @RegisterExtension
  static WireMockExtension notificationService =
      WireMockExtension.newInstance().options(wireMockConfig().port(8084)).build();

  @BeforeEach
  void setUp() {

    notificationService.resetAll();
    eventRegistrationService.resetAll();

    eventRepository = Mockito.mock(EventRepository.class);
    eventMapper = Mockito.mock(EventMapper.class);

    Mockito.when(eventRepository.save(Mockito.any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    Mockito.when(eventRepository.tryAcquireSeat(Mockito.any())).thenReturn(1);

    Mockito.when(eventMapper.eventCreateRequestToEvent(Mockito.any(), Mockito.anyString()))
        .thenAnswer(
            inv ->
                Event.builder()
                    .eventName(inv.getArgument(0, EventCreateRequest.class).getEventName())
                    .build());
    Mockito.when(eventMapper.eventToEventResponse(Mockito.any())).thenReturn(new EventResponse());

    EventRegistrationClient eventRegistrationClient =
        Feign.builder()
            .contract(new SpringMvcContract())
            .decoder(new JacksonDecoder())
            .encoder(new JacksonEncoder())
            .target(EventRegistrationClient.class, "http://localhost:8083");

    NotificationClient notificationClient =
        Feign.builder()
            .contract(new SpringMvcContract())
            .encoder(new JacksonEncoder())
            .target(NotificationClient.class, "http://localhost:8084");

    eventService =
        new EventServiceImpl(
            eventRepository, eventMapper, eventRegistrationClient, notificationClient);
  }

  @Test
  void shouldReturnReservationsOpen_whenParticipantAmountBelowMax() {
    eventRegistrationService.stubFor(
        get(urlEqualTo("/internal/registrations/accepted/1"))
            .willReturn(ok().withHeader("Content-Type", "application/json").withBody("5")));

    Event event =
        Event.builder()
            .id(1L)
            .eventName("Test Event")
            .eventDate(OffsetDateTime.now().plusDays(7))
            .eventDurationMinutes(120)
            .location("Moscow")
            .ageRequired(0)
            .eventGenderRequirement(EventGenderRequirement.NONE)
            .maxParticipantAmount(10)
            .eventStatus(EventStatus.PLANNED)
            .eventReservationStatus(EventReservationStatus.RESERVATIONS_OPEN)
            .confirmationRequired(false)
            .waitlistWhenAllReserved(false)
            .organizerKeycloakId("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            .build();

    EventReservationStatus result = eventService.updateCurrentEventReservationStatus(event);

    assertEquals(EventReservationStatus.RESERVATIONS_OPEN, result);
    eventRegistrationService.verify(
        getRequestedFor(urlEqualTo("/internal/registrations/accepted/1")));
  }

  @Test
  void shouldReturnAllReserved_whenParticipantAmountAboveMax() {
    eventRegistrationService.stubFor(
        get(urlEqualTo("/internal/registrations/accepted/1")).willReturn(ok().withBody("10")));

    Event event =
        Event.builder()
            .id(1L)
            .eventName("Test Event")
            .eventDate(OffsetDateTime.now().plusDays(7))
            .eventDurationMinutes(120)
            .location("Moscow")
            .ageRequired(0)
            .eventGenderRequirement(EventGenderRequirement.NONE)
            .maxParticipantAmount(10)
            .eventStatus(EventStatus.PLANNED)
            .eventReservationStatus(EventReservationStatus.RESERVATIONS_OPEN)
            .confirmationRequired(false)
            .waitlistWhenAllReserved(false)
            .organizerKeycloakId("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            .build();

    EventReservationStatus result = eventService.updateCurrentEventReservationStatus(event);
    assertEquals(EventReservationStatus.ALL_RESERVED, result);
    eventRegistrationService.verify(
        getRequestedFor(urlEqualTo("/internal/registrations/accepted/1")));
  }

  @Test
  void shouldReturnWaitlist_whenParticipantAmountAboveMaxAndWaitlistWhenAllReserved() {
    eventRegistrationService.stubFor(
        get(urlEqualTo("/internal/registrations/waiting/1")).willReturn(ok().withBody("0")));
    eventRegistrationService.stubFor(
        get(urlEqualTo("/internal/registrations/accepted/1")).willReturn(ok().withBody("10")));

    Event event =
        Event.builder()
            .id(1L)
            .eventName("Test Event")
            .eventDate(OffsetDateTime.now().plusDays(7))
            .eventDurationMinutes(120)
            .location("Moscow")
            .ageRequired(0)
            .eventGenderRequirement(EventGenderRequirement.NONE)
            .maxParticipantAmount(10)
            .eventStatus(EventStatus.PLANNED)
            .eventReservationStatus(EventReservationStatus.RESERVATIONS_OPEN)
            .confirmationRequired(false)
            .waitlistWhenAllReserved(true)
            .organizerKeycloakId("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            .build();

    EventReservationStatus result = eventService.updateCurrentEventReservationStatus(event);
    assertEquals(EventReservationStatus.WAITLIST, result);
    eventRegistrationService.verify(
        getRequestedFor(urlEqualTo("/internal/registrations/waiting/1")));
  }

  @Test
  void shouldSendNotification_whenEventCreated() throws InterruptedException {
    notificationService.stubFor(
        post(urlEqualTo("/internal/notifications/event-created?toEmail=test%40test.test"))
            .willReturn(ok()));

    EventCreateRequest request =
        EventCreateRequest.builder()
            .eventName("Tech Conference")
            .eventDescription("Annual tech meetup")
            .eventDate(OffsetDateTime.now().plusDays(7))
            .eventDurationMinutes(120)
            .location("Moscow, Tverskaya 1")
            .ageRequired(18)
            .eventGenderRequirement(EventGenderRequirement.NONE)
            .maxParticipantAmount(100)
            .confirmationRequired(false)
            .waitlistWhenAllReserved(true)
            .build();

    eventService.createEvent(request, "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "test@test.test");

    Thread.sleep(500);

    notificationService.verify(
        postRequestedFor(
            urlEqualTo("/internal/notifications/event-created?toEmail=test%40test.test")));
  }

  @Test
  void shouldPromoteWaitingQueue_whenWaitingQueueNotEmpty() {
    eventRegistrationService.stubFor(
        get(urlPathEqualTo("/internal/registrations/1/waiting-queue"))
            .withQueryParam("limit", equalTo("100"))
            .willReturn(
                ok().withHeader("Content-Type", "application/json").withBody("[ 1, 2, 3 ]")));

    eventRegistrationService.stubFor(
        post(urlPathEqualTo("/internal/registrations/promote"))
            .withQueryParam("eventId", equalTo("1"))
            .withQueryParam("status", equalTo("ACCEPTED"))
            .withQueryParam("description", equalTo("waiting spot freed up"))
            .willReturn(ok()));

    Event event =
        Event.builder()
            .id(1L)
            .eventName("Test Event")
            .eventDate(OffsetDateTime.now().plusDays(7))
            .eventDurationMinutes(120)
            .location("Moscow")
            .ageRequired(0)
            .eventGenderRequirement(EventGenderRequirement.NONE)
            .maxParticipantAmount(10)
            .eventStatus(EventStatus.PLANNED)
            .eventReservationStatus(EventReservationStatus.RESERVATIONS_OPEN)
            .confirmationRequired(false)
            .waitlistWhenAllReserved(false)
            .organizerKeycloakId("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            .build();

    Mockito.when(eventRepository.findById(Mockito.any())).thenReturn(Optional.ofNullable(event));

    eventService.promoteWaitingQueue(1L, 100);

    eventRegistrationService.verify(
        postRequestedFor(urlPathEqualTo("/internal/registrations/promote"))
            .withQueryParam("eventId", equalTo("1"))
            .withQueryParam("status", equalTo("ACCEPTED"))
            .withQueryParam("description", equalTo("waiting spot freed up"))
            .withRequestBody(containing("[ 1, 2, 3 ]")));

    eventRegistrationService.verify(
        getRequestedFor(urlPathEqualTo("/internal/registrations/1/waiting-queue"))
            .withQueryParam("limit", equalTo("100")));
  }
}
