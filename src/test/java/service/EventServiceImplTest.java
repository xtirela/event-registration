package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dto.EventCreateRequest;
import dto.EventRegRequest;
import dto.EventRegResponse;
import dto.EventResponse;
import dto.ParticipantCreateRequest;
import dto.ParticipantResponse;
import dto.UndoResponse;
import exception.EventCapacityExceededException;
import exception.EventNotFoundException;
import exception.EventRegException;
import exception.IllegalArgumentEventRegException;
import exception.ParticipantNotFoundException;
import exception.RegistrationNotFoundException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import model.Event;
import model.Participant;
import model.enums.ActionType;
import model.enums.EventGenderRequirement;
import model.enums.EventRegRequestStatus;
import model.enums.EventRegistrationStatus;
import model.enums.EventStatus;
import model.enums.ParticipantGender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.implementation.EventServiceImpl;

public class EventServiceImplTest {

  private EventService service;

  @BeforeEach
  void setUp() {
    service = new EventServiceImpl();
  }

  // ---------- arrange helpers ----------

  private EventCreateRequest event(
      String name,
      OffsetDateTime date,
      Duration duration,
      int ageRequired,
      int max,
      EventGenderRequirement gender) {
    return EventCreateRequest.builder()
        .eventName(name)
        .location("Main Hall")
        .eventDate(date)
        .eventDuration(duration)
        .ageRequired(ageRequired)
        .maxParticipantAmount(max)
        .genderRequirement(gender)
        .build();
  }

  private EventCreateRequest event(String name, int ageRequired, int max) {
    return event(
        name,
        OffsetDateTime.now().plusDays(30),
        Duration.ofHours(2),
        ageRequired,
        max,
        EventGenderRequirement.NONE);
  }

  private ParticipantCreateRequest participant(String email, int age, ParticipantGender gender) {
    return ParticipantCreateRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email(email)
        .age(age)
        .participantGender(gender)
        .build();
  }

  private EventRegResponse register(int participantId, int eventId) {
    return service.registerParticipant(new EventRegRequest(participantId, eventId));
  }

  private void arrangeEvent() {
    service.createEvent(event("Tech Conference", 18, 100));
  }

  private void arrangeEventAndParticipant() {
    service.createEvent(event("Tech Conference", 18, 100));
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
  }

  private void arrangeAcceptedRegistration() {
    arrangeEventAndParticipant();
    register(1, 1);
  }

  private void registerMissingParticipant() {
    try {
      register(1, 1);
    } catch (ParticipantNotFoundException expected) {
      // participant missing: registration 1 is saved as DENIED, then exception is thrown
    }
  }

  private void registerMissingEvent() {
    try {
      register(1, 999);
    } catch (EventNotFoundException expected) {
      // event missing: registration 1 is saved as DENIED, then exception is thrown
    }
  }

  private void arrangeWaitingQueueScenario() {
    service.createEvent(event("Full Event", 18, 1));
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    service.createParticipant(participant("jane@test.com", 25, ParticipantGender.FEMALE));
    register(1, 1);
    register(2, 1);
    service.changeRegistrationRequestStatus(2, EventRegRequestStatus.WAITING, "waiting", false);
  }

  // ---------- createEvent ----------

  @Test
  void givenValidEvent_whenCreateEvent_thenIdIsOne() {
    EventResponse response = service.createEvent(event("Tech Conference", 18, 100));

    assertEquals(1, response.getEventId());
  }

  @Test
  void givenValidEvent_whenCreateEvent_thenNameIsSaved() {
    EventResponse response = service.createEvent(event("Tech Conference", 18, 100));

    assertEquals("Tech Conference", response.getEventName());
  }

  @Test
  void givenValidEvent_whenCreateEvent_thenLocationIsSaved() {
    EventResponse response = service.createEvent(event("Tech Conference", 18, 100));

    assertEquals("Main Hall", response.getLocation());
  }

  @Test
  void givenValidEvent_whenCreateEvent_thenStatusIsPlanned() {
    EventResponse response = service.createEvent(event("Tech Conference", 18, 100));

    assertEquals(EventStatus.PLANNED, response.getEventStatus());
  }

  @Test
  void givenValidEvent_whenCreateEvent_thenRegistrationStatusIsOpen() {
    EventResponse response = service.createEvent(event("Tech Conference", 18, 100));

    assertEquals(EventRegistrationStatus.RESERVATIONS_OPEN, response.getEventRegistrationStatus());
  }

  @Test
  void givenValidEvent_whenCreateEvent_thenCurrentParticipantAmountIsZero() {
    EventResponse response = service.createEvent(event("Tech Conference", 18, 100));

    assertEquals(0, response.getCurrentParticipantAmount());
  }

