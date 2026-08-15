package service;

import dto.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import model.Event;
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

  Map<String, List<EventResponse>> getEventsGrouped(Function<Event, String> classifier);

  EventRegResponse getRegistrationRequestById(int eventRegistrationId);

  List<EventRegResponse> getRegistrationRequests();

  UndoResponse getLatestAction();

  EventRegResponse changeRegistrationRequestStatus(
      int registrationId,
      EventRegRequestStatus eventRegRequestStatus,
      String description,
      Boolean addToHistory);

  List<EventRegResponse> getRegistrationRequestsInWaitingQueue(Integer eventId);

  UndoResponse undoLatestAction();
}
