package service.implementation;

import collection.SimpleLinkedList;
import dto.*;
import exception.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import model.*;
import model.Event;
import model.enums.*;
import repository.EventRegistrationRepository;
import repository.EventRepository;
import repository.ParticipantRepository;
import service.EventService;

// @Component
// @Primary
public class EventServiceImpl implements EventService {

  private final ParticipantRepository participantRepository;
  private final EventRepository eventRepository;
  private final EventRegistrationRepository eventRegistrationRepository;

  private final SimpleLinkedList<ActionData> actionHistory = new SimpleLinkedList<>();

  public EventServiceImpl(
      EventRepository eventRepository,
      ParticipantRepository participantRepository,
      EventRegistrationRepository registrationRepository) {
    this.eventRepository = eventRepository;
    this.participantRepository = participantRepository;
    this.eventRegistrationRepository = registrationRepository;
  }

  @Override
  public UndoResponse getLatestAction() {
    if (actionHistory.isEmpty()) {
      throw new EventRegException("no action to undo", "undoLatestAction");
    }
    ActionData actionData = actionHistory.getLast();

    return UndoResponse.builder()
        .description("last action type to undo")
        .type(actionData.getActionType())
        .build();
  }

  @Override
  public UndoResponse undoLatestAction() {

    if (actionHistory.isEmpty()) {
      throw new EventRegException("no action to undo", "undoLatestAction");
    }

    ActionData actionData = actionHistory.removeLast();

    switch (actionData.getActionType()) {
      case ActionType.CREATE_EVENT:
        deleteEvent(actionData.getEventId());
        return UndoResponse.builder()
            .description("successfully undone event creation" + actionData.getEventId())
            .type(actionData.getActionType())
            .build();

      case ActionType.CREATE_PARTICIPANT:
        deleteParticipant(actionData.getParticipantId());
        return UndoResponse.builder()
            .description("successfully undone participant creation" + actionData.getParticipantId())
            .type(actionData.getActionType())
            .build();

      case ActionType.REGISTER_PARTICIPANT:
        deleteParticipantRegistration(actionData.getRegisteredRegistrationId());
        return UndoResponse.builder()
            .description(
                "successfully undone participant registration"
                    + actionData.getRegisteredRegistrationId())
            .type(actionData.getActionType())
            .build();

      case ActionType.CANCEL_REGISTRATION:
        if (actionData.getRegisteredRegistrationId() != null) {
          EventRegistration eventRegistration =
              eventRegistrationRepository.findById(actionData.getRegisteredRegistrationId());
          if (eventRegistration == null) {
            throw new RegistrationNotFoundException(
                actionData.getRegisteredRegistrationId(), "undoLatestAction");
          }
          changeRegistrationRequestStatus(
              actionData.getRegisteredRegistrationId(),
              actionData.getRegisteredEventRegRequestStatus(),
              "cancelled reg request after undo",
              false);
        }

        changeRegistrationRequestStatus(
            actionData.getCancelledRegistrationId(),
            actionData.getCancelledEventRegRequestStatus(),
            "uncancelled request",
            false);

        return UndoResponse.builder()
            .description(
                "successfully undone registration cancellation"
                    + actionData.getCancelledRegistrationId())
            .type(actionData.getActionType())
            .build();

      default:
        throw new EventRegException("Invalid action type", "undoLatestAction");
    }
  }

  @Override
  public EventResponse createEvent(EventCreateRequest eventCreateRequest) {

    checkValidCreateEvent(eventCreateRequest);

    if (eventRepository.existsByName(eventCreateRequest.getEventName())) {
      throw new DuplicateException(
          "Event with name " + eventCreateRequest.getEventName() + " already exists",
          "createEvent");
    }

    Event event =
        Event.builder()
            .createdAt(OffsetDateTime.now())
            .eventName(eventCreateRequest.getEventName())
            .location(eventCreateRequest.getLocation())
            .eventDate(eventCreateRequest.getEventDate())
            .eventDuration(eventCreateRequest.getEventDuration())
            .eventStatus(EventStatus.PLANNED)
            .eventRegistrationStatus(EventRegistrationStatus.RESERVATIONS_OPEN)
            .ageRequired(eventCreateRequest.getAgeRequired())
            .maxParticipantAmount(eventCreateRequest.getMaxParticipantAmount())
            .currentParticipantAmount(0)
            .eventGenderRequirement(eventCreateRequest.getGenderRequirement())
            .build();

    eventRepository.save(event);

    addActionToHistory(ActionType.CREATE_EVENT, null, event.getId(), null, null, null, null);

    return eventToEventResponse(event);
  }

