package com.eventreg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventreg.dto.request.create.EventCreateRequest;
import com.eventreg.dto.request.update.EventUpdateRequest;
import com.eventreg.exception.EventNotFoundException;
import com.eventreg.exception.UserNotFoundException;
import com.eventreg.mapper.EventMapper;
import com.eventreg.model.Event;
import com.eventreg.model.User;
import com.eventreg.model.enums.EventGenderRequirement;
import com.eventreg.model.enums.EventRegistrationStatus;
import com.eventreg.model.enums.EventReservationStatus;
import com.eventreg.model.enums.EventStatus;
import com.eventreg.repository.EventRepository;
import com.eventreg.service.implementation.EventServiceImpl;
import java.time.Duration;
import java.time.OffsetDateTime;
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
 * Unit tests for {@link EventServiceImpl}. Repository and collaborating {@link UserService} are
 * mocked; the {@link EventMapper} is a real MapStruct instance so mapped fields can be asserted.
 */
@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class EventServiceImplTest {

  @Mock private EventRepository eventRepository;

  @Mock private UserService userService;

  private EventMapper eventMapper;

  private EventServiceImpl eventService;

  @BeforeEach
  void setUp() {
    eventMapper = Mappers.getMapper(EventMapper.class);
    eventService = new EventServiceImpl(eventRepository, userService, eventMapper);
  }

  private EventCreateRequest sampleCreateRequest(Long organizerId) {
    return EventCreateRequest.builder()
        .eventName("Tech Conference")
        .eventDescription("Annual tech meetup")
        .eventDate(OffsetDateTime.now().plusDays(10))
        .eventDuration(Duration.ofHours(2))
        .location("Main Hall")
        .ageRequired(18)
        .eventGenderRequirement(EventGenderRequirement.NONE)
        .maxParticipantAmount(100)
        .confirmationRequired(false)
        .waitlistWhenAllReserved(false)
        .organizerId(organizerId)
        .build();
  }

  private Event plannedEvent(
      long id, int current, int max, boolean waitlist, boolean confirmation) {
    return Event.builder()
        .id(id)
        .eventName("Tech Conference")
        .eventDate(OffsetDateTime.now().plusDays(10))
        .eventDuration(Duration.ofHours(2))
        .location("Main Hall")
        .ageRequired(18)
        .eventGenderRequirement(EventGenderRequirement.NONE)
        .currentParticipantAmount(current)
        .maxParticipantAmount(max)
        .eventStatus(EventStatus.PLANNED)
        .eventReservationStatus(EventReservationStatus.RESERVATIONS_OPEN)
        .confirmationRequired(confirmation)
        .waitlistWhenAllReserved(waitlist)
        .build();
  }

  private void stubSaveReturnsArgument() {
    when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  private void stubCounts(long accepted, long waiting) {
    when(eventRepository.countRegistrationsByStatus(any(Long.class), eq("ACCEPTED")))
        .thenReturn(accepted);
    when(eventRepository.countRegistrationsByStatus(any(Long.class), eq("WAITING")))
        .thenReturn(waiting);
  }

  // ---------- createEvent ----------

  @Test
  void givenValidCreateRequestWhenCreateEventThenReturnsEventWithRequestedName() {
    User organizer =
        User.builder()
            .id(5L)
            .username("organizer")
            .email("o@x.com")
            .password("password123")
            .build();
    when(userService.findById(5L)).thenReturn(organizer);
    stubSaveReturnsArgument();

    Event result = eventService.createEvent(sampleCreateRequest(5L));

    assertThat(result.getEventName()).isEqualTo("Tech Conference");
  }

  @Test
  void givenValidCreateRequestWhenCreateEventThenSetsOrganizerFetchedFromUserService() {
    User organizer =
        User.builder()
            .id(5L)
            .username("organizer")
            .email("o@x.com")
            .password("password123")
            .build();
    when(userService.findById(5L)).thenReturn(organizer);
    stubSaveReturnsArgument();

    Event result = eventService.createEvent(sampleCreateRequest(5L));

    assertThat(result.getOrganizer()).isEqualTo(organizer);
  }

  @Test
  void givenValidCreateRequestWhenCreateEventThenPersistsEvent() {
    User organizer =
        User.builder()
            .id(5L)
            .username("organizer")
            .email("o@x.com")
            .password("password123")
            .build();
    when(userService.findById(5L)).thenReturn(organizer);
    stubSaveReturnsArgument();

    eventService.createEvent(sampleCreateRequest(5L));

    verify(eventRepository).save(any(Event.class));
  }

  @Test
  void givenMissingOrganizerWhenCreateEventThenThrowsUserNotFoundException() {
    when(userService.findById(9L)).thenThrow(new UserNotFoundException(9L, "findById"));

    org.junit.jupiter.api.Assertions.assertThrows(
        UserNotFoundException.class, () -> eventService.createEvent(sampleCreateRequest(9L)));
  }

  // ---------- updateCurrentEventReservationStatus ----------

  @Test
  void givenPlannedEventWithFreeSlotsWhenUpdateStatusThenReturnsReservationsOpen() {
    Event event = plannedEvent(1L, 0, 100, false, false);
    stubCounts(0, 0);
    stubSaveReturnsArgument();

    EventReservationStatus status = eventService.updateCurrentEventReservationStatus(event);

    assertThat(status).isEqualTo(EventReservationStatus.RESERVATIONS_OPEN);
  }

  @Test
  void givenPlannedEventRequiringConfirmationWhenUpdateStatusThenReturnsConfirmationRequired() {
    Event event = plannedEvent(1L, 0, 100, false, true);
    stubCounts(0, 0);
    stubSaveReturnsArgument();

    EventReservationStatus status = eventService.updateCurrentEventReservationStatus(event);

    assertThat(status).isEqualTo(EventReservationStatus.CONFIRMATION_REQUIRED);
  }

  @Test
  void givenEventExactlyAtCapacityWhenUpdateStatusThenReturnsAllReserved() {
    Event event = plannedEvent(1L, 100, 100, false, false);
    stubCounts(100, 0);
    stubSaveReturnsArgument();

    EventReservationStatus status = eventService.updateCurrentEventReservationStatus(event);

    assertThat(status).isEqualTo(EventReservationStatus.ALL_RESERVED);
  }

  @Test
  void givenEventOverCapacityWhenUpdateStatusThenReturnsAllReserved() {
    Event event = plannedEvent(1L, 101, 100, false, false);
    stubCounts(101, 0);
    stubSaveReturnsArgument();

    EventReservationStatus status = eventService.updateCurrentEventReservationStatus(event);

    assertThat(status).isEqualTo(EventReservationStatus.ALL_RESERVED);
  }

  @Test
  void givenEventOneBelowCapacityWhenUpdateStatusThenReturnsReservationsOpen() {
    Event event = plannedEvent(1L, 99, 100, false, false);
    stubCounts(99, 0);
    stubSaveReturnsArgument();

    EventReservationStatus status = eventService.updateCurrentEventReservationStatus(event);

    assertThat(status).isEqualTo(EventReservationStatus.RESERVATIONS_OPEN);
  }

  @Test
  void givenWaitlistEventWithWaitingUsersWhenUpdateStatusThenReturnsWaitlist() {
    Event event = plannedEvent(1L, 0, 100, true, false);
    stubCounts(0, 5);
    stubSaveReturnsArgument();

    EventReservationStatus status = eventService.updateCurrentEventReservationStatus(event);

    assertThat(status).isEqualTo(EventReservationStatus.WAITLIST);
  }

  @Test
  void givenWaitlistEventFullWithWaitingUsersWhenUpdateStatusThenReturnsWaitlist() {
    Event event = plannedEvent(1L, 100, 100, true, false);
    stubCounts(100, 7);
    stubSaveReturnsArgument();

    EventReservationStatus status = eventService.updateCurrentEventReservationStatus(event);

    assertThat(status).isEqualTo(EventReservationStatus.WAITLIST);
  }

  @Test
  void givenWaitlistEventWithNoWaitingUsersWhenUpdateStatusThenReturnsReservationsOpen() {
    Event event = plannedEvent(1L, 0, 100, true, false);
    stubCounts(0, 0);
    stubSaveReturnsArgument();

    EventReservationStatus status = eventService.updateCurrentEventReservationStatus(event);

    assertThat(status).isEqualTo(EventReservationStatus.RESERVATIONS_OPEN);
  }

  @Test
  void givenCancelledEventWhenUpdateStatusThenReturnsReservationsClosed() {
    Event event = plannedEvent(1L, 0, 100, false, false);
    event.setEventStatus(EventStatus.CANCELLED);
    stubSaveReturnsArgument();

    EventReservationStatus status = eventService.updateCurrentEventReservationStatus(event);

    assertThat(status).isEqualTo(EventReservationStatus.RESERVATIONS_CLOSED);
  }

  @Test
  void givenEndedEventWhenUpdateStatusThenReturnsReservationsClosed() {
    Event event = plannedEvent(1L, 0, 100, false, false);
    event.setEventStatus(EventStatus.ENDED);
    stubSaveReturnsArgument();

    EventReservationStatus status = eventService.updateCurrentEventReservationStatus(event);

    assertThat(status).isEqualTo(EventReservationStatus.RESERVATIONS_CLOSED);
  }

  @Test
  void givenUnchangedStatusWhenUpdateStatusThenDoesNotPersistEvent() {
    Event event = plannedEvent(1L, 0, 100, false, false);
    stubCounts(0, 0);

    eventService.updateCurrentEventReservationStatus(event);

    verify(eventRepository, never()).save(any(Event.class));
  }

  @Test
  void givenFreeSlotsEventWhenUpdateStatusThenSetsCurrentParticipantAmountFromRepository() {
    Event event = plannedEvent(1L, 0, 100, false, false);
    stubCounts(42, 0);
    stubSaveReturnsArgument();

    eventService.updateCurrentEventReservationStatus(event);

    assertThat(event.getCurrentParticipantAmount()).isEqualTo(42);
  }

  // ---------- updateEvent ----------

  @Test
  void givenExistingEventWhenUpdateEventThenReturnsUpdatedName() {
    Event existing = plannedEvent(1L, 0, 100, false, false);
    when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
    stubCounts(0, 0);
    stubSaveReturnsArgument();
    EventUpdateRequest request = EventUpdateRequest.builder().eventName("New Name").build();

    Event result = eventService.updateEvent(1L, request);

    assertThat(result.getEventName()).isEqualTo("New Name");
  }

  @Test
  void givenExistingEventWhenUpdateEventThenPersistsEvent() {
    Event existing = plannedEvent(1L, 0, 100, false, false);
    when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
    stubCounts(0, 0);
    stubSaveReturnsArgument();
    EventUpdateRequest request = EventUpdateRequest.builder().eventName("New Name").build();

    eventService.updateEvent(1L, request);

    verify(eventRepository).save(any(Event.class));
  }

  @Test
  void givenReducedCapacityBelowCurrentWhenUpdateEventThenRecalculatesToAllReserved() {
    Event existing = plannedEvent(1L, 100, 100, false, false);
    when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
    stubCounts(100, 0);
    stubSaveReturnsArgument();
    EventUpdateRequest request = EventUpdateRequest.builder().maxParticipantAmount(50).build();

    Event result = eventService.updateEvent(1L, request);

    assertThat(result.getEventReservationStatus()).isEqualTo(EventReservationStatus.ALL_RESERVED);
  }

  @Test
  void givenMissingEventWhenUpdateEventThenThrowsEventNotFoundException() {
    when(eventRepository.findById(7L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        EventNotFoundException.class,
        () -> eventService.updateEvent(7L, EventUpdateRequest.builder().build()));
  }

  // ---------- deleteEvent ----------

  @Test
  void givenExistingEventWhenDeleteEventThenDeletesEvent() {
    Event existing = plannedEvent(1L, 0, 100, false, false);
    when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));

    eventService.deleteEvent(1L);

    verify(eventRepository).delete(existing);
  }

  @Test
  void givenMissingEventWhenDeleteEventThenThrowsEventNotFoundException() {
    when(eventRepository.findById(7L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        EventNotFoundException.class, () -> eventService.deleteEvent(7L));
  }

  // ---------- findById ----------

  @Test
  void givenExistingEventWhenFindByIdThenReturnsEvent() {
    Event existing = plannedEvent(1L, 0, 100, false, false);
    when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));

    Event result = eventService.findById(1L);

    assertThat(result.getId()).isEqualTo(1L);
  }

  @Test
  void givenMissingEventWhenFindByIdThenThrowsEventNotFoundException() {
    when(eventRepository.findById(7L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        EventNotFoundException.class, () -> eventService.findById(7L));
  }

  // ---------- getCurrentParticipantAmountForEventWithStatus ----------

  @Test
  void givenEventWhenCountWithStatusThenReturnsRepositoryCount() {
    when(eventRepository.countRegistrationsByStatus(3L, "ACCEPTED")).thenReturn(12L);

    long result =
        eventService.getCurrentParticipantAmountForEventWithStatus(
            3L, EventRegistrationStatus.ACCEPTED);

    assertThat(result).isEqualTo(12L);
  }

  @Test
  void givenEventWhenCountWithStatusThenReturnsZeroWhenNoRegistrations() {
    when(eventRepository.countRegistrationsByStatus(3L, "WAITING")).thenReturn(0L);

    long result =
        eventService.getCurrentParticipantAmountForEventWithStatus(
            3L, EventRegistrationStatus.WAITING);

    assertThat(result).isEqualTo(0L);
  }

  // ---------- findAll ----------

  @Test
  void givenEventsWhenFindAllWithSpecThenReturnsPageFromRepository() {
    Event event = plannedEvent(1L, 0, 100, false, false);
    when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(event)));

    Page<Event> result = eventService.findAll(mock(Specification.class), Pageable.ofSize(10));

    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  void givenEventsWhenFindAllThenReturnsPageFromRepository() {
    Event event = plannedEvent(1L, 0, 100, false, false);
    when(eventRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(event)));

    Page<Event> result = eventService.findAll(Pageable.ofSize(10));

    assertThat(result.getTotalElements()).isEqualTo(1);
  }
}
