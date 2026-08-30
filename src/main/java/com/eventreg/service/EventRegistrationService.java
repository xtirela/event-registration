package com.eventreg.service;

import com.eventreg.dto.request.create.EventRegistrationCreateRequest;
import com.eventreg.model.Event;
import com.eventreg.model.EventRegistration;
import com.eventreg.model.enums.EventRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface EventRegistrationService {
  EventRegistration createEventRegistration(
      EventRegistrationCreateRequest eventRegistrationCreateRequest);

  void deleteEventRegistration(Long eventRegistrationId);

  EventRegistration findById(Long eventRegistrationId);

  Page<EventRegistration> findAll(Specification<EventRegistration> spec, Pageable page);

  Page<EventRegistration> findAll(Pageable page);

  EventRegistration changeRegistrationRequestStatus(
      Long registrationId, EventRegistrationStatus eventRegistrationStatus, String description);

  void updateWaitingQueueForEvent(Event eventId);

  // Page<EventRegistrationResponse> findAllInWaitingQueue(Specification<EventRegistration> spec,
  // Pageable
  // page, Integer eventId);
  //  EventRegistrationResponse getRegistrationRequestById(int eventRegistrationId);
  //
  //  List<EventRegistrationResponse> getRegistrationRequests();
  //
  //  List<EventRegistration> findByCreatedBetween(OffsetDateTime from, OffsetDateTime to);
}