  private void deleteEvent(Integer eventId) {

    eventRepository.delete(eventId);
  }

  @Override
  public ParticipantResponse createParticipant(ParticipantCreateRequest participantCreateRequest) {

    checkValidCreateRequest(participantCreateRequest);

    if (participantRepository.existsByEmail(participantCreateRequest.getEmail())) {
      throw new DuplicateException(
          "Participant with email " + participantCreateRequest.getEmail() + " already exists",
          "createParticipant");
    }

    Participant participant =
        Participant.builder()
            .firstName(participantCreateRequest.getFirstName())
            .lastName(participantCreateRequest.getLastName())
            .email(participantCreateRequest.getEmail())
            .age(participantCreateRequest.getAge())
            .participantGender(participantCreateRequest.getParticipantGender())
            .registeredAt(OffsetDateTime.now())
            .build();

    participantRepository.save(participant);

    addActionToHistory(
        ActionType.CREATE_PARTICIPANT, participant.getId(), null, null, null, null, null);

    return ParticipantResponse.builder()
        .participantId(participant.getId())
        .firstName(participant.getFirstName())
        .lastName(participant.getLastName())
        .email(participant.getEmail())
        .age(participant.getAge())
        .participantGender(participant.getParticipantGender())
        .registeredAt(participant.getRegisteredAt())
        .build();
  }

  private void deleteParticipant(Integer participantId) {

    participantRepository.delete(participantId);
  }

  @Override
  public EventRegResponse registerParticipant(EventRegRequest eventRegRequest) {

    Participant participant = participantRepository.findById(eventRegRequest.getParticipantId());
    if (participant == null) {
      throw new ParticipantNotFoundException(
          eventRegRequest.getParticipantId(), "registerParticipant");
    }

    Event event = eventRepository.findById(eventRegRequest.getEventId());
    if (event == null) {
      throw new EventNotFoundException(eventRegRequest.getEventId(), "registerParticipant");
    }

    EventRegistration eventRegistration =
        EventRegistration.builder()
            .participantId(eventRegRequest.getParticipantId())
            .eventId(eventRegRequest.getEventId())
            .eventRegRequestStatus(EventRegRequestStatus.PENDING)
            .description("Request undergoing review...")
            .createdAt(OffsetDateTime.now())
            .build();

    eventRegistrationRepository.save(eventRegistration);

    reviewParticipant(participant, event, eventRegistration);

    //    CompletableFuture.runAsync(() -> reviewParticipant(participant, event,
    // eventRegistration));

    addActionToHistory(
        ActionType.REGISTER_PARTICIPANT,
        participant.getId(),
        event.getId(),
        eventRegistration.getId(),
        null,
        null,
        null);

    return eventRegistrationToEventRegResponse(
        eventRegistrationRepository.findById(eventRegistration.getId()));
  }

  private void deleteParticipantRegistration(Integer participantRegistrationId) {

    EventRegistration eventRegistration =
        eventRegistrationRepository.findById(participantRegistrationId);

    if (eventRegistration != null) {
      Event event = eventRepository.findById(eventRegistration.getEventId());
      if (event == null) {
        throw new EventNotFoundException(
            eventRegistration.getEventId(),
            eventRegistration.getId(),
            "deleteParticipantRegistration");
      }
      if (eventRegistration.getEventRegRequestStatus().equals(EventRegRequestStatus.ACCEPTED)) {
        event.setCurrentParticipantAmount(event.getCurrentParticipantAmount() - 1);
        eventRepository.update(event);
      }
      if (eventRegistration.getEventRegRequestStatus().equals(EventRegRequestStatus.WAITING)) {
        eventRegistrationRepository.removeFromWaitingQueue(eventRegistration.getId());
      }

      eventRegistrationRepository.delete(participantRegistrationId);
    }
  }

