package service.implementation;

import dto.*;
import model.*;
import model.Event;
import model.enums.*;
import service.EventService;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;

//@Component
//@Primary
public class EventServiceImpl implements EventService {
    // создать мероприятие
    // зарегистрировать участника
    // отменить регистрацию
    // показать список
    private int eventCounter = 1;
    private int participantCounter = 1;
    private int eventRegistrationCounter = 1;

    private final HashMap<Integer, Event> events = new HashMap<Integer, Event>();

    private final HashMap<Integer, Participant> participants = new HashMap<Integer, Participant>();

    private final HashMap<Integer, EventRegistration> registrationRequests = new HashMap<Integer, EventRegistration>();

    private final HashMap<EventRegistration, Event> registeredParticipants = new HashMap<EventRegistration, Event>();

    @Override
    public EventResponse createEvent(EventCreateRequest eventCreateRequest) {

        EventResponse errorMessage = checkValidCreateEvent(eventCreateRequest);

        if (errorMessage != null) {
            return errorMessage;
        }


        Event event = Event.builder()
                .id(eventCounter++)
                .createdAt(OffsetDateTime.now())
                .eventName(eventCreateRequest.getEventName())
                .eventDate(eventCreateRequest.getEventDate())
                .eventDuration(eventCreateRequest.getEventDuration())
                .eventStatus(EventStatus.PLANNED)
                .eventRegistrationStatus(EventRegistrationStatus.RESERVATIONS_OPEN)
                .ageRequired(eventCreateRequest.getAgeRequired())
                .maxParticipantAmount(eventCreateRequest.getMaxParticipantAmount())
                .currentParticipantAmount(0)
                .eventGenderRequirement(eventCreateRequest.getGenderRequirement())
                .build();
        events.put(event.getId(), event);

        return eventToEventResponse(event);
    }

    @Override
    public ParticipantResponse createParticipant(ParticipantCreateRequest participantCreateRequest) {

        ParticipantResponse errorMessage = checkValidCreateRequest(participantCreateRequest);

        if (errorMessage != null) {
            return errorMessage;
        }

        Participant participant = Participant.builder()
                .id(participantCounter++)
                .firstName(participantCreateRequest.getFirstName())
                .lastName(participantCreateRequest.getLastName())
                .email(participantCreateRequest.getEmail())
                .age(participantCreateRequest.getAge())
                .participantGender(participantCreateRequest.getParticipantGender())
                .createdAt(OffsetDateTime.now())
                .build();
        participants.put(participant.getId(), participant);

        return ParticipantResponse
                .builder()
                .firstName(participant.getFirstName())
                .lastName(participant.getLastName())
                .email(participant.getEmail())
                .age(participant.getAge())
                .participantGender(participant.getParticipantGender())
                .build();
    }

    @Override
    public EventRegResponse registerParticipant(EventRegRequest eventRegRequest) {

        EventRegistration eventRegistration = EventRegistration.builder()
                .id(eventRegistrationCounter++)
                .participantId(eventRegRequest.getParticipantId())
                .eventId(eventRegRequest.getEventId())
                .eventRegRequestStatus(EventRegRequestStatus.PENDING).build();

        Participant participant = participants.get(eventRegRequest.getParticipantId());
        if (participant == null) {

            eventRegistration.setEventRegRequestStatus(EventRegRequestStatus.DENIED);
            registrationRequests.put(eventRegistration.getId(), eventRegistration);
            return EventRegResponse.builder().description("Participant does not exist!").eventRegRequestStatus(eventRegistration.getEventRegRequestStatus()).build();

        }

        Event event = events.get(eventRegRequest.getEventId());
        if (event == null) {

            eventRegistration.setEventRegRequestStatus(EventRegRequestStatus.DENIED);
            registrationRequests.put(eventRegistration.getId(), eventRegistration);
            return EventRegResponse.builder().description("Event does not exist!").eventRegRequestStatus(eventRegistration.getEventRegRequestStatus()).build();

        }

        eventRegistration.setDescription("Request undergoing review...");
        eventRegistration.setEventRegRequestStatus(EventRegRequestStatus.PENDING);

        registrationRequests.put(eventRegistration.getId(), eventRegistration);

        reviewParticipant(participant, event, eventRegistration);

        return EventRegistrationToEventRegResponse(eventRegistration, eventRegistration.getDescription(), eventRegistration.getEventRegRequestStatus());

    }

