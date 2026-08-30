package com.eventreg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventreg.dto.request.create.EventRegistrationCreateRequest;
import com.eventreg.exception.EventNotFoundException;
import com.eventreg.exception.EventRegistrationNotFoundException;
import com.eventreg.exception.ParticipantNotFoundException;
import com.eventreg.mapper.EventRegistrationMapper;
import com.eventreg.model.Event;
import com.eventreg.model.EventRegistration;
import com.eventreg.model.Participant;
import com.eventreg.model.enums.EventRegistrationStatus;
import com.eventreg.model.enums.EventReservationStatus;
import com.eventreg.model.enums.ParticipantGender;
import com.eventreg.repository.EventRegistrationRepository;
import com.eventreg.service.implementation.EmailService;
import com.eventreg.service.implementation.EventRegistrationServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Unit tests for {@link EventRegistrationServiceImpl}. Collaborating {@link EventService} and
 * {@link ParticipantService} are mocked; the {@link EventRegistrationRepository} is mocked and the
 * {@link EventRegistrationMapper} is a real MapStruct instance (unused by the service logic).
 */
@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class EventRegistrationServiceImplTest {

  @Mock private EventRegistrationRepository eventRegistrationRepository;

  @Mock private ParticipantService participantService;

  @Mock private EventService eventService;

  @Mock private EmailService emailService;

  private EventRegistrationMapper eventRegistrationMapper;

  private EventRegistrationServiceImpl eventRegistrationService;

  @BeforeEach
  void setUp() {
    eventRegistrationMapper = Mappers.getMapper(EventRegistrationMapper.class);
    eventRegistrationService =
        new EventRegistrationServiceImpl(
            eventRegistrationRepository,
            participantService,
            eventService,
            eventRegistrationMapper,
            emailService);
  }

  private Event event(long id) {
    return Event.builder()
        .id(id)
        .eventName("Tech Conference")
        .maxParticipantAmount(10)
        .currentParticipantAmount(5)
        .eventStatus(com.eventreg.model.enums.EventStatus.PLANNED)
        .eventReservationStatus(EventReservationStatus.RESERVATIONS_OPEN)
        .confirmationRequired(false)
        .build();
  }

  private Participant participant(long id) {
    return Participant.builder()
        .id(id)
        .firstName("John")
        .lastName("Doe")
        .age(25)
        .participantGender(ParticipantGender.MALE)
        .user(
            com.eventreg.model.User.builder()
                .id(1L)
                .username("u")
                .email("u@x.com")
                .password("password123")
                .build())
        .build();
  }

  private EventRegistrationCreateRequest createRequest(
      Long eventId, Long participantId, boolean skip) {
    return EventRegistrationCreateRequest.builder()
        .eventId(eventId)
        .participantId(participantId)
        .skipPending(skip)
        .build();
  }

  private void stubSaveReturnsArgument() {
    when(eventRegistrationRepository.save(any(EventRegistration.class)))
        .thenAnswer(inv -> inv.getArgument(0));
  }

  // ---------- createEventRegistration ----------

  @Test
  void givenOpenEventWhenCreateRegistrationThenReturnsAcceptedStatus() {
    Event event = event(1L);
    Participant participant = participant(2L);
    when(eventService.findById(1L)).thenReturn(event);
    when(participantService.findById(2L)).thenReturn(participant);
    when(eventService.updateCurrentEventReservationStatus(event))
        .thenReturn(EventReservationStatus.RESERVATIONS_OPEN);
    stubSaveReturnsArgument();

    EventRegistration result =
        eventRegistrationService.createEventRegistration(createRequest(1L, 2L, false));

    assertThat(result.getEventRegistrationStatus()).isEqualTo(EventRegistrationStatus.ACCEPTED);
  }

  @Test
  void givenOpenEventWhenCreateRegistrationThenReturnsAcceptedDescription() {
    Event event = event(1L);
    Participant participant = participant(2L);
    when(eventService.findById(1L)).thenReturn(event);
    when(participantService.findById(2L)).thenReturn(participant);
    when(eventService.updateCurrentEventReservationStatus(event))
        .thenReturn(EventReservationStatus.RESERVATIONS_OPEN);
    stubSaveReturnsArgument();

    EventRegistration result =
        eventRegistrationService.createEventRegistration(createRequest(1L, 2L, false));

    assertThat(result.getDescription()).isEqualTo("event registration accepted");
  }

  @Test
  void givenConfirmationRequiredEventWhenCreateRegistrationThenReturnsPendingStatus() {
    Event event = event(1L);
    Participant participant = participant(2L);
    when(eventService.findById(1L)).thenReturn(event);
    when(participantService.findById(2L)).thenReturn(participant);
    when(eventService.updateCurrentEventReservationStatus(event))
        .thenReturn(EventReservationStatus.CONFIRMATION_REQUIRED);
    stubSaveReturnsArgument();

    EventRegistration result =
        eventRegistrationService.createEventRegistration(createRequest(1L, 2L, false));

    assertThat(result.getEventRegistrationStatus()).isEqualTo(EventRegistrationStatus.PENDING);
  }

  @Test
  void givenWaitlistEventWhenCreateRegistrationThenReturnsWaitingStatus() {
    Event event = event(1L);
    Participant participant = participant(2L);
    when(eventService.findById(1L)).thenReturn(event);
    when(participantService.findById(2L)).thenReturn(participant);
    when(eventService.updateCurrentEventReservationStatus(event))
        .thenReturn(EventReservationStatus.WAITLIST);
    stubSaveReturnsArgument();

    EventRegistration result =
        eventRegistrationService.createEventRegistration(createRequest(1L, 2L, false));

    assertThat(result.getEventRegistrationStatus()).isEqualTo(EventRegistrationStatus.WAITING);
  }

  @Test
  void givenAllReservedEventWhenCreateRegistrationThenReturnsDeniedStatus() {
    Event event = event(1L);
    Participant participant = participant(2L);
    when(eventService.findById(1L)).thenReturn(event);
    when(participantService.findById(2L)).thenReturn(participant);
    when(eventService.updateCurrentEventReservationStatus(event))
        .thenReturn(EventReservationStatus.ALL_RESERVED);
    stubSaveReturnsArgument();

    EventRegistration result =
        eventRegistrationService.createEventRegistration(createRequest(1L, 2L, false));

    assertThat(result.getEventRegistrationStatus()).isEqualTo(EventRegistrationStatus.DENIED);
  }

  @Test
  void givenClosedEventWhenCreateRegistrationThenReturnsDeniedStatus() {
    Event event = event(1L);
    Participant participant = participant(2L);
    when(eventService.findById(1L)).thenReturn(event);
    when(participantService.findById(2L)).thenReturn(participant);
    when(eventService.updateCurrentEventReservationStatus(event))
        .thenReturn(EventReservationStatus.RESERVATIONS_CLOSED);
    stubSaveReturnsArgument();

    EventRegistration result =
        eventRegistrationService.createEventRegistration(createRequest(1L, 2L, false));

    assertThat(result.getEventRegistrationStatus()).isEqualTo(EventRegistrationStatus.DENIED);
  }

  @Test
  void givenPendingRegistrationWithSkipPendingWhenCreateRegistrationThenReturnsAcceptedStatus() {
    Event event = event(1L);
    Participant participant = participant(2L);
    when(eventService.findById(1L)).thenReturn(event);
    when(participantService.findById(2L)).thenReturn(participant);
    when(eventService.updateCurrentEventReservationStatus(event))
        .thenReturn(EventReservationStatus.CONFIRMATION_REQUIRED);
    stubSaveReturnsArgument();

    EventRegistration result =
        eventRegistrationService.createEventRegistration(createRequest(1L, 2L, true));

    assertThat(result.getEventRegistrationStatus()).isEqualTo(EventRegistrationStatus.ACCEPTED);
  }

  @Test
  void givenValidRequestWhenCreateRegistrationThenPersistsRegistration() {
    Event event = event(1L);
    Participant participant = participant(2L);
    when(eventService.findById(1L)).thenReturn(event);
    when(participantService.findById(2L)).thenReturn(participant);
    when(eventService.updateCurrentEventReservationStatus(event))
        .thenReturn(EventReservationStatus.RESERVATIONS_OPEN);
    stubSaveReturnsArgument();

    eventRegistrationService.createEventRegistration(createRequest(1L, 2L, false));

    verify(eventRegistrationRepository).save(any(EventRegistration.class));
  }

  @Test
  void givenMissingEventWhenCreateRegistrationThenThrowsEventNotFoundException() {
    when(eventService.findById(1L)).thenThrow(new EventNotFoundException(1L, "findById"));

    org.junit.jupiter.api.Assertions.assertThrows(
        EventNotFoundException.class,
        () -> eventRegistrationService.createEventRegistration(createRequest(1L, 2L, false)));
  }

  @Test
  void givenMissingParticipantWhenCreateRegistrationThenThrowsParticipantNotFoundException() {
    Event event = event(1L);
    when(eventService.findById(1L)).thenReturn(event);
    when(participantService.findById(2L))
        .thenThrow(new ParticipantNotFoundException(2L, "findById"));

    org.junit.jupiter.api.Assertions.assertThrows(
        ParticipantNotFoundException.class,
        () -> eventRegistrationService.createEventRegistration(createRequest(1L, 2L, false)));
  }

  // ---------- deleteEventRegistration ----------

  @Test
  void givenExistingRegistrationWhenDeleteRegistrationThenDeletesRegistration() {
    EventRegistration existing =
        EventRegistration.builder()
            .id(1L)
            .event(event(1L))
            .participant(participant(2L))
            .eventRegistrationStatus(EventRegistrationStatus.WAITING)
            .build();
    when(eventRegistrationRepository.findById(1L)).thenReturn(Optional.of(existing));

    eventRegistrationService.deleteEventRegistration(1L);

    verify(eventRegistrationRepository).delete(existing);
  }

  @Test
  void
      givenMissingRegistrationWhenDeleteRegistrationThenThrowsEventRegistrationNotFoundException() {
    when(eventRegistrationRepository.findById(7L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        EventRegistrationNotFoundException.class,
        () -> eventRegistrationService.deleteEventRegistration(7L));
  }

  // ---------- findById ----------

  @Test
  void givenExistingRegistrationWhenFindByIdThenReturnsRegistration() {
    EventRegistration existing =
        EventRegistration.builder()
            .id(1L)
            .event(event(1L))
            .participant(participant(2L))
            .eventRegistrationStatus(EventRegistrationStatus.ACCEPTED)
            .build();
    when(eventRegistrationRepository.findById(1L)).thenReturn(Optional.of(existing));

    EventRegistration result = eventRegistrationService.findById(1L);

    assertThat(result.getId()).isEqualTo(1L);
  }

  @Test
  void givenMissingRegistrationWhenFindByIdThenThrowsEventRegistrationNotFoundException() {
    when(eventRegistrationRepository.findById(7L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        EventRegistrationNotFoundException.class, () -> eventRegistrationService.findById(7L));
  }

  // ---------- changeRegistrationRequestStatus ----------

  @Test
  void givenExistingRegistrationWhenChangeStatusThenReturnsUpdatedStatus() {
    EventRegistration existing =
        EventRegistration.builder()
            .id(1L)
            .event(event(1L))
            .participant(participant(2L))
            .eventRegistrationStatus(EventRegistrationStatus.WAITING)
            .build();
    when(eventRegistrationRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(eventService.updateCurrentEventReservationStatus(existing.getEvent()))
        .thenReturn(EventReservationStatus.RESERVATIONS_OPEN);

    EventRegistration result =
        eventRegistrationService.changeRegistrationRequestStatus(
            1L, EventRegistrationStatus.ACCEPTED, "approved");

    assertThat(result.getEventRegistrationStatus()).isEqualTo(EventRegistrationStatus.ACCEPTED);
  }

  @Test
  void givenExistingRegistrationWhenChangeStatusThenReturnsUpdatedDescription() {
    EventRegistration existing =
        EventRegistration.builder()
            .id(1L)
            .event(event(1L))
            .participant(participant(2L))
            .eventRegistrationStatus(EventRegistrationStatus.WAITING)
            .build();
    when(eventRegistrationRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(eventService.updateCurrentEventReservationStatus(existing.getEvent()))
        .thenReturn(EventReservationStatus.RESERVATIONS_OPEN);

    EventRegistration result =
        eventRegistrationService.changeRegistrationRequestStatus(
            1L, EventRegistrationStatus.ACCEPTED, "approved");

    assertThat(result.getDescription()).isEqualTo("approved");
  }

  @Test
  void givenRegistrationWithoutEventWhenChangeStatusThenDoesNotCallEventService() {
    EventRegistration existing =
        EventRegistration.builder()
            .id(1L)
            .participant(participant(2L))
            .eventRegistrationStatus(EventRegistrationStatus.WAITING)
            .build();
    when(eventRegistrationRepository.findById(1L)).thenReturn(Optional.of(existing));

    eventRegistrationService.changeRegistrationRequestStatus(
        1L, EventRegistrationStatus.ACCEPTED, "approved");

    verify(eventService, never()).updateCurrentEventReservationStatus(any(Event.class));
  }

  @Test
  void givenMissingRegistrationWhenChangeStatusThenThrowsEventRegistrationNotFoundException() {
    when(eventRegistrationRepository.findById(7L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        EventRegistrationNotFoundException.class,
        () ->
            eventRegistrationService.changeRegistrationRequestStatus(
                7L, EventRegistrationStatus.ACCEPTED, "approved"));
  }

  // ---------- updateWaitingQueueForEvent ----------

  @Test
  void givenWaitingRegistrationAndSpaceWhenUpdateQueueThenPromotesToAccepted() {
    Event event = event(1L);
    EventRegistration waiting =
        EventRegistration.builder()
            .id(1L)
            .event(event)
            .participant(participant(2L))
            .eventRegistrationStatus(EventRegistrationStatus.WAITING)
            .description("none")
            .build();
    when(eventService.updateCurrentEventReservationStatus(event))
        .thenReturn(EventReservationStatus.RESERVATIONS_OPEN);
    when(eventRegistrationRepository.findWaitingQueue(eq(1L), anyInt()))
        .thenReturn(List.of(waiting));

    eventRegistrationService.updateWaitingQueueForEvent(event);

    assertThat(waiting.getEventRegistrationStatus()).isEqualTo(EventRegistrationStatus.ACCEPTED);
  }

  @Test
  void givenWaitingRegistrationAndSpaceWhenUpdateQueueThenSetsFreedUpDescription() {
    Event event = event(1L);
    EventRegistration waiting =
        EventRegistration.builder()
            .id(1L)
            .event(event)
            .participant(participant(2L))
            .eventRegistrationStatus(EventRegistrationStatus.WAITING)
            .description("none")
            .build();
    when(eventService.updateCurrentEventReservationStatus(event))
        .thenReturn(EventReservationStatus.RESERVATIONS_OPEN);
    when(eventRegistrationRepository.findWaitingQueue(eq(1L), anyInt()))
        .thenReturn(List.of(waiting));

    eventRegistrationService.updateWaitingQueueForEvent(event);

    assertThat(waiting.getDescription()).isEqualTo("waiting spot freed up");
  }

  @Test
  void givenWaitlistEventAndSpaceWhenUpdateQueueThenPromotesWaitingToAccepted() {
    Event event = event(1L);
    EventRegistration waiting =
        EventRegistration.builder()
            .id(1L)
            .event(event)
            .participant(participant(2L))
            .eventRegistrationStatus(EventRegistrationStatus.WAITING)
            .description("none")
            .build();
    when(eventService.updateCurrentEventReservationStatus(event))
        .thenReturn(EventReservationStatus.WAITLIST);
    when(eventRegistrationRepository.findWaitingQueue(eq(1L), anyInt()))
        .thenReturn(List.of(waiting));

    eventRegistrationService.updateWaitingQueueForEvent(event);

    assertThat(waiting.getEventRegistrationStatus()).isEqualTo(EventRegistrationStatus.ACCEPTED);
  }

  @Test
  void givenFullEventWhenUpdateQueueThenKeepsRegistrationWaiting() {
    Event event = event(1L);
    EventRegistration waiting =
        EventRegistration.builder()
            .id(1L)
            .event(event)
            .participant(participant(2L))
            .eventRegistrationStatus(EventRegistrationStatus.WAITING)
            .description("none")
            .build();
    when(eventService.updateCurrentEventReservationStatus(event))
        .thenReturn(EventReservationStatus.ALL_RESERVED);
    when(eventRegistrationRepository.findWaitingQueue(eq(1L), anyInt()))
        .thenReturn(List.of(waiting));

    eventRegistrationService.updateWaitingQueueForEvent(event);

    assertThat(waiting.getEventRegistrationStatus()).isEqualTo(EventRegistrationStatus.WAITING);
  }

  // ---------- findAll ----------

  @Test
  void givenRegistrationsWhenFindAllWithSpecThenReturnsPageFromRepository() {
    EventRegistration registration =
        EventRegistration.builder()
            .id(1L)
            .event(event(1L))
            .participant(participant(2L))
            .eventRegistrationStatus(EventRegistrationStatus.ACCEPTED)
            .build();
    when(eventRegistrationRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(registration)));

    Page<EventRegistration> result =
        eventRegistrationService.findAll(mock(Specification.class), Pageable.ofSize(10));

    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  void givenRegistrationsWhenFindAllThenReturnsPageFromRepository() {
    EventRegistration registration =
        EventRegistration.builder()
            .id(1L)
            .event(event(1L))
            .participant(participant(2L))
            .eventRegistrationStatus(EventRegistrationStatus.ACCEPTED)
            .build();
    when(eventRegistrationRepository.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(registration)));

    Page<EventRegistration> result = eventRegistrationService.findAll(Pageable.ofSize(10));

    assertThat(result.getTotalElements()).isEqualTo(1);
  }
}
