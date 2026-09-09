package com.eventreg.eventservice.service.implementation;

import com.eventreg.eventservice.annotation.Idempotent;
import com.eventreg.eventservice.dto.request.create.EventCreateRequest;
import com.eventreg.eventservice.dto.request.update.EventUpdateRequest;
import com.eventreg.eventservice.exception.EventNotFoundException;
import com.eventreg.eventservice.feign.EventRegistrationClient;
import com.eventreg.eventservice.feign.NotificationClient;
import com.eventreg.eventservice.mapper.EventMapper;
import com.eventreg.eventservice.model.Event;
import com.eventreg.eventservice.model.enums.EventReservationStatus;
import com.eventreg.eventservice.model.enums.EventStatus;
import com.eventreg.eventservice.repository.EventRepository;
import com.eventreg.eventservice.service.EventService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventServiceImpl implements EventService {
  private final EventRepository eventRepository;
  private final EventMapper eventMapper;
  private final EventRegistrationClient eventRegistrationClient;
  private final NotificationClient notificationClient;

  @Override
  @Transactional
  public EventReservationStatus updateCurrentEventReservationStatus(Event event) {
    if (event.getEventStatus().equals(EventStatus.CANCELLED)
        || event.getEventStatus().equals(EventStatus.ENDED)) {
      return updateReservationStatusIfChanged(event, EventReservationStatus.RESERVATIONS_CLOSED);
    }

    event.setCurrentParticipantAmount(
        Math.toIntExact(
            eventRegistrationClient.getCurrentParticipantAmountForEventWithStatusAccepted(
                event.getId())));

    if (event.getCurrentParticipantAmount() >= event.getMaxParticipantAmount()) {
      if (event.isWaitlistWhenAllReserved()) {
        event.setCurrentWaitingQueueParticipantAmount(
            Math.toIntExact(
                eventRegistrationClient.getCurrentParticipantAmountForEventWithStatusWaiting(
                    event.getId())));
        return updateReservationStatusIfChanged(event, EventReservationStatus.WAITLIST);
      }
      return updateReservationStatusIfChanged(event, EventReservationStatus.ALL_RESERVED);
    } else {
      if (event.isWaitlistWhenAllReserved()) {
        event.setCurrentWaitingQueueParticipantAmount(
            Math.toIntExact(
                eventRegistrationClient.getCurrentParticipantAmountForEventWithStatusWaiting(
                    event.getId())));
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
  public String decideRegistrationStatus(Long eventId) {
    Event event = findById(eventId);

    if (event.getEventStatus().equals(EventStatus.CANCELLED)
        || event.getEventStatus().equals(EventStatus.ENDED)
        || event.getEventReservationStatus().equals(EventReservationStatus.RESERVATIONS_CLOSED)
        || event.getEventReservationStatus().equals(EventReservationStatus.ALL_RESERVED)) {
      return "DENIED;event does not accept any more registrations";
    }

    if (event.getEventReservationStatus().equals(EventReservationStatus.WAITLIST)) {
      return "WAITING;event is in waitlist status";
    }

    if (event.getEventReservationStatus().equals(EventReservationStatus.CONFIRMATION_REQUIRED)) {
      // ponytail: проверка не атомарна против reconcile-перезаписи счётчика (раз в минуту);
      // для курсача ок, точный вариант — атомарная ёмкость-проверка в самом UPDATE.
      if (event.getCurrentParticipantAmount() < event.getMaxParticipantAmount()) {
        return "PENDING;registration requires confirmation";
      }
      return event.isWaitlistWhenAllReserved()
          ? "WAITING;event is in waitlist status"
          : "DENIED;event does not accept any more registrations";
    }

    // ponytail: CAS против кэшированного current_participant_amount, который раз в минуту
    // перезаписывает reconcile (updateCurrentEventReservationStatus). В окне между коммитом CAS
    // и reconcile два concurrent CAS могут оба пройти. Для курсача ок; точный вариант — один
    // писатель: только CAS + явные декременты вместо перезаписи reconcile.
    if (eventRepository.tryAcquireSeat(eventId) == 1) {
      return "ACCEPTED;event registration accepted";
    }

    return event.isWaitlistWhenAllReserved()
        ? "WAITING;event is in waitlist status"
        : "DENIED;event does not accept any more registrations";
  }

  @Override
  @Transactional
  public void promoteWaitingQueue(Long eventId, int limit) {
    Event event = findById(eventId);
    List<Long> waitingIds = eventRegistrationClient.getWaitingQueue(eventId, limit);

    List<Long> toPromote = new ArrayList<>();
    for (Long id : waitingIds) {
      // место берётся атомарно тут же; проигравшие CAS в промоушен не попадают
      if (eventRepository.tryAcquireSeat(eventId) == 1) {
        toPromote.add(id);
      } else {
        break;
      }
    }

    if (!toPromote.isEmpty()) {
      String status = event.isConfirmationRequired() ? "PENDING" : "ACCEPTED";
      eventRegistrationClient.promoteWaitingQueue(
          eventId, status, "waiting spot freed up", toPromote);
    }
  }

  @Idempotent
  @Override
  @Transactional
  public Event createEvent(
      EventCreateRequest request, String organizerKeycloakId, String organizerEmail) {
    Event event = eventMapper.eventCreateRequestToEvent(request, organizerKeycloakId);
    Event saved = eventRepository.save(event);

    // ponytail: fire-and-forget — уведомление не должно валить создание события; общий FJP
    // для курсача ок, при нагрузке — выделенный executor и retry/outbox
    CompletableFuture.runAsync(
        () -> {
          try {
            notificationClient.sendEventCreated(
                eventMapper.eventToEventResponse(saved), organizerEmail);
          } catch (Exception e) {
            log.warn("Failed to send event-created notification for event {}", saved.getId(), e);
          }
        });

    return saved;
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
