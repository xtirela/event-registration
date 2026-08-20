package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dto.EventResponse;
import dto.EventSummary;
import exception.EventNotFoundException;
import exception.NoEventsPresentException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import model.Event;
import model.EventRegistration;
import model.Participant;
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

/** Integration tests for the new read-only methods of {@link EventServiceImpl}. */
@Testcontainers
class EventServiceImplNewFunctionsTest {

  private EventRepository eventRepository;
  private ParticipantRepository participantRepository;
  private EventRegistrationRepository registrationRepository;
  private EventServiceImpl eventService;

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

  @BeforeEach
  void setUp() throws SQLException {
    cleanDatabase(connection);

    eventRepository = new EventRepositoryJdbc();
    participantRepository = new ParticipantRepositoryJdbc();
    registrationRepository = new EventRegistrationRepositoryJdbc();

    eventService =
        new EventServiceImpl(eventRepository, participantRepository, registrationRepository);
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

  private int seedEvent(
      String name, String location, int current, int max, EventRegistrationStatus status) {
    return eventRepository
        .save(
            Event.builder()
                .eventName(name)
                .location(location)
                .eventDate(OffsetDateTime.parse("2030-05-01T13:30:00+02:00"))
                .eventDuration(Duration.ofHours(3))
                .ageRequired(18)
                .currentParticipantAmount(current)
                .maxParticipantAmount(max)
                .eventStatus(EventStatus.PLANNED)
                .eventRegistrationStatus(status)
                .eventGenderRequirement(EventGenderRequirement.NONE)
                .createdAt(OffsetDateTime.parse("2026-01-01T10:00:00+00:00"))
                .build())
        .getId();
  }

  private int seedParticipant(String firstName, String lastName) {
    return participantRepository
        .save(
            Participant.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(firstName.toLowerCase() + "@test.com")
                .age(25)
                .participantGender(ParticipantGender.MALE)
                .registeredAt(OffsetDateTime.parse("2026-01-01T10:00:00+00:00"))
                .build())
        .getId();
  }

  private void seedRegistration(int participantId, int eventId, OffsetDateTime createdAt) {
    registrationRepository.save(
        EventRegistration.builder()
            .participantId(participantId)
            .eventId(eventId)
            .eventRegRequestStatus(EventRegRequestStatus.PENDING)
            .createdAt(createdAt)
            .build());
  }

  private void seedWaitingRegistration(int participantId, int eventId) {
    EventRegistration registration =
        EventRegistration.builder()
            .participantId(participantId)
            .eventId(eventId)
            .eventRegRequestStatus(EventRegRequestStatus.PENDING)
            .createdAt(OffsetDateTime.parse("2026-06-01T10:00:00+00:00"))
            .build();
    registrationRepository.save(registration);
    registrationRepository.addToWaitingQueue(registration);
  }

  // ---------- getEventSummary ----------

  @Test
  void givenExistingEvent_whenGetEventSummary_thenSummaryReturned() {
    int eventId = seedEvent("Party", "MainHall", 5, 20, EventRegistrationStatus.RESERVATIONS_OPEN);

    EventSummary summary = eventService.getEventSummary(eventId);

    assertEquals("Party", summary.getEventName());
  }

  @Test
  void givenFullEvent_whenGetEventSummary_thenFreeSpotsAreZero() {
    int eventId = seedEvent("Party", "MainHall", 20, 20, EventRegistrationStatus.ALL_RESERVED);

    EventSummary summary = eventService.getEventSummary(eventId);

    assertEquals(0, summary.getFreeSpotAmount());
  }

  @Test
  void givenEmptyEvent_whenGetEventSummary_thenFreeSpotsEqualCapacity() {
    int eventId = seedEvent("Party", "MainHall", 0, 20, EventRegistrationStatus.RESERVATIONS_OPEN);

    EventSummary summary = eventService.getEventSummary(eventId);

    assertEquals(20, summary.getFreeSpotAmount());
  }

  @Test
  void givenUnknownEventId_whenGetEventSummary_thenEventNotFound() {
    seedEvent("Party", "MainHall", 0, 20, EventRegistrationStatus.RESERVATIONS_OPEN);

    assertThrows(EventNotFoundException.class, () -> eventService.getEventSummary(999));
  }

  @Test
  void givenZeroEventId_whenGetEventSummary_thenEventNotFound() {
    seedEvent("Party", "MainHall", 0, 20, EventRegistrationStatus.RESERVATIONS_OPEN);

    assertThrows(EventNotFoundException.class, () -> eventService.getEventSummary(0));
  }

  // ---------- groupByFillStatus ----------

  @Test
  void givenTwoDifferentStatuses_whenGroupByFillStatus_thenTwoGroups() {
    seedEvent("Party", "MainHall", 0, 20, EventRegistrationStatus.RESERVATIONS_OPEN);
    seedEvent("Concert", "WestHall", 20, 20, EventRegistrationStatus.ALL_RESERVED);

    Map<String, Long> result = eventService.groupByFillStatus();

    assertEquals(2, result.size());
  }

  @Test
  void givenTwoEventsSameStatus_whenGroupByFillStatus_thenCountIsTwo() {
    seedEvent("Party", "MainHall", 0, 20, EventRegistrationStatus.RESERVATIONS_OPEN);
    seedEvent("Concert", "WestHall", 5, 20, EventRegistrationStatus.RESERVATIONS_OPEN);

    Map<String, Long> result = eventService.groupByFillStatus();

    assertEquals(2, result.get("RESERVATIONS_OPEN"));
  }

  @Test
  void givenEmptyRepository_whenGroupByFillStatus_thenNoEventsPresent() {
    assertThrows(NoEventsPresentException.class, () -> eventService.groupByFillStatus());
  }

  @Test
  void givenEmptyRepository_whenGroupByFillStatus_thenOperationNameIsGroupByFillStatus() {
    NoEventsPresentException exception =
        assertThrows(NoEventsPresentException.class, () -> eventService.groupByFillStatus());

    assertEquals("groupByFillStatus", exception.getOperation());
  }

  // ---------- findMostPopular ----------

  @Test
  void givenTwoEvents_whenFindMostPopular_thenBothReturned() {
    seedEvent("Party", "MainHall", 5, 20, EventRegistrationStatus.RESERVATIONS_OPEN);
    seedEvent("Concert", "WestHall", 2, 20, EventRegistrationStatus.RESERVATIONS_OPEN);

    var result = eventService.findMostPopular(10);

    assertEquals(2, result.size());
  }

  @Test
  void givenEvents_whenFindMostPopularLimitOne_thenTopEventReturned() {
    seedEvent("Party", "MainHall", 5, 20, EventRegistrationStatus.RESERVATIONS_OPEN);
    seedEvent("Concert", "WestHall", 2, 20, EventRegistrationStatus.RESERVATIONS_OPEN);

    var result = eventService.findMostPopular(1);

    assertEquals("Party", result.getFirst().getEventName());
  }

  @Test
  void givenZeroLimit_whenFindMostPopular_thenNoEventsPresent() {
    seedEvent("Party", "MainHall", 5, 20, EventRegistrationStatus.RESERVATIONS_OPEN);

    assertThrows(NoEventsPresentException.class, () -> eventService.findMostPopular(0));
  }

  @Test
  void givenEmptyRepository_whenFindMostPopular_thenNoEventsPresent() {
    assertThrows(NoEventsPresentException.class, () -> eventService.findMostPopular(10));
  }

  // ---------- findByCreatedBetween ----------

  @Test
  void givenRegistrationsInRange_whenFindByCreatedBetween_thenMatchingReturned() {
    int participantId = seedParticipant("John", "Doe");
    int eventId = seedEvent("Party", "MainHall", 5, 20, EventRegistrationStatus.RESERVATIONS_OPEN);
    seedRegistration(participantId, eventId, OffsetDateTime.parse("2026-06-01T10:00:00+00:00"));
    seedRegistration(participantId, eventId, OffsetDateTime.parse("2026-08-01T10:00:00+00:00"));
    seedRegistration(participantId, eventId, OffsetDateTime.parse("2027-01-01T10:00:00+00:00"));

    var result =
        eventService.findByCreatedBetween(
            OffsetDateTime.parse("2026-01-01T00:00:00+00:00"),
            OffsetDateTime.parse("2026-12-31T23:59:59+00:00"));

    assertEquals(2, result.size());
  }

  @Test
  void givenNullBounds_whenFindByCreatedBetween_thenAllReturned() {
    int participantId = seedParticipant("John", "Doe");
    int eventId = seedEvent("Party", "MainHall", 5, 20, EventRegistrationStatus.RESERVATIONS_OPEN);
    seedRegistration(participantId, eventId, OffsetDateTime.parse("2026-06-01T10:00:00+00:00"));

    var result = eventService.findByCreatedBetween(null, null);

    assertEquals(1, result.size());
  }

  @Test
  void givenEmptyRepository_whenFindByCreatedBetween_thenEmptyList() {
    var result =
        eventService.findByCreatedBetween(
            OffsetDateTime.parse("2026-01-01T00:00:00+00:00"),
            OffsetDateTime.parse("2026-12-31T23:59:59+00:00"));

    assertEquals(0, result.size());
  }

  // ---------- searchByFragment ----------

  @Test
  void givenParticipants_whenSearchByFragment_thenMatchingReturned() {
    seedParticipant("Alice", "Smith");
    seedParticipant("Bob", "Johnson");

    var result = eventService.searchByFragment("Al");

    assertEquals(1, result.size());
  }

  @Test
  void givenParticipants_whenSearchByEmptyFragment_thenAllReturned() {
    seedParticipant("Alice", "Smith");
    seedParticipant("Bob", "Johnson");

    var result = eventService.searchByFragment("");

    assertEquals(2, result.size());
  }

  @Test
  void givenParticipants_whenSearchByUnknownFragment_thenEmptyList() {
    seedParticipant("Alice", "Smith");

    var result = eventService.searchByFragment("zzz");

    assertEquals(0, result.size());
  }

  // ---------- getEventsGrouped ----------

  @Test
  void givenTwoEventsSameLocation_whenGetEventsGrouped_thenOneGroupOfTwo() {
    seedEvent("Party", "MainHall", 0, 20, EventRegistrationStatus.RESERVATIONS_OPEN);
    seedEvent("Concert", "MainHall", 0, 20, EventRegistrationStatus.RESERVATIONS_OPEN);

    Map<String, List<EventResponse>> result = eventService.getEventsGrouped(Event::getLocation);

    assertEquals(2, result.get("MainHall").size());
  }

  @Test
  void givenTwoEventsDifferentLocations_whenGetEventsGrouped_thenTwoGroups() {
    seedEvent("Party", "MainHall", 0, 20, EventRegistrationStatus.RESERVATIONS_OPEN);
    seedEvent("Concert", "WestHall", 0, 20, EventRegistrationStatus.RESERVATIONS_OPEN);

    Map<String, List<EventResponse>> result = eventService.getEventsGrouped(Event::getLocation);

    assertEquals(2, result.size());
  }

  @Test
  void givenEmptyRepository_whenGetEventsGrouped_thenEmptyMap() {
    Map<String, List<EventResponse>> result = eventService.getEventsGrouped(Event::getLocation);

    assertEquals(0, result.size());
  }

  // ---------- getRegistrationRequestsInWaitingQueue ----------

  @Test
  void givenWaitingRegistration_whenGetQueue_thenRegistrationMapped() {
    int eventId = seedEvent("Party", "MainHall", 20, 20, EventRegistrationStatus.ALL_RESERVED);
    int participantId = seedParticipant("John", "Doe");
    seedWaitingRegistration(participantId, eventId);

    var result = eventService.getRegistrationRequestsInWaitingQueue(eventId);

    assertEquals(1, result.getFirst().getRegistrationId());
  }

  @Test
  void givenWaitingRegistration_whenGetQueue_thenStatusIsWaiting() {
    int eventId = seedEvent("Party", "MainHall", 20, 20, EventRegistrationStatus.ALL_RESERVED);
    int participantId = seedParticipant("John", "Doe");
    seedWaitingRegistration(participantId, eventId);

    var result = eventService.getRegistrationRequestsInWaitingQueue(eventId);

    assertEquals(EventRegRequestStatus.WAITING, result.getFirst().getEventRegRequestStatus());
  }

  @Test
  void givenEmptyQueue_whenGetQueue_thenEmptyList() {
    var result = eventService.getRegistrationRequestsInWaitingQueue(1);

    assertEquals(0, result.size());
  }

  @Test
  void
      givenTwoWaitingRegistrationsForDifferentEvents_whenGetQueueForOneEvent_thenOnlyThatReturned() {
    int eventId1 = seedEvent("Party", "MainHall", 20, 20, EventRegistrationStatus.ALL_RESERVED);
    int eventId2 = seedEvent("Concert", "WestHall", 20, 20, EventRegistrationStatus.ALL_RESERVED);
    int participantId1 = seedParticipant("John", "Doe");
    int participantId2 = seedParticipant("Jane", "Smith");
    seedWaitingRegistration(participantId1, eventId1);
    seedWaitingRegistration(participantId2, eventId2);

    var result = eventService.getRegistrationRequestsInWaitingQueue(eventId1);

    assertEquals(1, result.size());
  }
}