  @Test
  void givenValidEvent_whenCreateEvent_thenAgeRequiredIsSaved() {
    EventResponse response = service.createEvent(event("Tech Conference", 18, 100));

    assertEquals(18, response.getAgeRequired());
  }

  @Test
  void givenValidEvent_whenCreateEvent_thenMaxParticipantAmountIsSaved() {
    EventResponse response = service.createEvent(event("Tech Conference", 18, 100));

    assertEquals(100, response.getMaxParticipantAmount());
  }

  @Test
  void givenMaxParticipantAmountOne_whenCreateEvent_thenEventCreated() {
    EventResponse response = service.createEvent(event("Small Event", 18, 1));

    assertEquals(1, response.getEventId());
  }

  @Test
  void givenAgeRequiredZero_whenCreateEvent_thenEventCreated() {
    EventResponse response = service.createEvent(event("Kids Event", 0, 100));

    assertEquals(1, response.getEventId());
  }

  @Test
  void givenAgeRequiredHundredFifty_whenCreateEvent_thenEventCreated() {
    EventResponse response = service.createEvent(event("Seniors Event", 150, 100));

    assertEquals(1, response.getEventId());
  }

  @Test
  void givenEventDateOneSecondAhead_whenCreateEvent_thenEventCreated() {
    EventCreateRequest request =
        event(
            "Soon Event",
            OffsetDateTime.now().plusSeconds(1),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.NONE);

    EventResponse response = service.createEvent(request);

    assertEquals(1, response.getEventId());
  }

  @Test
  void givenDurationOneMinute_whenCreateEvent_thenEventCreated() {
    EventCreateRequest request =
        event(
            "Flash Event",
            OffsetDateTime.now().plusDays(30),
            Duration.ofMinutes(1),
            18,
            100,
            EventGenderRequirement.NONE);

    EventResponse response = service.createEvent(request);

    assertEquals(1, response.getEventId());
  }

  @Test
  void givenNullEventRequest_whenCreateEvent_thenError() {
    assertThrows(IllegalArgumentEventRegException.class, () -> service.createEvent(null));
  }

