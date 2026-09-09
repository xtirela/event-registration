package com.eventreg.eventregistrationservice.service.implementation;

import com.eventreg.eventregistrationservice.annotation.Idempotent;
import com.eventreg.eventregistrationservice.dto.request.create.EventRegistrationCreateRequest;
import com.eventreg.eventregistrationservice.exception.EventRegistrationNotFoundException;
import com.eventreg.eventregistrationservice.feign.EventClient;
import com.eventreg.eventregistrationservice.mapper.EventRegistrationMapper;
import com.eventreg.eventregistrationservice.model.EventRegistration;
import com.eventreg.eventregistrationservice.model.enums.EventRegistrationStatus;
import com.eventreg.eventregistrationservice.repository.EventRegistrationRepository;
import com.eventreg.eventregistrationservice.service.EventRegistrationService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of the event registration service backed by the registration repository. */
@AllArgsConstructor
@Service
public class EventRegistrationServiceImpl implements EventRegistrationService {

  private final EventRegistrationRepository eventRegistrationRepository;
  private final EventRegistrationMapper eventRegistrationMapper;
  private final EventClient eventClient;

  @Transactional
  @Override
  @Idempotent
  public EventRegistration createEventRegistration(
      EventRegistrationCreateRequest eventRegistrationCreateRequest) {

    String[] decision =
        eventClient
            .getRegistrationDecision(eventRegistrationCreateRequest.getEventId())
            .split(";", 2);

    EventRegistration eventRegistration =
        EventRegistration.builder()
            .eventId(eventRegistrationCreateRequest.getEventId())
            .participantId(eventRegistrationCreateRequest.getParticipantId())
            .eventRegistrationStatus(EventRegistrationStatus.fromString(decision[0]))
            .description(decision.length > 1 ? decision[1] : "none")
            .build();

    if (eventRegistrationCreateRequest.isSkipPending()
        && eventRegistration.getEventRegistrationStatus().equals(EventRegistrationStatus.PENDING)) {
      eventRegistration.setEventRegistrationStatus(EventRegistrationStatus.ACCEPTED);
      eventRegistration.setDescription("event registration accepted");
    }

    return eventRegistrationRepository.save(eventRegistration);
  }

  @Override
  public List<Long> getWaitingQueueIds(Long eventId, int limit) {
    return eventRegistrationRepository.findWaitingQueue(eventId, limit).stream()
        .map(EventRegistration::getId)
        .toList();
  }

  @Override
  @Transactional
  public void promoteRegistrations(
      Long eventId,
      List<Long> registrationIds,
      EventRegistrationStatus status,
      String description) {
    eventRegistrationRepository.promoteAll(
        eventId, registrationIds, status, description, EventRegistrationStatus.WAITING);
  }

  @Override
  public Long getCurrentParticipantAmountForEventWaiting(Long eventId) {
    return getCurrentParticipantAmountForEventWithStatus(eventId, EventRegistrationStatus.WAITING);
  }

  @Override
  public Long getCurrentParticipantAmountForEventAccepted(Long eventId) {
    return getCurrentParticipantAmountForEventWithStatus(eventId, EventRegistrationStatus.ACCEPTED);
  }

  private Long getCurrentParticipantAmountForEventWithStatus(
      Long eventId, EventRegistrationStatus status) {
    return eventRegistrationRepository.countRegistrationsByStatus(eventId, String.valueOf(status));
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
  // ponytail: распространение статуса в event-service — через 60s reconcile (~1 мин латентность,
  // только консервативнее, без overflow). Если нужна детерминированная скорость — прямой Feign.
  public EventRegistration changeRegistrationRequestStatus(
      Long registrationId, EventRegistrationStatus eventRegistrationStatus, String description) {

    EventRegistration eventRegistration = findById(registrationId);
    eventRegistration.setEventRegistrationStatus(eventRegistrationStatus);
    eventRegistration.setDescription(description);

    return eventRegistration;
  }
}