  @Override
  public List<ParticipantResponse> getParticipants() {

    return participantRepository.findAll().stream()
        .map(
            participant ->
                ParticipantResponse.builder()
                    .participantId(participant.getId())
                    .firstName(participant.getFirstName())
                    .lastName(participant.getLastName())
                    .email(participant.getEmail())
                    .age(participant.getAge())
                    .participantGender(participant.getParticipantGender())
                    .registeredAt(participant.getRegisteredAt())
                    .build())
        .toList();
  }

  @Override
  public ParticipantResponse getParticipantById(int participantId) {
    Participant participant = participantRepository.findById(participantId);
    if (participant != null) {
      return ParticipantResponse.builder()
          .participantId(participant.getId())
          .firstName(participant.getFirstName())
          .lastName(participant.getLastName())
          .email(participant.getEmail())
          .age(participant.getAge())
          .participantGender(participant.getParticipantGender())
          .registeredAt(participant.getRegisteredAt())
          .build();
    } else {
      throw new ParticipantNotFoundException(participantId, "getParticipantById");
    }
  }

  @Override
  public List<ParticipantResponse> getParticipantsSorted(Comparator<Participant> comparator) {
    return participantRepository.findAll().stream()
        .sorted(comparator)
        .map(this::participantToParticipantResponse)
        .collect(Collectors.toList());
  }

  @Override
  public List<EventResponse> getEvents() {
    return eventRepository.findAll().stream().map(this::eventToEventResponse).toList();
  }

  @Override
  public List<EventResponse> getEventsFiltered(List<Predicate<Event>> predicates) {
    return eventRepository.findAll().stream()
        .filter(predicates.stream().reduce(Predicate::and).orElse(event -> true))
        .map(this::eventToEventResponse)
        .toList();
  }

  @Override
  public EventSummary getEventSummary(int eventId) {
    EventSummary eventSummary = eventRepository.getEventSummary(eventId);
    if (eventSummary != null) {
      return (eventSummary);
    } else {
      throw new EventNotFoundException(eventId, "getEventSummary");
    }
  }

  @Override
  public Map<String, Long> groupByFillStatus() {
    Map<String, Long> result = eventRepository.groupByFillStatus();
    if (!result.isEmpty()) {
      return result;
    } else {
      throw new NoEventsPresentException("no events have been found", "groupByFillStatus");
    }
  }

  @Override
  public List<Event> findMostPopular(int limit) {
    List<Event> result = eventRepository.findMostPopular(limit);
    if (!result.isEmpty()) {
      return result;
    } else {
      throw new NoEventsPresentException("no events have been found", "findMostPopular");
    }
  }

  @Override
  public List<EventRegistration> findByCreatedBetween(OffsetDateTime from, OffsetDateTime to) {
    return eventRegistrationRepository.findByCreatedBetween(from, to);
  }

  @Override
  public List<Participant> searchByFragment(String fragment) {
    return participantRepository.searchByFragment(fragment);
  }

  @Override
  public Map<String, List<EventResponse>> getEventsGrouped(Function<Event, String> classifier) {
    return eventRepository.findAll().stream()
        .collect(
            Collectors.groupingBy(
                classifier, Collectors.mapping(this::eventToEventResponse, Collectors.toList())));
  }

  @Override
  public EventResponse getEventById(int eventId) {
    Event event = eventRepository.findById(eventId);
    if (event != null) {
      return EventResponse.builder()
          .eventId(event.getId())
          .eventName(event.getEventName())
          .location(event.getLocation())
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
      throw new EventNotFoundException(eventId, "getEventById");
    }
  }

  @Override
  public List<EventRegResponse> getRegistrationRequests() {
    return eventRegistrationRepository.findAll().stream()
        .map(this::eventRegistrationToEventRegResponse)
        .toList();
  }

  @Override
  public EventRegResponse getRegistrationRequestById(int eventRegistrationId) {
    EventRegistration registration = eventRegistrationRepository.findById(eventRegistrationId);
    if (registration != null) {
      return eventRegistrationToEventRegResponse(registration);
    } else {
      throw new RegistrationNotFoundException(eventRegistrationId, "getRegistrationRequestById");
    }
  }

  @Override
  public List<EventRegResponse> getRegistrationRequestsInWaitingQueue(Integer eventId) {
    return eventRegistrationRepository.findAllInWaitingQueue().stream()
        .filter(e -> e.getEventId() == eventId)
        .map(this::eventRegistrationToEventRegResponse)
        .toList();
  }