  @Test
  void givenEmptyEventName_whenCreateEvent_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class, () -> service.createEvent(event("", 18, 100)));
  }

  @Test
  void givenBlankEventName_whenCreateEvent_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class, () -> service.createEvent(event("   ", 18, 100)));
  }

  @Test
  void givenNullLocation_whenCreateEvent_thenError() {
    EventCreateRequest request =
        event(
            "No Place",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.NONE);
    request.setLocation(null);

    assertThrows(IllegalArgumentEventRegException.class, () -> service.createEvent(request));
  }

  @Test
  void givenNullEventName_whenCreateEvent_thenError() {
    EventCreateRequest request =
        event(
            "No Name",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.NONE);
    request.setEventName(null);

    assertThrows(IllegalArgumentEventRegException.class, () -> service.createEvent(request));
  }

  @Test
  void givenBlankLocation_whenCreateEvent_thenError() {
    EventCreateRequest request =
        event(
            "No Place",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.NONE);
    request.setLocation(" ");

    assertThrows(IllegalArgumentEventRegException.class, () -> service.createEvent(request));
  }

  @Test
  void givenNullEventDate_whenCreateEvent_thenError() {
    EventCreateRequest request =
        event(
            "No Date",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.NONE);
    request.setEventDate(null);

    assertThrows(IllegalArgumentEventRegException.class, () -> service.createEvent(request));
  }

  @Test
  void givenPastEventDate_whenCreateEvent_thenError() {
    EventCreateRequest request =
        event(
            "Past Event",
            OffsetDateTime.now().minusDays(1),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.NONE);

    assertThrows(IllegalArgumentEventRegException.class, () -> service.createEvent(request));
  }

  @Test
  void givenNullDuration_whenCreateEvent_thenError() {
    EventCreateRequest request =
        event(
            "No Duration",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.NONE);
    request.setEventDuration(null);

    assertThrows(IllegalArgumentEventRegException.class, () -> service.createEvent(request));
  }

  @Test
  void givenZeroDuration_whenCreateEvent_thenError() {
    EventCreateRequest request =
        event(
            "Zero Duration",
            OffsetDateTime.now().plusDays(30),
            Duration.ZERO,
            18,
            100,
            EventGenderRequirement.NONE);

    assertThrows(IllegalArgumentEventRegException.class, () -> service.createEvent(request));
  }

  @Test
  void givenNegativeDuration_whenCreateEvent_thenError() {
    EventCreateRequest request =
        event(
            "Negative Duration",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(-1),
            18,
            100,
            EventGenderRequirement.NONE);

    assertThrows(IllegalArgumentEventRegException.class, () -> service.createEvent(request));
  }

  @Test
  void givenNegativeAgeRequired_whenCreateEvent_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> service.createEvent(event("Bad Age", -1, 100)));
  }

  @Test
  void givenAgeRequiredAboveMax_whenCreateEvent_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> service.createEvent(event("Bad Age", 151, 100)));
  }

  @Test
  void givenNullGenderRequirement_whenCreateEvent_thenError() {
    EventCreateRequest request =
        event(
            "No Gender",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.NONE);
    request.setGenderRequirement(null);

    assertThrows(IllegalArgumentEventRegException.class, () -> service.createEvent(request));
  }

  @Test
  void givenZeroMaxParticipantAmount_whenCreateEvent_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class, () -> service.createEvent(event("Bad Max", 18, 0)));
  }

  @Test
  void givenNegativeMaxParticipantAmount_whenCreateEvent_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> service.createEvent(event("Bad Max", 18, -1)));
  }

  // ---------- createParticipant ----------

  @Test
  void givenValidParticipant_whenCreateParticipant_thenIdIsOne() {
    ParticipantResponse response =
        service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertEquals(1, response.getParticipantId());
  }

  @Test
  void givenValidParticipant_whenCreateParticipant_thenFirstNameIsSaved() {
    ParticipantResponse response =
        service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertEquals("John", response.getFirstName());
  }

  @Test
  void givenValidParticipant_whenCreateParticipant_thenLastNameIsSaved() {
    ParticipantResponse response =
        service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertEquals("Doe", response.getLastName());
  }

  @Test
  void givenValidParticipant_whenCreateParticipant_thenEmailIsSaved() {
    ParticipantResponse response =
        service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertEquals("john@test.com", response.getEmail());
  }

  @Test
  void givenValidParticipant_whenCreateParticipant_thenAgeIsSaved() {
    ParticipantResponse response =
        service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertEquals(25, response.getAge());
  }

  @Test
  void givenValidParticipant_whenCreateParticipant_thenGenderIsSaved() {
    ParticipantResponse response =
        service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertEquals(ParticipantGender.MALE, response.getParticipantGender());
  }

  @Test
  void givenValidParticipant_whenCreateParticipant_thenRegisteredAtIsSet() {
    ParticipantResponse response =
        service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertNotNull(response.getRegisteredAt());
  }

  @Test
  void givenAgeOne_whenCreateParticipant_thenParticipantCreated() {
    ParticipantResponse response =
        service.createParticipant(participant("baby@test.com", 1, ParticipantGender.MALE));

    assertEquals(1, response.getParticipantId());
  }

  @Test
  void givenAgeHundredFifty_whenCreateParticipant_thenParticipantCreated() {
    ParticipantResponse response =
        service.createParticipant(participant("elder@test.com", 150, ParticipantGender.MALE));

    assertEquals(1, response.getParticipantId());
  }

  @Test
  void givenNullParticipantRequest_whenCreateParticipant_thenError() {
    assertThrows(IllegalArgumentEventRegException.class, () -> service.createParticipant(null));
  }

  @Test
  void givenBlankFirstName_whenCreateParticipant_thenError() {
    ParticipantCreateRequest request = participant("john@test.com", 25, ParticipantGender.MALE);
    request.setFirstName(" ");

    assertThrows(IllegalArgumentEventRegException.class, () -> service.createParticipant(request));
  }

  @Test
  void givenNullFirstName_whenCreateParticipant_thenError() {
    ParticipantCreateRequest request = participant("john@test.com", 25, ParticipantGender.MALE);
    request.setFirstName(null);

    assertThrows(IllegalArgumentEventRegException.class, () -> service.createParticipant(request));
  }

  @Test
  void givenNullLastName_whenCreateParticipant_thenError() {
    ParticipantCreateRequest request = participant("john@test.com", 25, ParticipantGender.MALE);
    request.setLastName(null);

    assertThrows(IllegalArgumentEventRegException.class, () -> service.createParticipant(request));
  }

  @Test
  void givenEmptyEmail_whenCreateParticipant_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> service.createParticipant(participant("", 25, ParticipantGender.MALE)));
  }

  @Test
  void givenNullEmail_whenCreateParticipant_thenError() {
    ParticipantCreateRequest request = participant("john@test.com", 25, ParticipantGender.MALE);
    request.setEmail(null);

    assertThrows(IllegalArgumentEventRegException.class, () -> service.createParticipant(request));
  }

  @Test
  void givenInvalidEmailFormat_whenCreateParticipant_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> service.createParticipant(participant("not-an-email", 25, ParticipantGender.MALE)));
  }

  @Test
  void givenZeroAge_whenCreateParticipant_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> service.createParticipant(participant("john@test.com", 0, ParticipantGender.MALE)));
  }

  @Test
  void givenAgeAboveMax_whenCreateParticipant_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> service.createParticipant(participant("john@test.com", 151, ParticipantGender.MALE)));
  }

  @Test
  void givenNullGender_whenCreateParticipant_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> service.createParticipant(participant("john@test.com", 25, null)));
  }

  // ---------- registerParticipant ----------

  @Test
  void givenValidData_whenRegisterParticipant_thenStatusIsAccepted() {
    arrangeEventAndParticipant();

    EventRegResponse response = register(1, 1);

    assertEquals(EventRegRequestStatus.ACCEPTED, response.getEventRegRequestStatus());
  }

  @Test
  void givenValidData_whenRegisterParticipant_thenRegistrationIdIsOne() {
    arrangeEventAndParticipant();

    EventRegResponse response = register(1, 1);

    assertEquals(1, response.getRegistrationId());
  }

  @Test
  void givenValidData_whenRegisterParticipant_thenParticipantAmountIncremented() {
    arrangeEventAndParticipant();

    register(1, 1);

    assertEquals(1, service.getEventById(1).getCurrentParticipantAmount());
  }

  @Test
  void givenAgeEqualToRequirement_whenRegisterParticipant_thenAccepted() {
    service.createEvent(event("Teens Event", 18, 100));
    service.createParticipant(participant("teen@test.com", 18, ParticipantGender.MALE));

    EventRegResponse response = register(1, 1);

    assertEquals(EventRegRequestStatus.ACCEPTED, response.getEventRegRequestStatus());
  }

  @Test
  void givenAgeJustBelowRequirement_whenRegisterParticipant_thenDenied() {
    service.createEvent(event("Teens Event", 18, 100));
    service.createParticipant(participant("teen@test.com", 17, ParticipantGender.MALE));

    EventRegResponse response = register(1, 1);

    assertEquals(EventRegRequestStatus.DENIED, response.getEventRegRequestStatus());
  }

  @Test
  void givenAgeJustAboveRequirement_whenRegisterParticipant_thenAccepted() {
    service.createEvent(event("Teens Event", 18, 100));
    service.createParticipant(participant("adult@test.com", 19, ParticipantGender.MALE));

    EventRegResponse response = register(1, 1);

    assertEquals(EventRegRequestStatus.ACCEPTED, response.getEventRegRequestStatus());
  }

  @Test
  void givenMaleInFemaleOnlyEvent_whenRegisterParticipant_thenDenied() {
    service.createEvent(
        event(
            "Ladies Event",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.FEMALE_ONLY));
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    EventRegResponse response = register(1, 1);

    assertEquals(EventRegRequestStatus.DENIED, response.getEventRegRequestStatus());
  }

  @Test
  void givenFemaleInMaleOnlyEvent_whenRegisterParticipant_thenDenied() {
    service.createEvent(
        event(
            "Gentlemen Event",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.MALE_ONLY));
    service.createParticipant(participant("jane@test.com", 25, ParticipantGender.FEMALE));

    EventRegResponse response = register(1, 1);

    assertEquals(EventRegRequestStatus.DENIED, response.getEventRegRequestStatus());
  }

  @Test
  void givenNotSpecifiedGenderInGenderRestrictedEvent_whenRegisterParticipant_thenDenied() {
    service.createEvent(
        event(
            "Ladies Event",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.FEMALE_ONLY));
    service.createParticipant(
        participant("anonymous@test.com", 25, ParticipantGender.NOT_SPECIFIED));

    EventRegResponse response = register(1, 1);

    assertEquals(EventRegRequestStatus.DENIED, response.getEventRegRequestStatus());
  }

  @Test
  void givenFullEvent_whenRegisterParticipant_thenDenied() {
    service.createEvent(event("Full Event", 18, 1));
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    service.createParticipant(participant("jane@test.com", 25, ParticipantGender.FEMALE));
    register(1, 1);

    EventRegResponse response = register(2, 1);

    assertEquals(EventRegRequestStatus.DENIED, response.getEventRegRequestStatus());
  }

  @Test
  void givenFullEvent_whenThirdParticipantRegisters_thenDenied() {
    service.createEvent(event("Full Event", 18, 1));
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    service.createParticipant(participant("jane@test.com", 25, ParticipantGender.FEMALE));
    service.createParticipant(participant("alice@test.com", 25, ParticipantGender.FEMALE));
    register(1, 1);
    register(2, 1);

    EventRegResponse response = register(3, 1);

    assertEquals(EventRegRequestStatus.DENIED, response.getEventRegRequestStatus());
  }

  @Test
  void givenMissingParticipant_whenRegisterParticipant_thenError() {
    arrangeEvent();

    assertThrows(ParticipantNotFoundException.class, () -> register(1, 1));
  }

  @Test
  void givenMissingEvent_whenRegisterParticipant_thenError() {
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertThrows(EventNotFoundException.class, () -> register(1, 1));
  }

  // ---------- getParticipantById ----------

  @Test
  void givenExistingId_whenGetParticipantById_thenFirstNameIsSaved() {
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    ParticipantResponse response = service.getParticipantById(1);

    assertEquals("John", response.getFirstName());
  }

  @Test
  void givenExistingId_whenGetParticipantById_thenEmailIsSaved() {
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    ParticipantResponse response = service.getParticipantById(1);

    assertEquals("john@test.com", response.getEmail());
  }

  @Test
  void givenExistingId_whenGetParticipantById_thenAgeIsSaved() {
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    ParticipantResponse response = service.getParticipantById(1);

    assertEquals(25, response.getAge());
  }

  @Test
  void givenMissingIdZero_whenGetParticipantById_thenError() {
    assertThrows(ParticipantNotFoundException.class, () -> service.getParticipantById(0));
  }

  @Test
  void givenNegativeId_whenGetParticipantById_thenError() {
    assertThrows(ParticipantNotFoundException.class, () -> service.getParticipantById(-1));
  }

  @Test
  void givenHugeId_whenGetParticipantById_thenError() {
    assertThrows(ParticipantNotFoundException.class, () -> service.getParticipantById(999999));
  }

  // ---------- getParticipants ----------

  @Test
  void givenTwoParticipants_whenGetParticipants_thenSizeIsTwo() {
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    service.createParticipant(participant("jane@test.com", 30, ParticipantGender.FEMALE));

    List<ParticipantResponse> participants = service.getParticipants();

    assertEquals(2, participants.size());
  }

  @Test
  void givenTwoParticipants_whenGetParticipants_thenFirstIsJohn() {
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    service.createParticipant(participant("jane@test.com", 30, ParticipantGender.FEMALE));

    ParticipantResponse first = service.getParticipants().get(0);

    assertEquals("John", first.getFirstName());
  }

  @Test
  void givenNoParticipants_whenGetParticipants_thenEmpty() {
    List<ParticipantResponse> participants = service.getParticipants();

    assertTrue(participants.isEmpty());
  }

  // ---------- getParticipantsSorted ----------

  @Test
  void givenTwoParticipants_whenGetSortedByAge_thenFirstIsYounger() {
    service.createParticipant(participant("john@test.com", 30, ParticipantGender.MALE));
    service.createParticipant(participant("jane@test.com", 20, ParticipantGender.FEMALE));

    List<ParticipantResponse> sorted =
        service.getParticipantsSorted(Comparator.comparingInt(Participant::getAge));

    assertEquals(20, sorted.get(0).getAge());
  }

  @Test
  void givenTwoParticipants_whenGetSortedByAge_thenSecondIsOlder() {
    service.createParticipant(participant("john@test.com", 30, ParticipantGender.MALE));
    service.createParticipant(participant("jane@test.com", 20, ParticipantGender.FEMALE));

    List<ParticipantResponse> sorted =
        service.getParticipantsSorted(Comparator.comparingInt(Participant::getAge));

    assertEquals(30, sorted.get(1).getAge());
  }

  @Test
  void givenNoParticipants_whenGetSorted_thenEmpty() {
    List<ParticipantResponse> sorted =
        service.getParticipantsSorted(Comparator.comparingInt(Participant::getAge));

    assertTrue(sorted.isEmpty());
  }

  // ---------- getEventById ----------

  @Test
  void givenExistingId_whenGetEventById_thenNameIsSaved() {
    arrangeEvent();

    EventResponse response = service.getEventById(1);

    assertEquals("Tech Conference", response.getEventName());
  }

  @Test
  void givenExistingId_whenGetEventById_thenDateIsSaved() {
    OffsetDateTime date = OffsetDateTime.parse("2030-05-05T10:00:00+00:00");
    service.createEvent(
        event("Tech Conference", date, Duration.ofHours(2), 18, 100, EventGenderRequirement.NONE));

    EventResponse response = service.getEventById(1);

    assertEquals(date, response.getEventDate());
  }

  @Test
  void givenExistingId_whenGetEventById_thenLocationIsSaved() {
    arrangeEvent();

    EventResponse response = service.getEventById(1);

    assertEquals("Main Hall", response.getLocation());
  }

  @Test
  void givenMissingIdZero_whenGetEventById_thenError() {
    assertThrows(EventNotFoundException.class, () -> service.getEventById(0));
  }

  @Test
  void givenNegativeId_whenGetEventById_thenError() {
    assertThrows(EventNotFoundException.class, () -> service.getEventById(-1));
  }

  @Test
  void givenHugeId_whenGetEventById_thenError() {
    assertThrows(EventNotFoundException.class, () -> service.getEventById(999999));
  }

  // ---------- getEvents ----------

  @Test
  void givenTwoEvents_whenGetEvents_thenSizeIsTwo() {
    service.createEvent(event("Event One", 18, 100));
    service.createEvent(event("Event Two", 18, 100));

    List<EventResponse> events = service.getEvents();

    assertEquals(2, events.size());
  }

  @Test
  void givenTwoEvents_whenGetEvents_thenFirstIsFirstCreated() {
    service.createEvent(event("Event One", 18, 100));
    service.createEvent(event("Event Two", 18, 100));

    EventResponse first = service.getEvents().get(0);

    assertEquals("Event One", first.getEventName());
  }

  @Test
  void givenNoEvents_whenGetEvents_thenEmpty() {
    List<EventResponse> events = service.getEvents();

    assertTrue(events.isEmpty());
  }

  // ---------- getEventsFiltered ----------

  @Test
  void givenPredicate_whenGetEventsFiltered_thenOnlyMatchingReturned() {
    service.createEvent(event("Teens Event", 18, 100));
    service.createEvent(event("Adults Event", 21, 100));

    List<EventResponse> filtered =
        service.getEventsFiltered(List.of(currentEvent -> currentEvent.getAgeRequired() >= 21));

    assertEquals(1, filtered.size());
  }

  @Test
  void givenPredicate_whenGetEventsFiltered_thenMatchingNameReturned() {
    service.createEvent(event("Teens Event", 18, 100));
    service.createEvent(event("Adults Event", 21, 100));

    List<EventResponse> filtered =
        service.getEventsFiltered(List.of(currentEvent -> currentEvent.getAgeRequired() >= 21));

    assertEquals("Adults Event", filtered.get(0).getEventName());
  }

  @Test
  void givenNoMatchingEvents_whenGetEventsFiltered_thenEmpty() {
    service.createEvent(event("Teens Event", 18, 100));

    List<EventResponse> filtered =
        service.getEventsFiltered(List.of(currentEvent -> currentEvent.getAgeRequired() >= 21));

    assertTrue(filtered.isEmpty());
  }

  @Test
  void givenEmptyPredicates_whenGetEventsFiltered_thenAllReturned() {
    service.createEvent(event("Teens Event", 18, 100));
    service.createEvent(event("Adults Event", 21, 100));

    List<EventResponse> filtered = service.getEventsFiltered(List.of());

    assertEquals(2, filtered.size());
  }

  // ---------- getEventsGrouped ----------

  @Test
  void givenTwoLocations_whenGetEventsGrouped_thenTwoGroups() {
    EventCreateRequest second =
        event(
            "Second Event",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.NONE);
    second.setLocation("West Hall");
    service.createEvent(event("First Event", 18, 100));
    service.createEvent(second);

    Map<String, List<EventResponse>> grouped = service.getEventsGrouped(Event::getLocation);

    assertEquals(2, grouped.size());
  }

  @Test
  void givenTwoLocations_whenGetEventsGrouped_thenGroupContainsEvent() {
    EventCreateRequest second =
        event(
            "Second Event",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.NONE);
    second.setLocation("West Hall");
    service.createEvent(event("First Event", 18, 100));
    service.createEvent(second);

    Map<String, List<EventResponse>> grouped = service.getEventsGrouped(Event::getLocation);

    assertEquals(1, grouped.get("Main Hall").size());
  }

  // ---------- getRegistrationRequestById ----------

  @Test
  void givenExistingId_whenGetRegistrationRequestById_thenStatusIsAccepted() {
    arrangeAcceptedRegistration();

    EventRegResponse response = service.getRegistrationRequestById(1);

    assertEquals(EventRegRequestStatus.ACCEPTED, response.getEventRegRequestStatus());
  }

  @Test
  void givenExistingId_whenGetRegistrationRequestById_thenParticipantIdIsOne() {
    arrangeAcceptedRegistration();

    EventRegResponse response = service.getRegistrationRequestById(1);

    assertEquals(1, response.getParticipantId());
  }

  @Test
  void givenExistingId_whenGetRegistrationRequestById_thenEventIdIsOne() {
    arrangeAcceptedRegistration();

    EventRegResponse response = service.getRegistrationRequestById(1);

    assertEquals(1, response.getEventId());
  }

  @Test
  void givenMissingIdZero_whenGetRegistrationRequestById_thenError() {
    assertThrows(RegistrationNotFoundException.class, () -> service.getRegistrationRequestById(0));
  }

  @Test
  void givenHugeId_whenGetRegistrationRequestById_thenError() {
    assertThrows(
        RegistrationNotFoundException.class, () -> service.getRegistrationRequestById(999999));
  }

  @Test
  void givenRegistrationWithoutParticipant_whenGetRegistrationRequestById_thenError() {
    arrangeEvent();
    registerMissingParticipant();

    assertThrows(ParticipantNotFoundException.class, () -> service.getRegistrationRequestById(1));
  }

  @Test
  void givenRegistrationForMissingEvent_whenGetRegistrationRequestById_thenError() {
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    registerMissingEvent();

    assertThrows(EventNotFoundException.class, () -> service.getRegistrationRequestById(1));
  }

  // ---------- getRegistrationRequests ----------

  @Test
  void givenTwoRegistrations_whenGetRegistrationRequests_thenSizeIsTwo() {
    service.createEvent(event("Tech Conference", 18, 100));
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    service.createParticipant(participant("jane@test.com", 25, ParticipantGender.FEMALE));
    register(1, 1);
    register(2, 1);

    List<EventRegResponse> registrations = service.getRegistrationRequests();

    assertEquals(2, registrations.size());
  }

  // ---------- getRegistrationRequestsInWaitingQueue ----------

  @Test
  void givenWaitingRegistration_whenGetWaitingQueue_thenSizeIsOne() {
    arrangeAcceptedRegistration();
    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.WAITING, "waiting", false);

    List<EventRegResponse> waiting = service.getRegistrationRequestsInWaitingQueue(1);

    assertEquals(1, waiting.size());
  }

  @Test
  void givenWaitingRegistration_whenGetWaitingQueue_thenRegistrationIdIsOne() {
    arrangeAcceptedRegistration();
    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.WAITING, "waiting", false);

    List<EventRegResponse> waiting = service.getRegistrationRequestsInWaitingQueue(1);

    assertEquals(1, waiting.get(0).getRegistrationId());
  }

  @Test
  void givenNoWaitingRegistrations_whenGetWaitingQueue_thenEmpty() {
    List<EventRegResponse> waiting = service.getRegistrationRequestsInWaitingQueue(1);

    assertTrue(waiting.isEmpty());
  }

  @Test
  void givenWaitingQueue_whenCancelAcceptedRegistration_thenWaitingAutoAccepted() {
    arrangeWaitingQueueScenario();

    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    assertEquals(
        EventRegRequestStatus.ACCEPTED,
        service.getRegistrationRequestById(2).getEventRegRequestStatus());
  }

  @Test
  void givenWaitingQueue_whenCancelAcceptedRegistration_thenParticipantAmountIsOne() {
    arrangeWaitingQueueScenario();

    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    assertEquals(1, service.getEventById(1).getCurrentParticipantAmount());
  }

  @Test
  void givenWaitingQueue_whenUndoCancel_thenCancelledRegistrationRestored() {
    arrangeWaitingQueueScenario();
    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    service.undoLatestAction();

    assertEquals(
        EventRegRequestStatus.ACCEPTED,
        service.getRegistrationRequestById(1).getEventRegRequestStatus());
  }

  @Test
  void givenWaitingQueue_whenUndoCancel_thenWaitingRegistrationBackToWaiting() {
    arrangeWaitingQueueScenario();
    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    service.undoLatestAction();

    assertEquals(
        EventRegRequestStatus.WAITING,
        service.getRegistrationRequestById(2).getEventRegRequestStatus());
  }

  // ---------- changeRegistrationRequestStatus ----------

  @Test
  void givenAcceptedRegistration_whenChangeToWaiting_thenStatusIsWaiting() {
    arrangeAcceptedRegistration();

    EventRegResponse response =
        service.changeRegistrationRequestStatus(1, EventRegRequestStatus.WAITING, "waiting", false);

    assertEquals(EventRegRequestStatus.WAITING, response.getEventRegRequestStatus());
  }

  @Test
  void givenAcceptedRegistration_whenChangeToWaiting_thenParticipantAmountDecremented() {
    arrangeAcceptedRegistration();

    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.WAITING, "waiting", false);

    assertEquals(0, service.getEventById(1).getCurrentParticipantAmount());
  }

  @Test
  void givenWaitingRegistration_whenChangeToAccepted_thenStatusIsAccepted() {
    arrangeAcceptedRegistration();
    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.WAITING, "waiting", false);

    EventRegResponse response =
        service.changeRegistrationRequestStatus(1, EventRegRequestStatus.ACCEPTED, "back", false);

    assertEquals(EventRegRequestStatus.ACCEPTED, response.getEventRegRequestStatus());
  }

  @Test
  void givenWaitingRegistration_whenChangeToAccepted_thenParticipantAmountIncremented() {
    arrangeAcceptedRegistration();
    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.WAITING, "waiting", false);

    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.ACCEPTED, "back", false);

    assertEquals(1, service.getEventById(1).getCurrentParticipantAmount());
  }

  @Test
  void givenAcceptedRegistration_whenChangeToDenied_thenStatusIsDenied() {
    arrangeAcceptedRegistration();

    EventRegResponse response =
        service.changeRegistrationRequestStatus(1, EventRegRequestStatus.DENIED, "no", false);

    assertEquals(EventRegRequestStatus.DENIED, response.getEventRegRequestStatus());
  }

  @Test
  void givenAcceptedRegistration_whenChangeToCancelled_thenStatusIsCancelled() {
    arrangeAcceptedRegistration();

    EventRegResponse response =
        service.changeRegistrationRequestStatus(
            1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    assertEquals(EventRegRequestStatus.CANCELLED, response.getEventRegRequestStatus());
  }

  @Test
  void givenAcceptedRegistration_whenChangeToCancelled_thenParticipantAmountDecremented() {
    arrangeAcceptedRegistration();

    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    assertEquals(0, service.getEventById(1).getCurrentParticipantAmount());
  }

  @Test
  void givenCancelWithoutHistory_whenUndo_thenRegisterIsUndone() {
    arrangeAcceptedRegistration();

    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.CANCELLED, "cancelled", false);
    service.undoLatestAction();

    assertThrows(RegistrationNotFoundException.class, () -> service.getRegistrationRequestById(1));
  }

  @Test
  void givenMissingRegistration_whenChangeStatus_thenError() {
    assertThrows(
        RegistrationNotFoundException.class,
        () ->
            service.changeRegistrationRequestStatus(
                999, EventRegRequestStatus.ACCEPTED, "x", false));
  }

  @Test
  void givenSameStatus_whenChangeStatus_thenError() {
    arrangeAcceptedRegistration();

    assertThrows(
        EventRegException.class,
        () ->
            service.changeRegistrationRequestStatus(
                1, EventRegRequestStatus.ACCEPTED, "same", false));
  }

  @Test
  void givenFullEvent_whenAcceptOverCapacity_thenError() {
    service.createEvent(event("Full Event", 18, 1));
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    service.createParticipant(participant("jane@test.com", 25, ParticipantGender.FEMALE));
    register(1, 1);
    register(2, 1);

    assertThrows(
        EventCapacityExceededException.class,
        () ->
            service.changeRegistrationRequestStatus(
                2, EventRegRequestStatus.ACCEPTED, "over capacity", false));
  }

  // ---------- undoLatestAction ----------

  @Test
  void givenEmptyHistory_whenUndo_thenError() {
    assertThrows(EventRegException.class, () -> service.undoLatestAction());
  }

  @Test
  void givenCreatedEvent_whenUndo_thenEventRemoved() {
    arrangeEvent();

    service.undoLatestAction();

    assertThrows(EventNotFoundException.class, () -> service.getEventById(1));
  }

  @Test
  void givenCreatedEvent_whenUndo_thenTypeIsCreateEvent() {
    arrangeEvent();

    UndoResponse response = service.undoLatestAction();

    assertEquals(ActionType.CREATE_EVENT, response.getType());
  }

  @Test
  void givenCreatedParticipant_whenUndo_thenParticipantRemoved() {
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    service.undoLatestAction();

    assertThrows(ParticipantNotFoundException.class, () -> service.getParticipantById(1));
  }

  @Test
  void givenCreatedParticipant_whenUndo_thenTypeIsCreateParticipant() {
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    UndoResponse response = service.undoLatestAction();

    assertEquals(ActionType.CREATE_PARTICIPANT, response.getType());
  }

  @Test
  void givenRegistration_whenUndo_thenRegistrationRemoved() {
    arrangeAcceptedRegistration();

    service.undoLatestAction();

    assertThrows(RegistrationNotFoundException.class, () -> service.getRegistrationRequestById(1));
  }

  @Test
  void givenWaitingRegistration_whenUndoRegister_thenRegistrationRemoved() {
    service.createEvent(event("Tech Conference", 18, 100));
    service.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    register(1, 1);
    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.WAITING, "waiting", false);

    service.undoLatestAction();

    assertThrows(RegistrationNotFoundException.class, () -> service.getRegistrationRequestById(1));
  }

  @Test
  void givenRegistration_whenUndo_thenTypeIsRegisterParticipant() {
    arrangeAcceptedRegistration();

    UndoResponse response = service.undoLatestAction();

    assertEquals(ActionType.REGISTER_PARTICIPANT, response.getType());
  }

  @Test
  void givenCancelledRegistration_whenUndo_thenStatusRestored() {
    arrangeAcceptedRegistration();
    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    service.undoLatestAction();

    assertEquals(
        EventRegRequestStatus.ACCEPTED,
        service.getRegistrationRequestById(1).getEventRegRequestStatus());
  }

  @Test
  void givenCancelledRegistration_whenUndo_thenParticipantAmountRestored() {
    arrangeAcceptedRegistration();
    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.CANCELLED, "cancelled", true);
    service.undoLatestAction();

    assertEquals(1, service.getEventById(1).getCurrentParticipantAmount());
  }

  @Test
  void givenCancelledRegistration_whenUndo_thenTypeIsCancelRegistration() {
    arrangeAcceptedRegistration();
    service.changeRegistrationRequestStatus(1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    UndoResponse response = service.undoLatestAction();

    assertEquals(ActionType.CANCEL_REGISTRATION, response.getType());
  }
}