    @Override
    public EventRegResponse cancelRegistration(CancelRegRequest cancelRegRequest) {

        EventRegistration eventRegistration = registrationRequests.get(cancelRegRequest.getEventRegistrationId());

        if (eventRegistration == null) {
            return EventRegResponse.builder().description("Event registration does not exist!").eventRegRequestStatus(EventRegRequestStatus.NOT_FOUND).build();
        }

        Event event = events.get(eventRegistration.getEventId());

        if (event == null) {
            return EventRegResponse.builder().description("Event to which registration points to does not exist!").eventRegRequestStatus(EventRegRequestStatus.NOT_FOUND).build();
        }

        if (eventRegistration.getEventRegRequestStatus() == EventRegRequestStatus.ACCEPTED) {
            event.setCurrentParticipantAmount(event.getCurrentParticipantAmount() - 1);
        }

        eventRegistration.setEventRegRequestStatus(EventRegRequestStatus.CANCELLED);
        eventRegistration.setDescription("Cancelled by participant");

        registrationRequests.put(cancelRegRequest.getEventRegistrationId(), eventRegistration);


        return EventRegistrationToEventRegResponse(eventRegistration, eventRegistration.getDescription(), eventRegistration.getEventRegRequestStatus());
    }

    @Override
    public List<ParticipantResponse> getParticipants() {
        Set<Map.Entry<Integer, Participant>> participantSet = participants.entrySet();
        List<ParticipantResponse> participantResponse = new ArrayList<>();

        for (Map.Entry<Integer, Participant> entry : participantSet) {
            Participant participant = entry.getValue();

            ParticipantResponse itemResponse = ParticipantResponse.builder()
                    .firstName(participant.getFirstName())
                    .lastName(participant.getLastName())
                    .email(participant.getEmail())
                    .age(participant.getAge())
                    .participantGender(participant.getParticipantGender())
                    .build();

            participantResponse.add(itemResponse);
        }

        return participantResponse;
    }

    @Override
    public ParticipantResponse getParticipantById(int participantId) {
        Participant participant = participants.get(participantId);
        if (participant != null) {
            return ParticipantResponse.builder()
                    .firstName(participant.getFirstName())
                    .lastName(participant.getLastName())
                    .email(participant.getEmail())
                    .age(participant.getAge())
                    .participantGender(participant.getParticipantGender())
                    .build();
        } else {
            return ParticipantResponse.builder()
                    .firstName("NOT FOUND")
                    .build();
        }

    }

    @Override
    public List<EventResponse> getEvents() {
        Set<Map.Entry<Integer, Event>> eventsSet = events.entrySet();
        List<EventResponse> eventsResponse = new ArrayList<>();

        for (Map.Entry<Integer, Event> entry : eventsSet) {
            Event event = entry.getValue();

            EventResponse eventResponse = EventResponse.builder()
                    .eventName(event.getEventName())
                    .eventDate(event.getEventDate())
                    .eventDuration(event.getEventDuration())
                    .ageRequired(event.getAgeRequired())
                    .currentParticipantAmount(event.getCurrentParticipantAmount())
                    .maxParticipantAmount(event.getMaxParticipantAmount())
                    .eventGenderRequirement(event.getEventGenderRequirement())
                    .eventStatus(event.getEventStatus())
                    .eventRegistrationStatus(event.getEventRegistrationStatus())
                    .build();

            eventsResponse.add(eventResponse);
        }

        return eventsResponse;
    }

    @Override
    public EventResponse getEventById(int eventId) {
        Event event = events.get(eventId);
        if (event != null) {
            return EventResponse.builder()
                    .eventName(event.getEventName())
                    .eventDate(event.getEventDate())
                    .eventDuration(event.getEventDuration())
                    .ageRequired(event.getAgeRequired())
                    .currentParticipantAmount(event.getCurrentParticipantAmount())
                    .maxParticipantAmount(event.getMaxParticipantAmount())
                    .eventGenderRequirement(event.getEventGenderRequirement())
                    .eventStatus(event.getEventStatus())
                    .eventRegistrationStatus(event.getEventRegistrationStatus())
                    .build();
        } else {
            return EventResponse.builder()
                    .eventName("NOT FOUND").build();
        }
    }

    @Override
    public List<EventRegResponse> getRegistrationRequests() {
        List<EventRegResponse> registrationResponses = new ArrayList<>();

        if (registrationRequests.isEmpty()) {
            return registrationResponses; // Возвращаем пустой список
        }

        for (Map.Entry<Integer, EventRegistration> entry : registrationRequests.entrySet()) {
            if (entry == null) {
                continue;
            }
            EventRegistration registration = entry.getValue();

            if (registration == null) {
                continue;
            }

            EventRegResponse response = EventRegistrationToEventRegResponse(registration, registration.getDescription(), registration.getEventRegRequestStatus());

            if (response != null) {
                registrationResponses.add(response);
            }
        }

        return registrationResponses;
    }

