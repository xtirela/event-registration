package view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Scanner;
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
import util.LogCapture;

/** Tests for the new menu actions (17-21) of {@link ConsoleView}. */
@Testcontainers
class ConsoleViewNewFunctionsTest {

  private ConsoleView consoleView;
  private EventRepository eventRepository;
  private ParticipantRepository participantRepository;
  private EventRegistrationRepository registrationRepository;

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
    EventServiceImpl eventService =
        new EventServiceImpl(eventRepository, participantRepository, registrationRepository);
    consoleView = new ConsoleView(eventService);
  }

  private void performConsoleAction(int action, String input) {
    System.setIn(new ByteArrayInputStream((input + "\n").getBytes()));
    Scanner scanner = new Scanner(System.in);
    consoleView.performAction(action, scanner);
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

  private int seedEvent(String name, String location, int current, int max) {
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
                .eventRegistrationStatus(EventRegistrationStatus.RESERVATIONS_OPEN)
                .eventGenderRequirement(EventGenderRequirement.NONE)
                .createdAt(OffsetDateTime.parse("2026-01-01T10:00:00+00:00"))
                .build())
        .getId();
  }

  private int seedParticipant(String firstName, String lastName, ParticipantGender gender) {
    return participantRepository
        .save(
            Participant.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(firstName.toLowerCase() + "@test.com")
                .age(25)
                .participantGender(gender)
                .registeredAt(OffsetDateTime.parse("2026-01-01T10:00:00+00:00"))
                .build())
        .getId();
  }

  private void seedRegistration(int participantId, int eventId, String description) {
    registrationRepository.save(
        EventRegistration.builder()
            .participantId(participantId)
            .eventId(eventId)
            .eventRegRequestStatus(EventRegRequestStatus.ACCEPTED)
            .description(description)
            .createdAt(OffsetDateTime.parse("2026-06-01T10:00:00+00:00"))
            .build());
  }

  // ---------- case 17: getEventSummary ----------

  @Test
  void givenEvent_whenSummaryAction_thenSummaryPrinted() {
    int eventId = seedEvent("Party", "MainHall", 5, 20);

    String output = LogCapture.capture(() -> performConsoleAction(17, String.valueOf(eventId)));

    assertTrue(output.contains("Party"));
  }

  @Test
  void givenNoEvent_whenSummaryAction_thenErrorPrinted() {
    String output = LogCapture.capture(() -> performConsoleAction(17, "999"));

    assertTrue(output.contains("ОШИБКА"));
  }

  @Test
  void givenNonNumericInput_whenSummaryAction_thenErrorPrinted() {
    seedEvent("Party", "MainHall", 5, 20);

    String output = LogCapture.capture(() -> performConsoleAction(17, "abc"));

    assertTrue(output.contains("ОШИБКА"));
  }

  // ---------- case 18: groupByFillStatus ----------

  @Test
  void givenEvents_whenGroupByFillStatusAction_thenStatusesPrinted() {
    seedEvent("Party", "MainHall", 20, 20);
    seedEvent("Concert", "WestHall", 20, 20);

    String output = LogCapture.capture(() -> performConsoleAction(18, ""));

    assertTrue(output.contains("RESERVATIONS_OPEN"));
  }

  @Test
  void givenNoEvents_whenGroupByFillStatusAction_thenErrorPrinted() {
    String output = LogCapture.capture(() -> performConsoleAction(18, ""));

    assertTrue(output.contains("ОШИБКА"));
  }

  // ---------- case 19: findMostPopular ----------

  @Test
  void givenEvents_whenMostPopularAction_thenEventsPrinted() {
    seedEvent("Party", "MainHall", 5, 20);
    seedEvent("Concert", "WestHall", 2, 20);

    String output = LogCapture.capture(() -> performConsoleAction(19, "2"));

    assertTrue(output.contains("Party"));
  }

  @Test
  void givenNoEvents_whenMostPopularAction_thenErrorPrinted() {
    String output = LogCapture.capture(() -> performConsoleAction(19, "2"));

    assertTrue(output.contains("ОШИБКА"));
  }

  @Test
  void givenNonNumericLimit_whenMostPopularAction_thenErrorPrinted() {
    String output = LogCapture.capture(() -> performConsoleAction(19, "abc"));

    assertTrue(output.contains("ОШИБКА"));
  }

  // ---------- case 20: searchByFragment ----------

  @Test
  void givenParticipants_whenFragmentAction_thenMatchingPrinted() {
    seedParticipant("Alice", "Smith", ParticipantGender.FEMALE);

    String output = LogCapture.capture(() -> performConsoleAction(20, "Al"));

    assertTrue(output.contains("Alice"));
  }

  @Test
  void givenNoMatches_whenFragmentAction_thenNoParticipantPrinted() {
    String output = LogCapture.capture(() -> performConsoleAction(20, "zzz"));

    assertFalse(output.contains("Alice"));
  }

  // ---------- case 21: findByCreatedBetween ----------

  @Test
  void givenRegistration_whenBetweenDatesAction_thenRegistrationPrinted() {
    int participantId = seedParticipant("Alice", "Smith", ParticipantGender.FEMALE);
    int eventId = seedEvent("Party", "MainHall", 5, 20);
    seedRegistration(participantId, eventId, "test-desc");

    String output =
        LogCapture.capture(
            () -> performConsoleAction(21, "2026-01-01T00:00:00+00:00,2026-12-31T23:59:59+00:00"));

    assertTrue(output.contains("test-desc"));
  }

  @Test
  void givenMalformedDate_whenBetweenDatesAction_thenErrorPrinted() {
    String output =
        LogCapture.capture(() -> performConsoleAction(21, "2026-01-01T00:00:00+00:00,abc"));

    assertTrue(output.contains("ОШИБКА"));
  }

  @Test
  void givenWrongElementCount_whenBetweenDatesAction_thenErrorPrinted() {
    String output = LogCapture.capture(() -> performConsoleAction(21, "2026-01-01"));

    assertTrue(output.contains("ОШИБКА"));
  }
}
