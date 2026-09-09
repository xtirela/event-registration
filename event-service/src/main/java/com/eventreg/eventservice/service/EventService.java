package com.eventreg.eventservice.service;

import com.eventreg.eventservice.dto.request.create.EventCreateRequest;
import com.eventreg.eventservice.dto.request.update.EventUpdateRequest;
import com.eventreg.eventservice.model.Event;
import com.eventreg.eventservice.model.enums.EventReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface EventService {

  Event createEvent(
      EventCreateRequest eventCreateRequest, String organizerKeycloakId, String organizerEmail);

  Event updateEvent(Long eventId, EventUpdateRequest eventUpdateRequest);

  void deleteEvent(Long eventId);

  Event findById(Long eventId);

  Page<Event> findAll(Specification<Event> spec, Pageable page);

  Page<Event> findAll(Pageable page);

  String decideRegistrationStatus(Long eventId);

  void promoteWaitingQueue(Long eventId, int limit);

  EventReservationStatus updateCurrentEventReservationStatus(Event event);
}