  private void pollWaitingQueue(int eventId) {
    EventRegistration eventRegistration = eventRegistrationRepository.pollWaitingQueue(eventId);
    if (eventRegistration != null) {

      Event event = eventRepository.findById(eventRegistration.getEventId());
      if (event == null) {
        throw new EventNotFoundException(
            eventRegistration.getEventId(), eventRegistration.getId(), "pollWaitingQueue");
      }
      changeRegistrationRequestStatus(
          eventRegistration.getId(),
          EventRegRequestStatus.ACCEPTED,
          "automatic accept after waiting for queue turn",
          false);
    }
  }

  // TODO: снести и переделать на логику чище когда будет переход на spring
  @Override
  public EventRegResponse changeRegistrationRequestStatus(
      int registrationId,
      EventRegRequestStatus eventRegRequestStatus,
      String description,
      Boolean addToHistory) {
    EventRegistration eventRegistration = eventRegistrationRepository.findById(registrationId);
    if (eventRegistration != null) {
      if (eventRegRequestStatus == eventRegistration.getEventRegRequestStatus()) {
        throw new EventRegException(
            "EventRegistration already has the exact same status" + eventRegistration.getId(),
            "changeRegistrationRequestStatus");
      }

      Event event = eventRepository.findById(eventRegistration.getEventId());
      if (event == null) {
        throw new EventNotFoundException(
            eventRegistration.getEventId(),
            eventRegistration.getId(),
            "changeRegistrationRequestStatus");
      }

      Integer registeredRegistrationId = null;
      EventRegRequestStatus registeredEventRegRequestStatus = null;

      if (eventRegistration.getEventRegRequestStatus().equals(EventRegRequestStatus.ACCEPTED)) {
        event.setCurrentParticipantAmount(event.getCurrentParticipantAmount() - 1);
        eventRepository.update(event);
        List<EventRegResponse> waiting = getRegistrationRequestsInWaitingQueue(event.getId());
        if (!waiting.isEmpty()) {
          registeredRegistrationId = waiting.getFirst().getRegistrationId();
          registeredEventRegRequestStatus = waiting.getFirst().getEventRegRequestStatus();

          pollWaitingQueue(eventRegistration.getEventId());
          event = eventRepository.findById(eventRegistration.getEventId());
        }
      }

      switch (eventRegRequestStatus) {
        case EventRegRequestStatus.WAITING:
          addToWaitingQueue(event, eventRegistration);
          break;
        case EventRegRequestStatus.ACCEPTED:
          if (event.getCurrentParticipantAmount() + 1 > event.getMaxParticipantAmount()) {
            throw new EventCapacityExceededException(
                event.getId(), "changeRegistrationRequestStatus");
          }
          if (event.getCurrentParticipantAmount() == (event.getMaxParticipantAmount())
              && event
                  .getEventRegistrationStatus()
                  .equals(EventRegistrationStatus.RESERVATIONS_OPEN)) {
            event.setEventRegistrationStatus(EventRegistrationStatus.ALL_RESERVED);
          }
          event.setCurrentParticipantAmount(event.getCurrentParticipantAmount() + 1);
          break;
        case EventRegRequestStatus.DENIED:
          break;
        case EventRegRequestStatus.CANCELLED:
          if (addToHistory) {
            addActionToHistory(
                ActionType.CANCEL_REGISTRATION,
                null,
                event.getId(),
                registeredRegistrationId,
                eventRegistration.getId(),
                registeredEventRegRequestStatus,
                eventRegistration.getEventRegRequestStatus());
          }
          eventRegistration.setEventRegRequestStatus(EventRegRequestStatus.CANCELLED);
      }

      eventRegistration.setEventRegRequestStatus(eventRegRequestStatus);
      eventRegistration.setDescription(description);
      eventRepository.update(event);

      eventRegistrationRepository.update(eventRegistration);

      return eventRegistrationToEventRegResponse(eventRegistration);
    } else {
      throw new RegistrationNotFoundException(registrationId, "changeRegistrationRequestStatus");
    }
  }

