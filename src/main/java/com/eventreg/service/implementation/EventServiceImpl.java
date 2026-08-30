package com.eventreg.service.implementation;

import com.eventreg.annotation.Idempotent;
import com.eventreg.dto.request.create.EventCreateRequest;
import com.eventreg.dto.request.update.EventUpdateRequest;
import com.eventreg.exception.EventNotFoundException;
import com.eventreg.mapper.EventMapper;
import com.eventreg.model.Event;
import com.eventreg.model.User;
import com.eventreg.model.enums.EventRegistrationStatus;
import com.eventreg.model.enums.EventReservationStatus;
import com.eventreg.model.enums.EventStatus;
import com.eventreg.repository.EventRepository;
import com.eventreg.service.EventService;
import com.eventreg.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@AllArgsConstructor
@Service
public class EventServiceImpl implements EventService {
  private final EventRepository eventRepository;
  private final UserService userService;
  private final EventMapper eventMapper;

  @Override
  @Transactional
  public EventReservationStatus updateCurrentEventReservationStatus(Event event) {
    log.info("enter update current event method {}", event.getId());

    if (event.getEventStatus().equals(EventStatus.CANCELLED)
        || event.getEventStatus().equals(EventStatus.ENDED)) {
      return updateReservationStatusIfChanged(event, EventReservationStatus.RESERVATIONS_CLOSED);
    }

    event.setCurrentParticipantAmount(
        (int)
            getCurrentParticipantAmountForEventWithStatus(
                event.getId(), EventRegistrationStatus.ACCEPTED));

    log.info(
        "event {} current participant amount {} ",
        event.getId(),
        event.getCurrentParticipantAmount());

    if (event.getCurrentParticipantAmount() >= event.getMaxParticipantAmount()) {
      if (event.isWaitlistWhenAllReserved()) {
        log.info("hit isWaitlistWhenAllReserved");
        event.setCurrentWaitingQueueParticipantAmount(
            (int)
                getCurrentParticipantAmountForEventWithStatus(
                    event.getId(), EventRegistrationStatus.WAITING));
        log.info("event current waiting {}", event.getCurrentWaitingQueueParticipantAmount());
        return updateReservationStatusIfChanged(event, EventReservationStatus.WAITLIST);
      }
      return updateReservationStatusIfChanged(event, EventReservationStatus.ALL_RESERVED);
    } else {
      if (event.isWaitlistWhenAllReserved()) {
        event.setCurrentWaitingQueueParticipantAmount(
            (int)
                getCurrentParticipantAmountForEventWithStatus(
                    event.getId(), EventRegistrationStatus.WAITING));
        if (event.getCurrentWaitingQueueParticipantAmount() > 0) {
          return updateReservationStatusIfChanged(event, EventReservationStatus.WAITLIST);
        }
      }

      return event.isConfirmationRequired()
          ? updateReservationStatusIfChanged(event, EventReservationStatus.CONFIRMATION_REQUIRED)
          : updateReservationStatusIfChanged(event, EventReservationStatus.RESERVATIONS_OPEN);
    }
  }

  private EventReservationStatus updateReservationStatusIfChanged(
      Event event, EventReservationStatus status) {
    if (event.getEventReservationStatus().equals(status)) {
      return status;
    }

    event.setEventReservationStatus(status);
    return eventRepository.save(event).getEventReservationStatus();
  }

  @Override
  @Transactional
  public long getCurrentParticipantAmountForEventWithStatus(
      Long eventId, EventRegistrationStatus eventRegistrationStatus) {
    String statusString = String.valueOf(eventRegistrationStatus);
    log.info("Counting registrations for event {} with status {}", eventId, statusString);

    long count = eventRepository.countRegistrationsByStatus(eventId, statusString);
    log.info("Count result: {}", count);

    return count;
  }

  @Idempotent
  @Override
  @Transactional
  public Event createEvent(EventCreateRequest request) {
    Event event = eventMapper.eventCreateRequestToEvent(request);

    User organizer = userService.findById(request.getOrganizerId());
    event.setOrganizer(organizer);

    return eventRepository.save(event);
  }

  @Idempotent
  @Override
  @Transactional
  public Event updateEvent(Long eventId, EventUpdateRequest request) {
    Event event = findById(eventId);
    eventMapper.eventUpdateRequestToEvent(request, event);
    updateCurrentEventReservationStatus(event);
    return eventRepository.save(event);
  }

  @Idempotent
  @Override
  @Transactional
  public void deleteEvent(Long eventId) {
    Event event = findById(eventId);
    eventRepository.delete(event);
  }

  @Override
  @Transactional(readOnly = true)
  public Event findById(Long eventId) {
    return eventRepository
        .findById(eventId)
        .orElseThrow(() -> new EventNotFoundException(eventId, "findById"));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Event> findAll(Specification<Event> spec, Pageable page) {
    return eventRepository.findAll(spec, page);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Event> findAll(Pageable page) {
    return eventRepository.findAll(page);
  }
}
