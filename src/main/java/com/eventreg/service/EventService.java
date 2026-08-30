package com.eventreg.service;

import com.eventreg.dto.request.create.EventCreateRequest;
import com.eventreg.dto.request.update.EventUpdateRequest;
import com.eventreg.model.Event;
import com.eventreg.model.enums.EventRegistrationStatus;
import com.eventreg.model.enums.EventReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface EventService {

  Event createEvent(EventCreateRequest eventCreateRequest);

  Event updateEvent(Long eventId, EventUpdateRequest eventUpdateRequest);

  void deleteEvent(Long eventId);

  Event findById(Long eventId);

  Page<Event> findAll(Specification<Event> spec, Pageable page);

  Page<Event> findAll(Pageable page);

  EventReservationStatus updateCurrentEventReservationStatus(Event event);

  long getCurrentParticipantAmountForEventWithStatus(
      Long eventId, EventRegistrationStatus eventRegistrationStatus);
}
