package com.eventreg.eventregistrationservice.service;

import com.eventreg.eventregistrationservice.dto.request.create.EventRegistrationCreateRequest;
import com.eventreg.eventregistrationservice.model.EventRegistration;
import com.eventreg.eventregistrationservice.model.enums.EventRegistrationStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/** Service facade for event registration operations. */
public interface EventRegistrationService {
  EventRegistration createEventRegistration(
      EventRegistrationCreateRequest eventRegistrationCreateRequest);

  void deleteEventRegistration(Long eventRegistrationId);

  EventRegistration findById(Long eventRegistrationId);

  Page<EventRegistration> findAll(Specification<EventRegistration> spec, Pageable page);

  Page<EventRegistration> findAll(Pageable page);

  EventRegistration changeRegistrationRequestStatus(
      Long registrationId, EventRegistrationStatus eventRegistrationStatus, String description);

  List<Long> getWaitingQueueIds(Long eventId, int limit);

  void promoteRegistrations(
      Long eventId, List<Long> registrationIds, EventRegistrationStatus status, String description);

  Long getCurrentParticipantAmountForEventWaiting(Long eventId);

  Long getCurrentParticipantAmountForEventAccepted(Long eventId);

  // Page<EventRegistrationResponse> findAllInWaitingQueue(Specification<EventRegistration> spec,
  // Pageable
  // page, Integer eventId);
  //  EventRegistrationResponse getRegistrationRequestById(int eventRegistrationId);
  //
  //  List<EventRegistrationResponse> getRegistrationRequests();
  //
  //  List<EventRegistration> findByCreatedBetween(OffsetDateTime from, OffsetDateTime to);
}