    @Override
    public EventRegResponse getRegistrationRequestById(int eventRegistrationId) {
        EventRegistration registration = registrationRequests.get(eventRegistrationId);
        if (registration != null) {
            return EventRegistrationToEventRegResponse(registration, registration.getDescription(), registration.getEventRegRequestStatus());
        } else {
            return EventRegResponse.builder().eventRegRequestStatus(EventRegRequestStatus.NOT_FOUND).description("Registration request not found!").build();
        }
    }

    @Override
    public List<ParticipantEventPair> getRegisteredParticipant(Integer participantId) {
        Set<Map.Entry<EventRegistration, Event>> registeredParticipantsSet = registeredParticipants.entrySet();

        List<ParticipantEventPair> RegParticipantsResponse = new ArrayList<>();

        for (Map.Entry<EventRegistration, Event> entry : registeredParticipantsSet) {
            Event event = entry.getValue();
            EventRegistration registration = entry.getKey();

            EventResponse eventResponse = eventToEventResponse(event);
            EventRegResponse eventRegResponse = EventRegistrationToEventRegResponse(registration, registration.getDescription(), registration.getEventRegRequestStatus());
            if (participantId == null) {
                RegParticipantsResponse.add(ParticipantEventPair.builder().eventRegResponse(eventRegResponse).eventResponse(eventResponse).build());
            } else if (participantId.equals(registration.getParticipantId())) {
                RegParticipantsResponse.add(ParticipantEventPair.builder().eventRegResponse(eventRegResponse).eventResponse(eventResponse).build());
            }
        }

        return RegParticipantsResponse;
    }

    @Override
    public List<ParticipantEventPair> getAllRegisteredParticipants() {

        Set<Map.Entry<EventRegistration, Event>> registeredParticipantsSet = registeredParticipants.entrySet();

        List<ParticipantEventPair> RegParticipantsResponse = new ArrayList<>();

        for (Map.Entry<EventRegistration, Event> entry : registeredParticipantsSet) {
            Event event = entry.getValue();
            EventRegistration registration = entry.getKey();

            EventResponse eventResponse = eventToEventResponse(event);
            EventRegResponse eventRegResponse = EventRegistrationToEventRegResponse(registration, registration.getDescription(), registration.getEventRegRequestStatus());
            RegParticipantsResponse.add(ParticipantEventPair.builder().eventRegResponse(eventRegResponse).eventResponse(eventResponse).build());

        }

        return RegParticipantsResponse;
    }


    private void reviewParticipant(Participant participant, Event event, EventRegistration eventRegistration) {
        if (!(event.getEventRegistrationStatus() == EventRegistrationStatus.RESERVATIONS_OPEN)) {
            eventRegistration.setEventRegRequestStatus(EventRegRequestStatus.DENIED);
            eventRegistration.setDescription("Reservations are not open!");
            registrationRequests.put(eventRegistration.getId(), eventRegistration);
            return;
        }

        if (participant.getAge() < event.getAgeRequired()) {
            eventRegistration.setEventRegRequestStatus(EventRegRequestStatus.DENIED);
            eventRegistration.setDescription("Participant is out of age!");
            registrationRequests.put(eventRegistration.getId(), eventRegistration);
            return;
        }

        if (event.getCurrentParticipantAmount() >= event.getMaxParticipantAmount()) {
            eventRegistration.setEventRegRequestStatus(EventRegRequestStatus.DENIED);
            event.setEventRegistrationStatus(EventRegistrationStatus.ALL_RESERVED);
            events.put(event.getId(), event);
            eventRegistration.setDescription("All spots reserved");
            registrationRequests.put(eventRegistration.getId(), eventRegistration);
            return;
        }

        if (event.getEventGenderRequirement() != EventGenderRequirement.NONE) {
            if (participant.getParticipantGender() == ParticipantGender.MALE && (event.getEventGenderRequirement() == EventGenderRequirement.FEMALE_ONLY)) {
                eventRegistration.setEventRegRequestStatus(EventRegRequestStatus.DENIED);
                eventRegistration.setDescription("Event is female only");
                registrationRequests.put(eventRegistration.getId(), eventRegistration);
                return;
            }
            if (participant.getParticipantGender() == ParticipantGender.FEMALE && (event.getEventGenderRequirement() == EventGenderRequirement.MALE_ONLY)) {
                eventRegistration.setEventRegRequestStatus(EventRegRequestStatus.DENIED);
                eventRegistration.setDescription("Event is male only");
                registrationRequests.put(eventRegistration.getId(), eventRegistration);
                return;
            }
            if (participant.getParticipantGender() == ParticipantGender.NOT_SPECIFIED) {
                eventRegistration.setEventRegRequestStatus(EventRegRequestStatus.DENIED);
                eventRegistration.setDescription("Event requires gender specified");
                registrationRequests.put(eventRegistration.getId(), eventRegistration);
                return;
            }
        }

        eventRegistration.setEventRegRequestStatus(EventRegRequestStatus.ACCEPTED);
        eventRegistration.setDescription("Reservation accepted");

        registrationRequests.put(eventRegistration.getId(), eventRegistration);

        registeredParticipants.put(eventRegistration, event);

        event.setCurrentParticipantAmount(event.getCurrentParticipantAmount() + 1);
        events.put(event.getId(), event);
    }