  private void addToWaitingQueue(Event event, EventRegistration eventRegistration) {
    eventRegistrationRepository.addToWaitingQueue(eventRegistration);
    event.setEventRegistrationStatus(EventRegistrationStatus.WAITLIST);
  }

  private void reviewParticipant(
      Participant participant, Event event, EventRegistration eventRegistration) {
    if (!(event.getEventRegistrationStatus() == EventRegistrationStatus.RESERVATIONS_OPEN)) {
      changeRegistrationRequestStatus(
          eventRegistration.getId(),
          model.enums.EventRegRequestStatus.DENIED,
          "Reservations are not open!",
          false);
      return;
    }

    if (participant.getAge() < event.getAgeRequired()) {
      changeRegistrationRequestStatus(
          eventRegistration.getId(),
          model.enums.EventRegRequestStatus.DENIED,
          "Participant is out of age!",
          false);
      return;
    }

    if (event.getCurrentParticipantAmount() >= event.getMaxParticipantAmount()) {
      if (event.getEventRegistrationStatus().equals(EventRegistrationStatus.RESERVATIONS_OPEN)) {
        event.setEventRegistrationStatus(EventRegistrationStatus.ALL_RESERVED);
      }
      eventRepository.update(event);
      changeRegistrationRequestStatus(
          eventRegistration.getId(),
          model.enums.EventRegRequestStatus.DENIED,
          "All spots reserved",
          false);
      return;
    }

    if (event.getEventGenderRequirement() != EventGenderRequirement.NONE) {
      if (participant.getParticipantGender() == ParticipantGender.MALE
          && (event.getEventGenderRequirement() == EventGenderRequirement.FEMALE_ONLY)) {
        changeRegistrationRequestStatus(
            eventRegistration.getId(), EventRegRequestStatus.DENIED, "Event is female only", false);
        return;
      }
      if (participant.getParticipantGender() == ParticipantGender.FEMALE
          && (event.getEventGenderRequirement() == EventGenderRequirement.MALE_ONLY)) {
        changeRegistrationRequestStatus(
            eventRegistration.getId(), EventRegRequestStatus.DENIED, "Event is male only", false);
        return;
      }
      if (participant.getParticipantGender() == ParticipantGender.NOT_SPECIFIED) {
        changeRegistrationRequestStatus(
            eventRegistration.getId(),
            EventRegRequestStatus.DENIED,
            "Event requires gender specified",
            false);
        return;
      }
    }

    changeRegistrationRequestStatus(
        eventRegistration.getId(), EventRegRequestStatus.ACCEPTED, "Reservation accepted", false);
  }

  private void checkValidCreateRequest(ParticipantCreateRequest request) {
    if (request == null) {
      throw new IllegalArgumentEventRegException(
          "request cannot be null", "checkValidCreateRequest");
    }

    if (request.getFirstName() == null || request.getFirstName().isBlank()) {
      throw new IllegalArgumentEventRegException(
          "first name is required", "checkValidCreateRequest");
    }

    if (request.getLastName() == null || request.getLastName().isBlank()) {
      throw new IllegalArgumentEventRegException(
          "last name is required", "checkValidCreateRequest");
    }

    if (request.getEmail() == null || request.getEmail().isBlank()) {
      throw new IllegalArgumentEventRegException("email is required", "checkValidCreateRequest");
    }

    if (!request.getEmail().matches(".+@.+\\..+")) {
      throw new IllegalArgumentEventRegException("invalid email format", "checkValidCreateRequest");
    }

    if (request.getAge() <= 0 || request.getAge() > 150) {
      throw new IllegalArgumentEventRegException(
          "Invalid age: " + request.getAge(), "checkValidCreateRequest");
    }

    if (request.getParticipantGender() == null) {
      throw new IllegalArgumentEventRegException("gender is required", "checkValidCreateRequest");
    }
  }

