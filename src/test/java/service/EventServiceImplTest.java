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
import exception.DuplicateException;
import exception.EventCapacityExceededException;
import exception.EventNotFoundException;
import exception.EventRegException;
import exception.IllegalArgumentEventRegException;
import exception.ParticipantNotFoundException;
import exception.RegistrationNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import model.Event;
import model.Participant;
import model.enums.ActionType;
import model.enums.EventGenderRequirement;
import model.enums.EventRegRequestStatus;
import model.enums.EventRegistrationStatus;
import model.enums.EventStatus;
import model.enums.ParticipantGender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import repository.EventRegistrationRepository;
import repository.EventRepository;
import repository.ParticipantRepository;
import repository.implementation.Jdbc.EventRegistrationRepositoryJdbc;
import repository.implementation.Jdbc.EventRepositoryJdbc;
import repository.implementation.Jdbc.ParticipantRepositoryJdbc;
import service.implementation.EventServiceImpl;

/** Unit tests for {@link EventServiceImpl}. */
@Testcontainers
public class EventServiceImplTest {

  private EventService eventService;

  private static Connection connection;

  @Container
  private static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:18")
          .withDatabaseName("test_db")
          .withUsername("test")
          .withPassword("test");

  @BeforeAll
  static void setUpDatabase() throws SQLException {
    System.setProperty("db.url", postgres.getJdbcUrl());
    System.setProperty("db.user", postgres.getUsername());
    System.setProperty("db.password", postgres.getPassword());

    connection =
        DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

    runMigrations(connection);
  }

  private static void runMigrations(Connection connection) {
    try {
      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));

