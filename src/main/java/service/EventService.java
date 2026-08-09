package service;

import dto.*;
import java.util.List;

public interface EventService {
  EventResponse createEvent(EventCreateRequest eventCreateRequest);

  ParticipantResponse createParticipant(ParticipantCreateRequest participantCreateRequest);

  EventRegResponse registerParticipant(EventRegRequest eventRegRequest);

  EventRegResponse cancelRegistration(CancelRegRequest cancelRegRequest);

  List<ParticipantResponse> getParticipants();

  ParticipantResponse getParticipantById(int participantId);

  List<EventResponse> getEvents();

  EventResponse getEventById(int eventId);

  List<EventRegResponse> getRegistrationRequests();

  EventRegResponse getRegistrationRequestById(int eventRegistrationId);

  List<ParticipantEventPair> getRegisteredParticipant(Integer participantId);

  List<ParticipantEventPair> getAllRegisteredParticipants();
}