  private void checkValidCreateEvent(EventCreateRequest request) {
    if (request == null) {
      throw new IllegalArgumentEventRegException(
          "EventCreateRequest cannot be null", "checkValidCreateEvent");
    }
    if (request.getEventName() == null || request.getEventName().isBlank()) {
      throw new IllegalArgumentEventRegException(
          "Event name cannot be empty", "checkValidCreateEvent");
    }
    if (request.getLocation() == null || request.getLocation().isBlank()) {
      throw new IllegalArgumentEventRegException(
          "Event location is required", "checkValidCreateEvent");
    }
    if (request.getEventDate() == null) {
      throw new IllegalArgumentEventRegException("Event date is required", "checkValidCreateEvent");
    }
    if (request.getEventDate().isBefore(OffsetDateTime.now())) {
      throw new IllegalArgumentEventRegException(
          "Event date cannot be in the past: " + request.getEventDate(), "checkValidCreateEvent");
    }
    if (request.getEventDuration() == null
        || request.getEventDuration().isNegative()
        || request.getEventDuration().isZero()) {
      throw new IllegalArgumentEventRegException(
          "Duration must be positive", "checkValidCreateEvent");
    }
    if (request.getAgeRequired() < 0 || request.getAgeRequired() > 150) {
      throw new IllegalArgumentEventRegException(
          "Age must be 0-150, got: " + request.getAgeRequired(), "checkValidCreateEvent");
    }
    if (request.getGenderRequirement() == null) {
      throw new IllegalArgumentEventRegException(
          "Gender requirement must be MALE_ONLY, FEMALE_ONLY, or NONE", "checkValidCreateEvent");
    }
    if (request.getMaxParticipantAmount() <= 0) {
      throw new IllegalArgumentEventRegException(
          "Max participants must be > 0, got: " + request.getMaxParticipantAmount(),
          "checkValidCreateEvent");
    }
  }

  private EventRegResponse eventRegistrationToEventRegResponse(
      EventRegistration eventRegistration) {

    Participant participant = participantRepository.findById(eventRegistration.getParticipantId());
    if (participant == null) {
      throw new ParticipantNotFoundException(
          eventRegistration.getParticipantId(), "eventRegistrationToEventRegResponse");
    }

    Event event = eventRepository.findById(eventRegistration.getEventId());
    if (event == null) {
      throw new EventNotFoundException(
          eventRegistration.getEventId(), "eventRegistrationToEventRegResponse");
    }

    return EventRegResponse.builder()
        .registrationId(eventRegistration.getId())
        .participantId(eventRegistration.getParticipantId())
        .eventId(eventRegistration.getEventId())
        .eventResponse(eventToEventResponse(event))
        .participantResponse(participantToParticipantResponse(participant))
        .eventRegRequestStatus(eventRegistration.getEventRegRequestStatus())
        .description(eventRegistration.getDescription())
        .build();
  }

  private EventResponse eventToEventResponse(Event event) {
    return EventResponse.builder()
        .eventId(event.getId())
        .eventName(event.getEventName())
        .location(event.getLocation())
        .eventDate(event.getEventDate())
        .eventDuration(event.getEventDuration())
        .ageRequired(event.getAgeRequired())
        .currentParticipantAmount(event.getCurrentParticipantAmount())
        .maxParticipantAmount(event.getMaxParticipantAmount())
        .eventGenderRequirement(event.getEventGenderRequirement())
        .eventStatus(event.getEventStatus())
        .eventRegistrationStatus(event.getEventRegistrationStatus())
        .build();
  }

  private ParticipantResponse participantToParticipantResponse(Participant participant) {
    return ParticipantResponse.builder()
        .participantId(participant.getId())
        .firstName(participant.getFirstName())
        .lastName(participant.getLastName())
        .email(participant.getEmail())
        .age(participant.getAge())
        .participantGender(participant.getParticipantGender())
        .registeredAt(participant.getRegisteredAt())
        .build();
  }

  private void addActionToHistory(
      ActionType actionType,
      Integer participantId,
      Integer eventId,
      Integer registeredRegistrationId,
      Integer cancelledRegistrationId,
      EventRegRequestStatus registeredEventRegRequestStatus,
      EventRegRequestStatus cancelledEventRegRequestStatus) {
    actionHistory.addLast(
        ActionData.builder()
            .actionType(actionType)
            .participantId(participantId)
            .eventId(eventId)
            .registeredRegistrationId(registeredRegistrationId)
            .cancelledRegistrationId(cancelledRegistrationId)
            .registeredEventRegRequestStatus(registeredEventRegRequestStatus)
            .cancelledEventRegRequestStatus(cancelledEventRegRequestStatus)
            .build());
  }
}