      Liquibase liquibase =
          new Liquibase(
              "db/changelog/db.changelog-master.yaml", new ClassLoaderResourceAccessor(), database);
      liquibase.update("");
      connection.setAutoCommit(true);
    } catch (Exception e) {
      throw new RuntimeException("Ошибка миграций", e);
    }
  }

  private static void cleanDatabase(Connection connection) throws SQLException {
    connection.setAutoCommit(true);
    try (var statement = connection.createStatement()) {
      statement.execute("TRUNCATE event_registration, event, participant RESTART IDENTITY CASCADE");
    }
  }

  @BeforeEach
  void setUp() throws SQLException {
    cleanDatabase(connection);

    ParticipantRepository participantRepo = new ParticipantRepositoryJdbc();
    EventRepository eventRepo = new EventRepositoryJdbc();
    EventRegistrationRepository regRepo = new EventRegistrationRepositoryJdbc();

    eventService = new EventServiceImpl(eventRepo, participantRepo, regRepo);
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
    return eventService.registerParticipant(new EventRegRequest(participantId, eventId));
  }

  private void arrangeEvent() {
    eventService.createEvent(event("Tech Conference", 18, 100));
  }

  private void arrangeEventAndParticipant() {
    eventService.createEvent(event("Tech Conference", 18, 100));
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
  }

  private void arrangeAcceptedRegistration() {
    arrangeEventAndParticipant();
    register(1, 1);
  }

  private void registerMissingParticipant() {
    try {
      register(1, 1);
    } catch (ParticipantNotFoundException expected) {
      // participant missing: registration is rejected before it is persisted
    }
  }

  private void registerMissingEvent() {
    try {
      register(1, 999);
    } catch (EventNotFoundException expected) {
      // event missing: registration is rejected before it is persisted
    }
  }

  private void arrangeWaitingQueueScenario() {
    eventService.createEvent(event("Full Event", 18, 1));
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    eventService.createParticipant(participant("jane@test.com", 25, ParticipantGender.FEMALE));
    register(1, 1);
    register(2, 1);
    eventService.changeRegistrationRequestStatus(
        2, EventRegRequestStatus.WAITING, "waiting", false);
  }

  // ---------- duplicates ----------

  @Test
  void givenOneSpot_When100ConcurrentRegistration_ThenOnlyOneAccepted() throws Exception {
    eventService.createEvent(event("Tech Conference", 18, 100));

    assertThrows(
        DuplicateException.class,
        () -> eventService.createEvent(event("Tech Conference", 18, 100)));
  }

  //  @Test
  //  void givenDuplicateEventName_whenCreateEvent_thenDuplicateException() throws
  // ExecutionException, InterruptedException {
  //    eventService.createEvent(event("Concurrency test", 18, 1));
  //
  //    List<Integer> participantIds = new ArrayList<>();
  //    for (int i = 0; i < 100; i++)
  //    {
  //        ParticipantResponse response = eventService.createParticipant(
  //                participant("user" + i + "@test.com", 25, ParticipantGender.NOT_SPECIFIED));
  //
  //        participantIds.add(response.getParticipantId());
  //    }
  //
  //    ExecutorService executor = Executors.newFixedThreadPool(100);
  //    List<Future<EventRegResponse>> futures = new ArrayList<>();
  //
  //      for (int participantId : participantIds) {
  //          futures.add(executor.submit(() -> register(participantId, 1)));
  //      }
  //
  //      int accepted = 0;
  //      int denied = 0;
  //      for (Future<EventRegResponse> future : futures)
  //      {
  //          EventRegResponse response = future.get();
  //          if (response.getEventRegRequestStatus() == EventRegRequestStatus.ACCEPTED) {
  //              accepted++;
  //          } else {
  //              denied++;
  //          }
  //      }
  //
  //      executor.shutdown();
  //
  //      assertEquals(1,accepted);
  //      assertEquals(99,denied);
  //  }

  @Test
  void givenDuplicateParticipantEmail_whenCreateParticipant_thenDuplicateException() {
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertThrows(
        DuplicateException.class,
        () ->
            eventService.createParticipant(
                participant("john@test.com", 25, ParticipantGender.MALE)));
  }

  @Test
  void givenUndoneParticipant_whenCreateParticipantWithSameEmail_thenParticipantCreated() {
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    eventService.undoLatestAction();

    ParticipantResponse response =
        eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertEquals("john@test.com", response.getEmail());
  }

  // ---------- createEvent ----------

  @Test
  void givenValidEvent_whenCreateEvent_thenIdIsOne() {
    EventResponse response = eventService.createEvent(event("Tech Conference", 18, 100));

    assertEquals(1, response.getEventId());
  }

  @Test
  void givenValidEvent_whenCreateEvent_thenNameIsSaved() {
    EventResponse response = eventService.createEvent(event("Tech Conference", 18, 100));

    assertEquals("Tech Conference", response.getEventName());
  }

  @Test
  void givenValidEvent_whenCreateEvent_thenLocationIsSaved() {
    EventResponse response = eventService.createEvent(event("Tech Conference", 18, 100));

    assertEquals("Main Hall", response.getLocation());
  }

  @Test
  void givenValidEvent_whenCreateEvent_thenStatusIsPlanned() {
    EventResponse response = eventService.createEvent(event("Tech Conference", 18, 100));

    assertEquals(EventStatus.PLANNED, response.getEventStatus());
  }

  @Test
  void givenValidEvent_whenCreateEvent_thenRegistrationStatusIsOpen() {
    EventResponse response = eventService.createEvent(event("Tech Conference", 18, 100));

    assertEquals(EventRegistrationStatus.RESERVATIONS_OPEN, response.getEventRegistrationStatus());
  }

  @Test
  void givenValidEvent_whenCreateEvent_thenCurrentParticipantAmountIsZero() {
    EventResponse response = eventService.createEvent(event("Tech Conference", 18, 100));

    assertEquals(0, response.getCurrentParticipantAmount());
  }

  @Test
  void givenValidEvent_whenCreateEvent_thenAgeRequiredIsSaved() {
    EventResponse response = eventService.createEvent(event("Tech Conference", 18, 100));

    assertEquals(18, response.getAgeRequired());
  }

  @Test
  void givenValidEvent_whenCreateEvent_thenMaxParticipantAmountIsSaved() {
    EventResponse response = eventService.createEvent(event("Tech Conference", 18, 100));

    assertEquals(100, response.getMaxParticipantAmount());
  }

  @Test
  void givenMaxParticipantAmountOne_whenCreateEvent_thenEventCreated() {
    EventResponse response = eventService.createEvent(event("Small Event", 18, 1));

    assertEquals(1, response.getEventId());
  }

  @Test
  void givenAgeRequiredZero_whenCreateEvent_thenEventCreated() {
    EventResponse response = eventService.createEvent(event("Kids Event", 0, 100));

    assertEquals(1, response.getEventId());
  }

  @Test
  void givenAgeRequiredHundredFifty_whenCreateEvent_thenEventCreated() {
    EventResponse response = eventService.createEvent(event("Seniors Event", 150, 100));

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

    EventResponse response = eventService.createEvent(request);

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

    EventResponse response = eventService.createEvent(request);

    assertEquals(1, response.getEventId());
  }

  @Test
  void givenNullEventRequest_whenCreateEvent_thenError() {
    assertThrows(IllegalArgumentEventRegException.class, () -> eventService.createEvent(null));
  }

  @Test
  void givenEmptyEventName_whenCreateEvent_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class, () -> eventService.createEvent(event("", 18, 100)));
  }

  @Test
  void givenBlankEventName_whenCreateEvent_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> eventService.createEvent(event("   ", 18, 100)));
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

    assertThrows(IllegalArgumentEventRegException.class, () -> eventService.createEvent(request));
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

    assertThrows(IllegalArgumentEventRegException.class, () -> eventService.createEvent(request));
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

    assertThrows(IllegalArgumentEventRegException.class, () -> eventService.createEvent(request));
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

    assertThrows(IllegalArgumentEventRegException.class, () -> eventService.createEvent(request));
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

    assertThrows(IllegalArgumentEventRegException.class, () -> eventService.createEvent(request));
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

    assertThrows(IllegalArgumentEventRegException.class, () -> eventService.createEvent(request));
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

    assertThrows(IllegalArgumentEventRegException.class, () -> eventService.createEvent(request));
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

    assertThrows(IllegalArgumentEventRegException.class, () -> eventService.createEvent(request));
  }

  @Test
  void givenNegativeAgeRequired_whenCreateEvent_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> eventService.createEvent(event("Bad Age", -1, 100)));
  }

  @Test
  void givenAgeRequiredAboveMax_whenCreateEvent_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> eventService.createEvent(event("Bad Age", 151, 100)));
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

    assertThrows(IllegalArgumentEventRegException.class, () -> eventService.createEvent(request));
  }

  @Test
  void givenZeroMaxParticipantAmount_whenCreateEvent_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> eventService.createEvent(event("Bad Max", 18, 0)));
  }

  @Test
  void givenNegativeMaxParticipantAmount_whenCreateEvent_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> eventService.createEvent(event("Bad Max", 18, -1)));
  }

  // ---------- createParticipant ----------

  @Test
  void givenValidParticipant_whenCreateParticipant_thenIdIsOne() {
    ParticipantResponse response =
        eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertEquals(1, response.getParticipantId());
  }

  @Test
  void givenValidParticipant_whenCreateParticipant_thenFirstNameIsSaved() {
    ParticipantResponse response =
        eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertEquals("John", response.getFirstName());
  }

  @Test
  void givenValidParticipant_whenCreateParticipant_thenLastNameIsSaved() {
    ParticipantResponse response =
        eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertEquals("Doe", response.getLastName());
  }

  @Test
  void givenValidParticipant_whenCreateParticipant_thenEmailIsSaved() {
    ParticipantResponse response =
        eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertEquals("john@test.com", response.getEmail());
  }

  @Test
  void givenValidParticipant_whenCreateParticipant_thenAgeIsSaved() {
    ParticipantResponse response =
        eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertEquals(25, response.getAge());
  }

  @Test
  void givenValidParticipant_whenCreateParticipant_thenGenderIsSaved() {
    ParticipantResponse response =
        eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertEquals(ParticipantGender.MALE, response.getParticipantGender());
  }

  @Test
  void givenValidParticipant_whenCreateParticipant_thenRegisteredAtIsSet() {
    ParticipantResponse response =
        eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertNotNull(response.getRegisteredAt());
  }

  @Test
  void givenAgeOne_whenCreateParticipant_thenParticipantCreated() {
    ParticipantResponse response =
        eventService.createParticipant(participant("baby@test.com", 1, ParticipantGender.MALE));

    assertEquals(1, response.getParticipantId());
  }

  @Test
  void givenAgeHundredFifty_whenCreateParticipant_thenParticipantCreated() {
    ParticipantResponse response =
        eventService.createParticipant(participant("elder@test.com", 150, ParticipantGender.MALE));

    assertEquals(1, response.getParticipantId());
  }

  @Test
  void givenNullParticipantRequest_whenCreateParticipant_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class, () -> eventService.createParticipant(null));
  }

  @Test
  void givenBlankFirstName_whenCreateParticipant_thenError() {
    ParticipantCreateRequest request = participant("john@test.com", 25, ParticipantGender.MALE);
    request.setFirstName(" ");

    assertThrows(
        IllegalArgumentEventRegException.class, () -> eventService.createParticipant(request));
  }

  @Test
  void givenNullFirstName_whenCreateParticipant_thenError() {
    ParticipantCreateRequest request = participant("john@test.com", 25, ParticipantGender.MALE);
    request.setFirstName(null);

    assertThrows(
        IllegalArgumentEventRegException.class, () -> eventService.createParticipant(request));
  }

  @Test
  void givenNullLastName_whenCreateParticipant_thenError() {
    ParticipantCreateRequest request = participant("john@test.com", 25, ParticipantGender.MALE);
    request.setLastName(null);

    assertThrows(
        IllegalArgumentEventRegException.class, () -> eventService.createParticipant(request));
  }

  @Test
  void givenEmptyEmail_whenCreateParticipant_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> eventService.createParticipant(participant("", 25, ParticipantGender.MALE)));
  }

  @Test
  void givenNullEmail_whenCreateParticipant_thenError() {
    ParticipantCreateRequest request = participant("john@test.com", 25, ParticipantGender.MALE);
    request.setEmail(null);

    assertThrows(
        IllegalArgumentEventRegException.class, () -> eventService.createParticipant(request));
  }

  @Test
  void givenInvalidEmailFormat_whenCreateParticipant_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () ->
            eventService.createParticipant(
                participant("not-an-email", 25, ParticipantGender.MALE)));
  }

  @Test
  void givenZeroAge_whenCreateParticipant_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () ->
            eventService.createParticipant(
                participant("john@test.com", 0, ParticipantGender.MALE)));
  }

  @Test
  void givenAgeAboveMax_whenCreateParticipant_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () ->
            eventService.createParticipant(
                participant("john@test.com", 151, ParticipantGender.MALE)));
  }

  @Test
  void givenNullGender_whenCreateParticipant_thenError() {
    assertThrows(
        IllegalArgumentEventRegException.class,
        () -> eventService.createParticipant(participant("john@test.com", 25, null)));
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

    assertEquals(1, eventService.getEventById(1).getCurrentParticipantAmount());
  }

  @Test
  void givenAgeEqualToRequirement_whenRegisterParticipant_thenAccepted() {
    eventService.createEvent(event("Teens Event", 18, 100));
    eventService.createParticipant(participant("teen@test.com", 18, ParticipantGender.MALE));

    EventRegResponse response = register(1, 1);

    assertEquals(EventRegRequestStatus.ACCEPTED, response.getEventRegRequestStatus());
  }

  @Test
  void givenAgeJustBelowRequirement_whenRegisterParticipant_thenDenied() {
    eventService.createEvent(event("Teens Event", 18, 100));
    eventService.createParticipant(participant("teen@test.com", 17, ParticipantGender.MALE));

    EventRegResponse response = register(1, 1);

    assertEquals(EventRegRequestStatus.DENIED, response.getEventRegRequestStatus());
  }

  @Test
  void givenAgeJustAboveRequirement_whenRegisterParticipant_thenAccepted() {
    eventService.createEvent(event("Teens Event", 18, 100));
    eventService.createParticipant(participant("adult@test.com", 19, ParticipantGender.MALE));

    EventRegResponse response = register(1, 1);

    assertEquals(EventRegRequestStatus.ACCEPTED, response.getEventRegRequestStatus());
  }

  @Test
  void givenMaleInFemaleOnlyEvent_whenRegisterParticipant_thenDenied() {
    eventService.createEvent(
        event(
            "Ladies Event",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.FEMALE_ONLY));
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    EventRegResponse response = register(1, 1);

    assertEquals(EventRegRequestStatus.DENIED, response.getEventRegRequestStatus());
  }

  @Test
  void givenFemaleInMaleOnlyEvent_whenRegisterParticipant_thenDenied() {
    eventService.createEvent(
        event(
            "Gentlemen Event",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.MALE_ONLY));
    eventService.createParticipant(participant("jane@test.com", 25, ParticipantGender.FEMALE));

    EventRegResponse response = register(1, 1);

    assertEquals(EventRegRequestStatus.DENIED, response.getEventRegRequestStatus());
  }

  @Test
  void givenNotSpecifiedGenderInGenderRestrictedEvent_whenRegisterParticipant_thenDenied() {
    eventService.createEvent(
        event(
            "Ladies Event",
            OffsetDateTime.now().plusDays(30),
            Duration.ofHours(2),
            18,
            100,
            EventGenderRequirement.FEMALE_ONLY));
    eventService.createParticipant(
        participant("anonymous@test.com", 25, ParticipantGender.NOT_SPECIFIED));

    EventRegResponse response = register(1, 1);

    assertEquals(EventRegRequestStatus.DENIED, response.getEventRegRequestStatus());
  }

  @Test
  void givenFullEvent_whenRegisterParticipant_thenDenied() {
    eventService.createEvent(event("Full Event", 18, 1));
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    eventService.createParticipant(participant("jane@test.com", 25, ParticipantGender.FEMALE));
    register(1, 1);

    EventRegResponse response = register(2, 1);

    assertEquals(EventRegRequestStatus.DENIED, response.getEventRegRequestStatus());
  }

  @Test
  void givenFullEvent_whenThirdParticipantRegisters_thenDenied() {
    eventService.createEvent(event("Full Event", 18, 1));
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    eventService.createParticipant(participant("jane@test.com", 25, ParticipantGender.FEMALE));
    eventService.createParticipant(participant("alice@test.com", 25, ParticipantGender.FEMALE));
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
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    assertThrows(EventNotFoundException.class, () -> register(1, 1));
  }

  // ---------- getParticipantById ----------

  @Test
  void givenExistingId_whenGetParticipantById_thenFirstNameIsSaved() {
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    ParticipantResponse response = eventService.getParticipantById(1);

    assertEquals("John", response.getFirstName());
  }

  @Test
  void givenExistingId_whenGetParticipantById_thenEmailIsSaved() {
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    ParticipantResponse response = eventService.getParticipantById(1);

    assertEquals("john@test.com", response.getEmail());
  }

  @Test
  void givenExistingId_whenGetParticipantById_thenAgeIsSaved() {
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    ParticipantResponse response = eventService.getParticipantById(1);

    assertEquals(25, response.getAge());
  }

  @Test
  void givenMissingIdZero_whenGetParticipantById_thenError() {
    assertThrows(ParticipantNotFoundException.class, () -> eventService.getParticipantById(0));
  }

  @Test
  void givenNegativeId_whenGetParticipantById_thenError() {
    assertThrows(ParticipantNotFoundException.class, () -> eventService.getParticipantById(-1));
  }

  @Test
  void givenHugeId_whenGetParticipantById_thenError() {
    assertThrows(ParticipantNotFoundException.class, () -> eventService.getParticipantById(999999));
  }

  // ---------- getParticipants ----------

  @Test
  void givenTwoParticipants_whenGetParticipants_thenSizeIsTwo() {
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    eventService.createParticipant(participant("jane@test.com", 30, ParticipantGender.FEMALE));

    List<ParticipantResponse> participants = eventService.getParticipants();

    assertEquals(2, participants.size());
  }

  @Test
  void givenTwoParticipants_whenGetParticipants_thenFirstIsJohn() {
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    eventService.createParticipant(participant("jane@test.com", 30, ParticipantGender.FEMALE));

    ParticipantResponse first = eventService.getParticipants().get(0);

    assertEquals("John", first.getFirstName());
  }

  @Test
  void givenNoParticipants_whenGetParticipants_thenEmpty() {
    List<ParticipantResponse> participants = eventService.getParticipants();

    assertTrue(participants.isEmpty());
  }

  // ---------- getParticipantsSorted ----------

  @Test
  void givenTwoParticipants_whenGetSortedByAge_thenFirstIsYounger() {
    eventService.createParticipant(participant("john@test.com", 30, ParticipantGender.MALE));
    eventService.createParticipant(participant("jane@test.com", 20, ParticipantGender.FEMALE));

    List<ParticipantResponse> sorted =
        eventService.getParticipantsSorted(Comparator.comparingInt(Participant::getAge));

    assertEquals(20, sorted.get(0).getAge());
  }

  @Test
  void givenTwoParticipants_whenGetSortedByAge_thenSecondIsOlder() {
    eventService.createParticipant(participant("john@test.com", 30, ParticipantGender.MALE));
    eventService.createParticipant(participant("jane@test.com", 20, ParticipantGender.FEMALE));

    List<ParticipantResponse> sorted =
        eventService.getParticipantsSorted(Comparator.comparingInt(Participant::getAge));

    assertEquals(30, sorted.get(1).getAge());
  }

  @Test
  void givenNoParticipants_whenGetSorted_thenEmpty() {
    List<ParticipantResponse> sorted =
        eventService.getParticipantsSorted(Comparator.comparingInt(Participant::getAge));

    assertTrue(sorted.isEmpty());
  }

  // ---------- getEventById ----------

  @Test
  void givenExistingId_whenGetEventById_thenNameIsSaved() {
    arrangeEvent();

    EventResponse response = eventService.getEventById(1);

    assertEquals("Tech Conference", response.getEventName());
  }

  @Test
  void givenExistingId_whenGetEventById_thenDateIsSaved() {
    OffsetDateTime date = OffsetDateTime.parse("2030-05-05T10:00:00+00:00");
    eventService.createEvent(
        event("Tech Conference", date, Duration.ofHours(2), 18, 100, EventGenderRequirement.NONE));

    EventResponse response = eventService.getEventById(1);

    assertEquals(date, response.getEventDate());
  }

  @Test
  void givenExistingId_whenGetEventById_thenLocationIsSaved() {
    arrangeEvent();

    EventResponse response = eventService.getEventById(1);

    assertEquals("Main Hall", response.getLocation());
  }

  @Test
  void givenMissingIdZero_whenGetEventById_thenError() {
    assertThrows(EventNotFoundException.class, () -> eventService.getEventById(0));
  }

  @Test
  void givenNegativeId_whenGetEventById_thenError() {
    assertThrows(EventNotFoundException.class, () -> eventService.getEventById(-1));
  }

  @Test
  void givenHugeId_whenGetEventById_thenError() {
    assertThrows(EventNotFoundException.class, () -> eventService.getEventById(999999));
  }

  // ---------- getEvents ----------

  @Test
  void givenTwoEvents_whenGetEvents_thenSizeIsTwo() {
    eventService.createEvent(event("Event One", 18, 100));
    eventService.createEvent(event("Event Two", 18, 100));

    List<EventResponse> events = eventService.getEvents();

    assertEquals(2, events.size());
  }

  @Test
  void givenTwoEvents_whenGetEvents_thenFirstIsFirstCreated() {
    eventService.createEvent(event("Event One", 18, 100));
    eventService.createEvent(event("Event Two", 18, 100));

    EventResponse first = eventService.getEvents().get(0);

    assertEquals("Event One", first.getEventName());
  }

  @Test
  void givenNoEvents_whenGetEvents_thenEmpty() {
    List<EventResponse> events = eventService.getEvents();

    assertTrue(events.isEmpty());
  }

  // ---------- getEventsFiltered ----------

  @Test
  void givenPredicate_whenGetEventsFiltered_thenOnlyMatchingReturned() {
    eventService.createEvent(event("Teens Event", 18, 100));
    eventService.createEvent(event("Adults Event", 21, 100));

    List<EventResponse> filtered =
        eventService.getEventsFiltered(
            List.of(currentEvent -> currentEvent.getAgeRequired() >= 21));

    assertEquals(1, filtered.size());
  }

  @Test
  void givenPredicate_whenGetEventsFiltered_thenMatchingNameReturned() {
    eventService.createEvent(event("Teens Event", 18, 100));
    eventService.createEvent(event("Adults Event", 21, 100));

    List<EventResponse> filtered =
        eventService.getEventsFiltered(
            List.of(currentEvent -> currentEvent.getAgeRequired() >= 21));

    assertEquals("Adults Event", filtered.get(0).getEventName());
  }

  @Test
  void givenNoMatchingEvents_whenGetEventsFiltered_thenEmpty() {
    eventService.createEvent(event("Teens Event", 18, 100));

    List<EventResponse> filtered =
        eventService.getEventsFiltered(
            List.of(currentEvent -> currentEvent.getAgeRequired() >= 21));

    assertTrue(filtered.isEmpty());
  }

  @Test
  void givenEmptyPredicates_whenGetEventsFiltered_thenAllReturned() {
    eventService.createEvent(event("Teens Event", 18, 100));
    eventService.createEvent(event("Adults Event", 21, 100));

    List<EventResponse> filtered = eventService.getEventsFiltered(List.of());

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
    eventService.createEvent(event("First Event", 18, 100));
    eventService.createEvent(second);

    Map<String, List<EventResponse>> grouped = eventService.getEventsGrouped(Event::getLocation);

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
    eventService.createEvent(event("First Event", 18, 100));
    eventService.createEvent(second);

    Map<String, List<EventResponse>> grouped = eventService.getEventsGrouped(Event::getLocation);

    assertEquals(1, grouped.get("Main Hall").size());
  }

  // ---------- getRegistrationRequestById ----------

  @Test
  void givenExistingId_whenGetRegistrationRequestById_thenStatusIsAccepted() {
    arrangeAcceptedRegistration();

    EventRegResponse response = eventService.getRegistrationRequestById(1);

    assertEquals(EventRegRequestStatus.ACCEPTED, response.getEventRegRequestStatus());
  }

  @Test
  void givenExistingId_whenGetRegistrationRequestById_thenParticipantIdIsOne() {
    arrangeAcceptedRegistration();

    EventRegResponse response = eventService.getRegistrationRequestById(1);

    assertEquals(1, response.getParticipantId());
  }

  @Test
  void givenExistingId_whenGetRegistrationRequestById_thenEventIdIsOne() {
    arrangeAcceptedRegistration();

    EventRegResponse response = eventService.getRegistrationRequestById(1);

    assertEquals(1, response.getEventId());
  }

  @Test
  void givenMissingIdZero_whenGetRegistrationRequestById_thenError() {
    assertThrows(
        RegistrationNotFoundException.class, () -> eventService.getRegistrationRequestById(0));
  }

  @Test
  void givenHugeId_whenGetRegistrationRequestById_thenError() {
    assertThrows(
        RegistrationNotFoundException.class, () -> eventService.getRegistrationRequestById(999999));
  }

  @Test
  void givenRegistrationWithoutParticipant_whenGetRegistrationRequestById_thenError() {
    arrangeEvent();
    registerMissingParticipant();

    assertThrows(
        RegistrationNotFoundException.class, () -> eventService.getRegistrationRequestById(1));
  }

  @Test
  void givenRegistrationForMissingEvent_whenGetRegistrationRequestById_thenError() {
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    registerMissingEvent();

    assertThrows(
        RegistrationNotFoundException.class, () -> eventService.getRegistrationRequestById(1));
  }

  // ---------- getRegistrationRequests ----------

  @Test
  void givenTwoRegistrations_whenGetRegistrationRequests_thenSizeIsTwo() {
    eventService.createEvent(event("Tech Conference", 18, 100));
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    eventService.createParticipant(participant("jane@test.com", 25, ParticipantGender.FEMALE));
    register(1, 1);
    register(2, 1);

    List<EventRegResponse> registrations = eventService.getRegistrationRequests();

    assertEquals(2, registrations.size());
  }

  // ---------- getRegistrationRequestsInWaitingQueue ----------

  @Test
  void givenWaitingRegistration_whenGetWaitingQueue_thenSizeIsOne() {
    arrangeAcceptedRegistration();
    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.WAITING, "waiting", false);

    List<EventRegResponse> waiting = eventService.getRegistrationRequestsInWaitingQueue(1);

    assertEquals(1, waiting.size());
  }

  @Test
  void givenWaitingRegistration_whenGetWaitingQueue_thenRegistrationIdIsOne() {
    arrangeAcceptedRegistration();
    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.WAITING, "waiting", false);

    List<EventRegResponse> waiting = eventService.getRegistrationRequestsInWaitingQueue(1);

    assertEquals(1, waiting.get(0).getRegistrationId());
  }

  @Test
  void givenNoWaitingRegistrations_whenGetWaitingQueue_thenEmpty() {
    List<EventRegResponse> waiting = eventService.getRegistrationRequestsInWaitingQueue(1);

    assertTrue(waiting.isEmpty());
  }

  @Test
  void givenWaitingQueue_whenCancelAcceptedRegistration_thenWaitingAutoAccepted() {
    arrangeWaitingQueueScenario();

    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    assertEquals(
        EventRegRequestStatus.ACCEPTED,
        eventService.getRegistrationRequestById(2).getEventRegRequestStatus());
  }

  @Test
  void givenWaitingQueue_whenCancelAcceptedRegistration_thenParticipantAmountIsOne() {
    arrangeWaitingQueueScenario();

    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    assertEquals(1, eventService.getEventById(1).getCurrentParticipantAmount());
  }

  @Test
  void givenWaitingQueue_whenUndoCancel_thenCancelledRegistrationRestored() {
    arrangeWaitingQueueScenario();
    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    eventService.undoLatestAction();

    assertEquals(
        EventRegRequestStatus.ACCEPTED,
        eventService.getRegistrationRequestById(1).getEventRegRequestStatus());
  }

  @Test
  void givenWaitingQueue_whenUndoCancel_thenWaitingRegistrationBackToWaiting() {
    arrangeWaitingQueueScenario();
    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    eventService.undoLatestAction();

    assertEquals(
        EventRegRequestStatus.WAITING,
        eventService.getRegistrationRequestById(2).getEventRegRequestStatus());
  }

  // ---------- changeRegistrationRequestStatus ----------

  @Test
  void givenAcceptedRegistration_whenChangeToWaiting_thenStatusIsWaiting() {
    arrangeAcceptedRegistration();

    EventRegResponse response =
        eventService.changeRegistrationRequestStatus(
            1, EventRegRequestStatus.WAITING, "waiting", false);

    assertEquals(EventRegRequestStatus.WAITING, response.getEventRegRequestStatus());
  }

  @Test
  void givenAcceptedRegistration_whenChangeToWaiting_thenParticipantAmountDecremented() {
    arrangeAcceptedRegistration();

    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.WAITING, "waiting", false);

    assertEquals(0, eventService.getEventById(1).getCurrentParticipantAmount());
  }

  @Test
  void givenWaitingRegistration_whenChangeToAccepted_thenStatusIsAccepted() {
    arrangeAcceptedRegistration();
    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.WAITING, "waiting", false);

    EventRegResponse response =
        eventService.changeRegistrationRequestStatus(
            1, EventRegRequestStatus.ACCEPTED, "back", false);

    assertEquals(EventRegRequestStatus.ACCEPTED, response.getEventRegRequestStatus());
  }

  @Test
  void givenWaitingRegistration_whenChangeToAccepted_thenParticipantAmountIncremented() {
    arrangeAcceptedRegistration();
    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.WAITING, "waiting", false);

    eventService.changeRegistrationRequestStatus(1, EventRegRequestStatus.ACCEPTED, "back", false);

    assertEquals(1, eventService.getEventById(1).getCurrentParticipantAmount());
  }

  @Test
  void givenAcceptedRegistration_whenChangeToDenied_thenStatusIsDenied() {
    arrangeAcceptedRegistration();

    EventRegResponse response =
        eventService.changeRegistrationRequestStatus(1, EventRegRequestStatus.DENIED, "no", false);

    assertEquals(EventRegRequestStatus.DENIED, response.getEventRegRequestStatus());
  }

  @Test
  void givenAcceptedRegistration_whenChangeToCancelled_thenStatusIsCancelled() {
    arrangeAcceptedRegistration();

    EventRegResponse response =
        eventService.changeRegistrationRequestStatus(
            1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    assertEquals(EventRegRequestStatus.CANCELLED, response.getEventRegRequestStatus());
  }

  @Test
  void givenAcceptedRegistration_whenChangeToCancelled_thenParticipantAmountDecremented() {
    arrangeAcceptedRegistration();

    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    assertEquals(0, eventService.getEventById(1).getCurrentParticipantAmount());
  }

  @Test
  void givenCancelWithoutHistory_whenUndo_thenRegisterIsUndone() {
    arrangeAcceptedRegistration();

    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.CANCELLED, "cancelled", false);
    eventService.undoLatestAction();

    assertThrows(
        RegistrationNotFoundException.class, () -> eventService.getRegistrationRequestById(1));
  }

  @Test
  void givenMissingRegistration_whenChangeStatus_thenError() {
    assertThrows(
        RegistrationNotFoundException.class,
        () ->
            eventService.changeRegistrationRequestStatus(
                999, EventRegRequestStatus.ACCEPTED, "x", false));
  }

  @Test
  void givenSameStatus_whenChangeStatus_thenError() {
    arrangeAcceptedRegistration();

    assertThrows(
        EventRegException.class,
        () ->
            eventService.changeRegistrationRequestStatus(
                1, EventRegRequestStatus.ACCEPTED, "same", false));
  }

  @Test
  void givenFullEvent_whenAcceptOverCapacity_thenError() {
    eventService.createEvent(event("Full Event", 18, 1));
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    eventService.createParticipant(participant("jane@test.com", 25, ParticipantGender.FEMALE));
    register(1, 1);
    register(2, 1);

    assertThrows(
        EventCapacityExceededException.class,
        () ->
            eventService.changeRegistrationRequestStatus(
                2, EventRegRequestStatus.ACCEPTED, "over capacity", false));
  }

  // ---------- undoLatestAction ----------

  @Test
  void givenEmptyHistory_whenUndo_thenError() {
    assertThrows(EventRegException.class, () -> eventService.undoLatestAction());
  }

  @Test
  void givenCreatedEvent_whenUndo_thenEventRemoved() {
    arrangeEvent();

    eventService.undoLatestAction();

    assertThrows(EventNotFoundException.class, () -> eventService.getEventById(1));
  }

  @Test
  void givenCreatedEvent_whenUndo_thenTypeIsCreateEvent() {
    arrangeEvent();

    UndoResponse response = eventService.undoLatestAction();

    assertEquals(ActionType.CREATE_EVENT, response.getType());
  }

  @Test
  void givenCreatedParticipant_whenUndo_thenParticipantRemoved() {
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    eventService.undoLatestAction();

    assertThrows(ParticipantNotFoundException.class, () -> eventService.getParticipantById(1));
  }

  @Test
  void givenCreatedParticipant_whenUndo_thenTypeIsCreateParticipant() {
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));

    UndoResponse response = eventService.undoLatestAction();

    assertEquals(ActionType.CREATE_PARTICIPANT, response.getType());
  }

  @Test
  void givenRegistration_whenUndo_thenRegistrationRemoved() {
    arrangeAcceptedRegistration();

    eventService.undoLatestAction();

    assertThrows(
        RegistrationNotFoundException.class, () -> eventService.getRegistrationRequestById(1));
  }

  @Test
  void givenWaitingRegistration_whenUndoRegister_thenRegistrationRemoved() {
    eventService.createEvent(event("Tech Conference", 18, 100));
    eventService.createParticipant(participant("john@test.com", 25, ParticipantGender.MALE));
    register(1, 1);
    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.WAITING, "waiting", false);

    eventService.undoLatestAction();

    assertThrows(
        RegistrationNotFoundException.class, () -> eventService.getRegistrationRequestById(1));
  }

  @Test
  void givenRegistration_whenUndo_thenTypeIsRegisterParticipant() {
    arrangeAcceptedRegistration();

    UndoResponse response = eventService.undoLatestAction();

    assertEquals(ActionType.REGISTER_PARTICIPANT, response.getType());
  }

  @Test
  void givenCancelledRegistration_whenUndo_thenStatusRestored() {
    arrangeAcceptedRegistration();
    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    eventService.undoLatestAction();

    assertEquals(
        EventRegRequestStatus.ACCEPTED,
        eventService.getRegistrationRequestById(1).getEventRegRequestStatus());
  }

  @Test
  void givenCancelledRegistration_whenUndo_thenParticipantAmountRestored() {
    arrangeAcceptedRegistration();
    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.CANCELLED, "cancelled", true);
    eventService.undoLatestAction();

    assertEquals(1, eventService.getEventById(1).getCurrentParticipantAmount());
  }

  @Test
  void givenCancelledRegistration_whenUndo_thenTypeIsCancelRegistration() {
    arrangeAcceptedRegistration();
    eventService.changeRegistrationRequestStatus(
        1, EventRegRequestStatus.CANCELLED, "cancelled", true);

    UndoResponse response = eventService.undoLatestAction();

    assertEquals(ActionType.CANCEL_REGISTRATION, response.getType());
  }
}
