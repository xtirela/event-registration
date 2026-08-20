package service;

import dto.*;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import model.Event;
import model.EventRegistration;
import model.Participant;
import model.enums.EventRegRequestStatus;

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

  EventSummary getEventSummary(int eventId);

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