    private EventRegResponse EventRegistrationToEventRegResponse(
            EventRegistration eventRegistration, String description, EventRegRequestStatus eventRegRequestStatus) {

        Participant participant = participants.get(eventRegistration.getParticipantId());
        if (participant == null) {
            return EventRegResponse.builder().description("Participant does not exist!").build();
        }

        Event event = events.get(eventRegistration.getEventId());
        if (event == null) {
            return EventRegResponse.builder().description("Event does not exist!").build();
        }


        return EventRegResponse.builder()
                .firstName(participant.getFirstName())
                .lastName(participant.getLastName())
                .email(participant.getEmail())
                .age(participant.getAge())
                .participantGender(participant.getParticipantGender())
                .eventRegRequestStatus(eventRegRequestStatus)
                .description(description)
                .eventName(event.getEventName())
                .build();
    }

    private EventResponse eventToEventResponse(Event event) {
        return EventResponse
                .builder()
                .eventName(event.getEventName())
                .eventDate(event.getEventDate())
                .eventDuration(event.getEventDuration())
                .ageRequired(event.getAgeRequired())
                .maxParticipantAmount(event.getMaxParticipantAmount())
                .eventStatus(event.getEventStatus())
                .eventRegistrationStatus(event.getEventRegistrationStatus())
                .eventGenderRequirement(event.getEventGenderRequirement())
                .build();
    }

    private ParticipantResponse checkValidCreateRequest(ParticipantCreateRequest request) {
        if (request == null) {
            return ParticipantResponse.builder()
                    .firstName("Error: request cannot be null")
                    .build();
        }

        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            return ParticipantResponse.builder()
                    .firstName("Error: first name is required")
                    .build();
        }

        if (request.getLastName() == null || request.getLastName().isBlank()) {
            return ParticipantResponse.builder()
                    .firstName("Error: last name is required")
                    .build();
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ParticipantResponse.builder()
                    .firstName("Error: email is required")
                    .build();
        }

        if (!request.getEmail().matches(".+@.+\\..+")) {
            return ParticipantResponse.builder()
                    .firstName("Error: invalid email format")
                    .build();
        }

        if (request.getAge() <= 0 || request.getAge() > 150) {
            return ParticipantResponse.builder()
                    .firstName("Invalid age: " + request.getAge())
                    .build();
        }

        if (request.getParticipantGender() == null) {
            return ParticipantResponse.builder()
                    .firstName("Error: gender is required")
                    .build();
        }
        return null;
    }

    private EventResponse checkValidCreateEvent(EventCreateRequest request)
    {
        if (request == null) {
        return EventResponse.builder()
                .eventName("Error: request cannot be null")
                .build();
        }

        if (request.getEventName() == null || request.getEventName().isBlank()) {
            return EventResponse.builder()
                    .eventName("Error: event name cannot be empty")
                    .build();
        }

        if (request.getEventDate() == null) {
            return EventResponse.builder()
                    .eventName("Error: event date is required")
                    .build();
        }

        if (request.getEventDate().isBefore(OffsetDateTime.now())) {
            return EventResponse.builder()
                    .eventName("Error: event date cannot be in the past")
                    .build();
        }

        if (request.getEventDuration() == null || request.getEventDuration().isNegative() || request.getEventDuration().isZero()) {
            return EventResponse.builder()
                    .eventName("Error: duration must be positive")
                    .build();
        }

        if (request.getAgeRequired() < 0 || request.getAgeRequired() > 150) {
            return EventResponse.builder()
                    .eventName("Invalid age: " + request.getAgeRequired())
                    .build();
        }

        if (request.getGenderRequirement() == null) {
            return EventResponse.builder()
                    .eventName("Error: gender requirement must be specified (MALE_ONLY/FEMALE_ONLY/ANY)")
                    .build();
        }

        if (request.getMaxParticipantAmount() <= 0) {
            return EventResponse.builder()
                    .eventName("Error: maximum participant amount must be > 0")
                    .build();
        }

        return null;
    }

}
