package com.eventreg.service.implementation;

import com.eventreg.annotation.Idempotent;
import com.eventreg.dto.request.create.EventRegistrationCreateRequest;
import com.eventreg.exception.EventRegistrationNotFoundException;
import com.eventreg.mapper.EventRegistrationMapper;
import com.eventreg.model.Event;
import com.eventreg.model.EventRegistration;
import com.eventreg.model.Participant;
import com.eventreg.model.enums.EventRegistrationStatus;
import com.eventreg.model.enums.EventReservationStatus;
import com.eventreg.repository.EventRegistrationRepository;
import com.eventreg.service.EventRegistrationService;
import com.eventreg.service.EventService;
import com.eventreg.service.ParticipantService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class EventRegistrationServiceImpl implements EventRegistrationService {

  private final EventRegistrationRepository eventRegistrationRepository;
  private final ParticipantService participantService;
  private final EventService eventService;
  private final EventRegistrationMapper eventRegistrationMapper;

  private final EmailService emailService;

  @Transactional
  @Override
  @Idempotent
  public EventRegistration createEventRegistration(
      EventRegistrationCreateRequest eventRegistrationCreateRequest) {

    Event event = eventService.findById(eventRegistrationCreateRequest.getEventId());
    Participant participant =
        participantService.findById(eventRegistrationCreateRequest.getParticipantId());

    Pair<EventRegistrationStatus, String> statusDescriptionPair =
        determineRegistrationStatus(event);

    EventRegistration eventRegistration =
        EventRegistration.builder()
            .participant(participant)
            .event(event)
            .eventRegistrationStatus(statusDescriptionPair.getFirst())
            .description(statusDescriptionPair.getSecond())
            .build();

    if (eventRegistrationCreateRequest.isSkipPending()
        && eventRegistration.getEventRegistrationStatus().equals(EventRegistrationStatus.PENDING)) {
      eventRegistration.setEventRegistrationStatus(EventRegistrationStatus.ACCEPTED);
      eventRegistration.setDescription("event registration accepted");
    }

    emailService.sendEventConfirmation(
        eventRegistration.getParticipant().getUser().getEmail(), // или откуда у тебя email
        event.getEventName(),
        eventRegistration.getDescription());

    return eventRegistrationRepository.save(eventRegistration);
  }

  private Pair<EventRegistrationStatus, String> determineRegistrationStatus(Event event) {
    EventReservationStatus eventReservationStatus =
        eventService.updateCurrentEventReservationStatus(event);
    if (eventReservationStatus.equals(EventReservationStatus.RESERVATIONS_CLOSED)
        || eventReservationStatus.equals(EventReservationStatus.ALL_RESERVED)) {
      return Pair.of(
          EventRegistrationStatus.DENIED, "event does not accept any more registrations");
    } else if (eventReservationStatus.equals(EventReservationStatus.WAITLIST)) {
      return Pair.of(EventRegistrationStatus.WAITING, "event is in waitlist status");
    } else if (eventReservationStatus.equals(EventReservationStatus.CONFIRMATION_REQUIRED)) {
      return Pair.of(EventRegistrationStatus.PENDING, "registration requires confirmation");
    }

    return Pair.of(EventRegistrationStatus.ACCEPTED, "event registration accepted");
  }

  @Override
  @Transactional
  public void updateWaitingQueueForEvent(Event event) {
    List<EventRegistration> inWaitingQueue =
        eventRegistrationRepository.findWaitingQueue(
            event.getId(), event.getMaxParticipantAmount() - event.getCurrentParticipantAmount());
    for (EventRegistration er : inWaitingQueue) {
      EventRegistrationStatus newStatus = determineRegistrationStatus(event).getFirst();

      if (!(newStatus.equals(EventRegistrationStatus.ACCEPTED)
          || newStatus.equals(EventRegistrationStatus.PENDING))) {
        if (newStatus.equals(EventRegistrationStatus.WAITING)
            && (event.getMaxParticipantAmount() - event.getCurrentParticipantAmount() > 0)) {
          // TODO: опять залупу написал, обход логики так себе надо как-то лучше реализовать что
          // новый пользователи получают WAITING и ждёт пока старые станут ACCEPTED в следующм цикле
          // обновления scheduler
          er.setEventRegistrationStatus(
              event.isConfirmationRequired()
                  ? EventRegistrationStatus.PENDING
                  : EventRegistrationStatus.ACCEPTED);
          er.setDescription("waiting spot freed up");
          continue;
        }
        break;
      }

      er.setEventRegistrationStatus(newStatus);
      er.setDescription("waiting spot freed up");
    }
  }

  @Idempotent
  @Override
  @Transactional
  public void deleteEventRegistration(Long eventRegistrationId) {
    EventRegistration eventRegistration = findById(eventRegistrationId);

    eventRegistrationRepository.delete(eventRegistration);
  }

  @Override
  @Transactional(readOnly = true)
  public EventRegistration findById(Long eventRegistrationId) {
    return eventRegistrationRepository
        .findById(eventRegistrationId)
        .orElseThrow(() -> new EventRegistrationNotFoundException(eventRegistrationId, "findById"));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<EventRegistration> findAll(Specification<EventRegistration> spec, Pageable page) {
    return eventRegistrationRepository.findAll(spec, page);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<EventRegistration> findAll(Pageable page) {
    return eventRegistrationRepository.findAll(page);
  }

  @Idempotent
  @Override
  @Transactional
  public EventRegistration changeRegistrationRequestStatus(
      Long registrationId, EventRegistrationStatus eventRegistrationStatus, String description) {
    EventRegistration eventRegistration = findById(registrationId);
    eventRegistration.setEventRegistrationStatus(eventRegistrationStatus);
    eventRegistration.setDescription(description);
    if (eventRegistration.getEvent() != null) {
      eventService.updateCurrentEventReservationStatus(eventRegistration.getEvent());
    }

    return eventRegistration;
  }
}
