package com.eventreg.service;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import com.eventreg.dto.request.EventCreateRequest;
import com.eventreg.dto.request.EventRegRequest;
import com.eventreg.dto.request.ParticipantCreateRequest;
import com.eventreg.dto.response.*;
import com.eventreg.model.Event;
import com.eventreg.model.EventRegistration;
import com.eventreg.model.Participant;
import com.eventreg.model.enums.EventRegRequestStatus;

public interface EventService {

  EventResponse createEvent(EventCreateRequest eventCreateRequest);

  ParticipantResponse createParticipant(ParticipantCreateRequest participantCreateRequest);

  EventRegResponse registerParticipant(EventRegRequest eventRegRequest);

  ParticipantResponse getParticipantById(int participantId);

  List<ParticipantResponse> getParticipants();

  List<ParticipantResponse> getParticipantsSorted(Comparator<Participant> comparator);

  EventResponse getEventById(int eventId);

  List<EventResponse> getEvents();

  List<EventResponse> getEventsFiltered(List<Predicate<Event>> predicates);

  EventSummaryResponse getEventSummary(int eventId);

  Map<String, Long> groupByFillStatus();

  List<Event> findMostPopular(int limit);

  List<Participant> searchByFragment(String fragment);

  Map<String, List<EventResponse>> getEventsGrouped(Function<Event, String> classifier);

  EventRegResponse getRegistrationRequestById(int eventRegistrationId);

  List<EventRegResponse> getRegistrationRequests();

  List<EventRegistration> findByCreatedBetween(OffsetDateTime from, OffsetDateTime to);

  UndoResponse getLatestAction();

  EventRegResponse changeRegistrationRequestStatus(
      int registrationId,
      EventRegRequestStatus eventRegRequestStatus,
      String description,
      Boolean addToHistory);

  List<EventRegResponse> getRegistrationRequestsInWaitingQueue(Integer eventId);

  UndoResponse undoLatestAction();
}
