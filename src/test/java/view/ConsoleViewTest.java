package view;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import exception.EventNotFoundException;
import exception.ParticipantNotFoundException;
import exception.RegistrationNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.stream.Stream;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import model.enums.EventRegRequestStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import repository.EventRegistrationRepository;
import repository.EventRepository;
import repository.ParticipantRepository;
import repository.implementation.Jdbc.EventRegistrationRepositoryJdbc;
import repository.implementation.Jdbc.EventRepositoryJdbc;
import repository.implementation.Jdbc.ParticipantRepositoryJdbc;
import service.EventService;
import service.implementation.EventServiceImpl;

/** Integration tests for {@link ConsoleView} menu actions. */
@Testcontainers
public class ConsoleViewTest {

  private EventService eventService;
  private ConsoleView consoleView;

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

    ParticipantRepository participantRepo = new ParticipantRepositoryJdbc();
    EventRepository eventRepo = new EventRepositoryJdbc();
    EventRegistrationRepository regRepo = new EventRegistrationRepositoryJdbc();

    eventService = new EventServiceImpl(eventRepo, participantRepo, regRepo);

    consoleView = new ConsoleView(eventService);
  }

  // ---------- helpers ----------

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

  void performConsoleAction(int action, String input) {
    System.setIn(new ByteArrayInputStream((input + "\n").getBytes()));

    Scanner scanner = new Scanner(System.in);

    consoleView.performAction(action, scanner);
  }

  String captureOutput(Runnable action) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern("%msg%n");
    encoder.start();
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<>();
    appender.setContext(context);
    appender.setEncoder(encoder);
    appender.setOutputStream(buffer);
    appender.start();
    Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
    root.addAppender(appender);
    try {
      action.run();
    } finally {
      root.detachAppender(appender);
      appender.stop();
    }
    return buffer.toString();
  }

  void arrangeParticipant() {
    performConsoleAction(1, "John,Doe,john@test.com,25,MALE");
  }

  void arrangeEvent() {
    performConsoleAction(2, "Party,MainHall,2030-05-01T13:30:00+02:00,3,18,20,NONE");
  }

  void arrangeParticipantAndEvent() {
    arrangeParticipant();
    arrangeEvent();
  }

  void arrangeRegistration() {
    arrangeParticipantAndEvent();
    performConsoleAction(3, "1,1");
  }

  void arrangeTwoEvents() {
    performConsoleAction(2, "Party,MainHall,2030-05-01T13:30:00+02:00,3,18,20,NONE");
    performConsoleAction(2, "Concert,WestHall,2030-06-01T15:00:00+00:00,4,21,50,FEMALE_ONLY");
  }

  void arrangeTwoParticipants() {
    performConsoleAction(1, "Alice,Smith,alice@test.com,20,FEMALE");
    performConsoleAction(1, "Bob,Johnson,bob@test.com,30,MALE");
  }

  void arrangeFullEventScenario() {
    performConsoleAction(2, "Full Event,MainHall,2030-07-01T10:00:00+00:00,2,18,1,NONE");
    performConsoleAction(1, "John,Doe,john@test.com,25,MALE");
    performConsoleAction(1, "Jane,Smith,jane@test.com,25,FEMALE");
    performConsoleAction(3, "1,1");
  }

  // ---------- method sources ----------

  static Stream<Arguments> validParticipantInputs() {
    return Stream.of(
        Arguments.of("John,Doe,john@test.com,25,MALE", "John"),
        Arguments.of("Jane,Smith,jane@test.com,30,FEMALE", "Jane"));
  }

  static Stream<Arguments> edgeParticipantInputs() {
    return Stream.of(
        Arguments.of("Baby,Test,baby@test.com,1,MALE", "Baby"),
        Arguments.of("Elder,Test,elder@test.com,150,FEMALE", "Elder"),
        Arguments.of("Anon,Test,anon@test.com,25,NOT_SPECIFIED", "Anon"));
  }

  static Stream<String> invalidParticipantInputs() {
    return Stream.of(
        "John,Doe,john@testcom,25,MALE",
        " ,Doe,john@test.com,25,MALE",
        "John,,john@test.com,25,MALE",
        "John,Doe,,25,MALE",
        "John,Doe,john@test.com,0,MALE",
        "John,Doe,john@test.com,151,MALE",
        "John,Doe,john@test.com,abc,MALE",
        "John,Doe,john@test.com,25",
        "John,Doe,john@test.com,25,");
  }

  static Stream<Arguments> validEventInputs() {
    return Stream.of(
        Arguments.of("Party,MainHall,2030-05-01T13:30:00+02:00,3,18,20,NONE", "Party"),
        Arguments.of("Concert,WestHall,2030-06-01T15:00:00+00:00,4,21,50,FEMALE_ONLY", "Concert"));
  }

  static Stream<Arguments> edgeEventInputs() {
    return Stream.of(
        Arguments.of("Kids,MainHall,2030-07-01T10:00:00+00:00,2,0,100,NONE", "Kids"),
        Arguments.of("Seniors,MainHall,2030-07-01T10:00:00+00:00,2,150,100,NONE", "Seniors"),
        Arguments.of("Small,MainHall,2030-07-01T10:00:00+00:00,2,18,1,NONE", "Small"),
        Arguments.of("Men Only,MainHall,2030-07-01T10:00:00+00:00,2,18,50,MALE_ONLY", "Men Only"));
  }

  static Stream<String> invalidEventInputs() {
    return Stream.of(
        "Party,MainHall,2020-01-01T10:00:00+00:00,2,18,50,NONE",
        ",MainHall,2030-07-01T10:00:00+00:00,2,18,50,NONE",
        "Party,MainHall,2030-07-01T10:00:00+00:00,2,18,0,NONE",
        "Party,MainHall,2030-07-01T10:00:00+00:00,2,18,50,BAD",
        "Party,MainHall,2030-07-01T10:00:00+00:00,2,18,50");
  }

  static Stream<Arguments> validRegistrationInputs() {
    return Stream.of(
        Arguments.of(
            "John,Doe,john@test.com,25,MALE",
            "Party,MainHall,2030-05-01T13:30:00+02:00,3,18,20,NONE",
            "1,1",
            EventRegRequestStatus.ACCEPTED));
  }

  static Stream<Arguments> edgeRegistrationInputs() {
    return Stream.of(
        Arguments.of(
            "Teen,Test,teen@test.com,17,MALE",
            "Party,MainHall,2030-05-01T13:30:00+02:00,3,18,20,NONE",
            "1,1",
            EventRegRequestStatus.DENIED),
        Arguments.of(
            "John,Doe,john@test.com,25,MALE",
            "Ladies,MainHall,2030-05-01T13:30:00+02:00,3,18,20,FEMALE_ONLY",
            "1,1",
            EventRegRequestStatus.DENIED));
  }

  static Stream<Arguments> missingParticipantRegistrationInputs() {
    return Stream.of(
        Arguments.of(
            "John,Doe,john@test.com,25,MALE",
            "Party,MainHall,2030-05-01T13:30:00+02:00,3,18,20,NONE",
            "999,1"));
  }

  static Stream<Arguments> missingEventRegistrationInputs() {
    return Stream.of(
        Arguments.of(
            "John,Doe,john@test.com,25,MALE",
            "Party,MainHall,2030-05-01T13:30:00+02:00,3,18,20,NONE",
            "1,999"));
  }

  static Stream<Arguments> validCancelInputs() {
    return Stream.of(
        Arguments.of(
            "John,Doe,john@test.com,25,MALE",
            "Party,MainHall,2030-05-01T13:30:00+02:00,3,18,20,NONE",
            "1,1",
            "1"),
        Arguments.of(
            "Jane,Smith,jane@test.com,30,FEMALE",
            "Concert,WestHall,2030-06-01T15:00:00+00:00,4,21,50,FEMALE_ONLY",
            "1,1",
            "1"));
  }

  static Stream<Arguments> invalidCancelCreationInputs() {
    return Stream.of(
        Arguments.of(
            "John,Doe,john@testcom,25,MALE",
            "Party,MainHall,2030-05-01T13:30:00+02:00,3,18,20,NONE",
            "1,1",
            "1"));
  }

  static Stream<Arguments> invalidCancelIdInputs() {
    return Stream.of(
        Arguments.of(
            "John,Doe,john@test.com,25,MALE",
            "Party,MainHall,2030-05-01T13:30:00+02:00,3,18,20,NONE",
            "1,1",
            "999"));
  }

  static Stream<String> sortFields() {
    return Stream.of("AGE", "NAME", "REGISTERED_AT");
  }

  // ---------- case 1: create participant ----------

  @ParameterizedTest
  @MethodSource("validParticipantInputs")
  void givenValidDataWhenCreateParticipantThenParticipantCreated(
      String input, String expectedFirstName) {
    performConsoleAction(1, input);

    assertEquals(expectedFirstName, eventService.getParticipantById(1).getFirstName());
  }

  @ParameterizedTest
  @MethodSource("edgeParticipantInputs")
  void givenEdgeCaseDataWhenCreateParticipantThenParticipantCreated(
      String input, String expectedFirstName) {
    performConsoleAction(1, input);

    assertEquals(expectedFirstName, eventService.getParticipantById(1).getFirstName());
  }

  @ParameterizedTest
  @MethodSource("invalidParticipantInputs")
  void givenInvalidDataWhenCreateParticipantThenParticipantNotCreated(String input) {
    performConsoleAction(1, input);

    assertThrows(ParticipantNotFoundException.class, () -> eventService.getParticipantById(1));
  }

  // ---------- case 2: create event ----------

  @ParameterizedTest
  @MethodSource("validEventInputs")
  void givenValidDataWhenCreateEventThenEventCreated(String input, String expectedName) {
    performConsoleAction(2, input);

    assertEquals(expectedName, eventService.getEventById(1).getEventName());
  }

  @ParameterizedTest
  @MethodSource("edgeEventInputs")
  void givenEdgeCaseDataWhenCreateEventThenEventCreated(String input, String expectedName) {
    performConsoleAction(2, input);

    assertEquals(expectedName, eventService.getEventById(1).getEventName());
  }

  @ParameterizedTest
  @MethodSource("invalidEventInputs")
  void givenInvalidDataWhenCreateEventThenEventNotCreated(String input) {
    performConsoleAction(2, input);

    assertThrows(EventNotFoundException.class, () -> eventService.getEventById(1));
  }

  // ---------- case 3: register participant ----------

  @ParameterizedTest
  @MethodSource("validRegistrationInputs")
  void givenValidDataWhenRegisterParticipantThenStatusAccepted(
      String participantInput, String eventInput, String regInput, EventRegRequestStatus status) {
    performConsoleAction(1, participantInput);
    performConsoleAction(2, eventInput);

    performConsoleAction(3, regInput);

    assertEquals(status, eventService.getRegistrationRequestById(1).getEventRegRequestStatus());
  }

  @ParameterizedTest
  @MethodSource("edgeRegistrationInputs")
  void givenEdgeCaseDataWhenRegisterParticipantThenStatusDenied(
      String participantInput, String eventInput, String regInput, EventRegRequestStatus status) {
    performConsoleAction(1, participantInput);
    performConsoleAction(2, eventInput);

    performConsoleAction(3, regInput);

    assertEquals(status, eventService.getRegistrationRequestById(1).getEventRegRequestStatus());
  }

  @ParameterizedTest
  @MethodSource("missingParticipantRegistrationInputs")
  void givenMissingParticipantWhenRegisterParticipantThenRegistrationNotResolvable(
      String participantInput, String eventInput, String regInput) {
    performConsoleAction(1, participantInput);
    performConsoleAction(2, eventInput);

    performConsoleAction(3, regInput);

    assertThrows(
        RegistrationNotFoundException.class, () -> eventService.getRegistrationRequestById(1));
  }

  @ParameterizedTest
  @MethodSource("missingEventRegistrationInputs")
  void givenMissingEventWhenRegisterParticipantThenRegistrationNotResolvable(
      String participantInput, String eventInput, String regInput) {
    performConsoleAction(1, participantInput);
    performConsoleAction(2, eventInput);

    performConsoleAction(3, regInput);

    assertThrows(
        RegistrationNotFoundException.class, () -> eventService.getRegistrationRequestById(1));
  }

  @Test
  void givenMalformedRegistrationInput_whenRegisterParticipant_thenErrorPrinted() {
    arrangeParticipantAndEvent();

    String output = captureOutput(() -> performConsoleAction(3, "1"));

    assertTrue(output.contains("ОШИБКА"));
  }

  @Test
  void givenFullEvent_whenRegisterAndChooseYes_thenWaitingQueue() {
    arrangeFullEventScenario();

    performConsoleAction(3, "2,1\nY");

    assertEquals(
        EventRegRequestStatus.WAITING,
        eventService.getRegistrationRequestById(2).getEventRegRequestStatus());
  }

  @Test
  void givenFullEvent_whenRegisterAndChooseNo_thenDenied() {
    arrangeFullEventScenario();

    performConsoleAction(3, "2,1\nN");

    assertEquals(
        EventRegRequestStatus.DENIED,
        eventService.getRegistrationRequestById(2).getEventRegRequestStatus());
  }

  @Test
  void givenFullEvent_whenRegisterAndChooseOther_thenDenied() {
    arrangeFullEventScenario();

    performConsoleAction(3, "2,1\nX");

    assertEquals(
        EventRegRequestStatus.DENIED,
        eventService.getRegistrationRequestById(2).getEventRegRequestStatus());
  }

  // ---------- case 4: cancel registration ----------

  @ParameterizedTest
  @MethodSource("validCancelInputs")
  void givenValidDataWhenCancelRegistrationThenStatusCancelled(
      String participantInput, String eventInput, String regInput, String cancelInput) {
    performConsoleAction(1, participantInput);
    performConsoleAction(2, eventInput);
    performConsoleAction(3, regInput);

    performConsoleAction(4, cancelInput);

    assertEquals(
        EventRegRequestStatus.CANCELLED,
        eventService.getRegistrationRequestById(1).getEventRegRequestStatus());
  }

  @ParameterizedTest
  @MethodSource("validCancelInputs")
  void givenValidDataWhenCancelRegistrationThenParticipantAmountZero(
      String participantInput, String eventInput, String regInput, String cancelInput) {
    performConsoleAction(1, participantInput);
    performConsoleAction(2, eventInput);
    performConsoleAction(3, regInput);

    performConsoleAction(4, cancelInput);

    assertEquals(0, eventService.getEventById(1).getCurrentParticipantAmount());
  }

  @ParameterizedTest
  @MethodSource("invalidCancelCreationInputs")
  void givenInvalidCreationWhenCancelRegistrationThenRegistrationNotResolvable(
      String participantInput, String eventInput, String regInput, String cancelInput) {
    performConsoleAction(1, participantInput);
    performConsoleAction(2, eventInput);
    performConsoleAction(3, regInput);

    performConsoleAction(4, cancelInput);

    assertThrows(
        RegistrationNotFoundException.class, () -> eventService.getRegistrationRequestById(1));
  }

  @ParameterizedTest
  @MethodSource("invalidCancelIdInputs")
  void givenInvalidCancelIdWhenCancelRegistrationThenRegistrationStaysAccepted(
      String participantInput, String eventInput, String regInput, String cancelInput) {
    performConsoleAction(1, participantInput);
    performConsoleAction(2, eventInput);
    performConsoleAction(3, regInput);

    performConsoleAction(4, cancelInput);

    assertEquals(
        EventRegRequestStatus.ACCEPTED,
        eventService.getRegistrationRequestById(1).getEventRegRequestStatus());
  }

  // ---------- case 5: get event by id ----------

  @Test
  void givenExistingEvent_whenGetEventById_thenEventPrinted() {
    arrangeEvent();

    String output = captureOutput(() -> performConsoleAction(5, "1"));

    assertTrue(output.contains("Party"));
  }

  @Test
  void givenMissingEvent_whenGetEventById_thenErrorPrinted() {
    String output = captureOutput(() -> performConsoleAction(5, "999"));

    assertTrue(output.contains("ОШИБКА"));
  }

  // ---------- case 6: get all events ----------

  @Test
  void givenTwoEvents_whenGetAllEvents_thenEventsPrinted() {
    arrangeTwoEvents();

    String output = captureOutput(() -> performConsoleAction(6, ""));

    assertTrue(output.contains("Party"));
  }

  @Test
  void givenNoEvents_whenGetAllEvents_thenNoThrow() {
    assertDoesNotThrow(() -> performConsoleAction(6, ""));
  }

  // ---------- case 7: filter events ----------

  @Test
  void givenAgeFilter_whenFilterEvents_thenMatchingEventPrinted() {
    arrangeTwoEvents();

    String output = captureOutput(() -> performConsoleAction(7, ",,21"));

    assertTrue(output.contains("Concert"));
  }

  @Test
  void givenAgeFilter_whenFilterEvents_thenOtherEventNotPrinted() {
    arrangeTwoEvents();

    String output = captureOutput(() -> performConsoleAction(7, ",,21"));

    assertFalse(output.contains("Party"));
  }

  @Test
  void givenLocationAndAgeFilter_whenFilterEvents_thenMatchingEventPrinted() {
    arrangeTwoEvents();

    String output = captureOutput(() -> performConsoleAction(7, ",MainHall,18"));

    assertTrue(output.contains("Party"));
  }

  @Test
  void givenDateAndAgeFilter_whenFilterEvents_thenMatchingEventPrinted() {
    arrangeTwoEvents();

    String output = captureOutput(() -> performConsoleAction(7, "2030-05-01T13:30:00+02:00,,18"));

    assertTrue(output.contains("Party"));
  }

  @Test
  void givenInvalidFilter_whenFilterEvents_thenErrorPrinted() {
    arrangeTwoEvents();

    String output = captureOutput(() -> performConsoleAction(7, "21"));

    assertTrue(output.contains("ОШИБКА"));
  }

  // ---------- case 8: group events ----------

  @Test
  void givenTwoEvents_whenGroupEvents_thenMainHallPrinted() {
    arrangeTwoEvents();

    String output = captureOutput(() -> performConsoleAction(8, ""));

    assertTrue(output.contains("MainHall"));
  }

  @Test
  void givenNoEvents_whenGroupEvents_thenNoThrow() {
    assertDoesNotThrow(() -> performConsoleAction(8, ""));
  }

  // ---------- case 9: get participant by id ----------

  @Test
  void givenExistingParticipant_whenGetParticipantById_thenParticipantPrinted() {
    arrangeParticipant();

    String output = captureOutput(() -> performConsoleAction(9, "1"));

    assertTrue(output.contains("John"));
  }

  @Test
  void givenMissingParticipant_whenGetParticipantById_thenErrorPrinted() {
    String output = captureOutput(() -> performConsoleAction(9, "999"));

    assertTrue(output.contains("ОШИБКА"));
  }

  // ---------- case 10: get all participants ----------

  @Test
  void givenTwoParticipants_whenGetAllParticipants_thenParticipantsPrinted() {
    arrangeTwoParticipants();

    String output = captureOutput(() -> performConsoleAction(10, ""));

    assertTrue(output.contains("Alice"));
  }

  @Test
  void givenNoParticipants_whenGetAllParticipants_thenNoThrow() {
    assertDoesNotThrow(() -> performConsoleAction(10, ""));
  }

  // ---------- case 11: sort participants ----------

  @ParameterizedTest
  @MethodSource("sortFields")
  void givenSortFieldWhenSortParticipantsThenAliceBeforeBob(String sortField) {
    arrangeTwoParticipants();

    String output = captureOutput(() -> performConsoleAction(11, sortField));

    assertTrue(output.indexOf("Alice") < output.indexOf("Bob"));
  }

  @Test
  void givenGenderSort_whenSortParticipants_thenBobBeforeAlice() {
    arrangeTwoParticipants();

    String output = captureOutput(() -> performConsoleAction(11, "GENDER"));

    assertTrue(output.indexOf("Bob") < output.indexOf("Alice"));
  }

  @Test
  void givenInvalidSortField_whenSortParticipants_thenErrorPrinted() {
    arrangeTwoParticipants();

    String output = captureOutput(() -> performConsoleAction(11, "HEIGHT"));

    assertTrue(output.contains("ОШИБКА"));
  }

  // ---------- case 12: get registration by id ----------

  @Test
  void givenExistingRegistration_whenGetRegistrationById_thenRegistrationPrinted() {
    arrangeRegistration();

    String output = captureOutput(() -> performConsoleAction(12, "1"));

    assertTrue(output.contains("ACCEPTED"));
  }

  @Test
  void givenMissingRegistration_whenGetRegistrationById_thenErrorPrinted() {
    String output = captureOutput(() -> performConsoleAction(12, "999"));

    assertTrue(output.contains("ОШИБКА"));
  }

  // ---------- case 13: get all registrations ----------

  @Test
  void givenRegistration_whenGetAllRegistrations_thenRegistrationPrinted() {
    arrangeRegistration();

    String output = captureOutput(() -> performConsoleAction(13, ""));

    assertTrue(output.contains("ACCEPTED"));
  }

  @Test
  void givenNoRegistrations_whenGetAllRegistrations_thenNoThrow() {
    assertDoesNotThrow(() -> performConsoleAction(13, ""));
  }

  // ---------- case 14: undo latest action ----------

  @Test
  void givenCreatedEvent_whenUndo_thenEventRemoved() {
    arrangeEvent();

    performConsoleAction(14, "Y");

    assertThrows(EventNotFoundException.class, () -> eventService.getEventById(1));
  }

  @Test
  void givenCreatedParticipant_whenUndo_thenParticipantRemoved() {
    arrangeParticipant();

    performConsoleAction(14, "Y");

    assertThrows(ParticipantNotFoundException.class, () -> eventService.getParticipantById(1));
  }

  @Test
  void givenRegistration_whenUndo_thenRegistrationRemoved() {
    arrangeRegistration();

    performConsoleAction(14, "Y");

    assertThrows(
        RegistrationNotFoundException.class, () -> eventService.getRegistrationRequestById(1));
  }

  @Test
  void givenEmptyHistory_whenUndo_thenErrorPrinted() {
    String output = captureOutput(() -> performConsoleAction(14, ""));

    assertTrue(output.contains("ОШИБКА"));
  }

  @Test
  void givenInvalidConfirmation_whenUndo_thenEventNotRemoved() {
    arrangeEvent();

    performConsoleAction(14, "X");

    assertDoesNotThrow(() -> eventService.getEventById(1));
  }

  @Test
  void givenNegativeConfirmation_whenUndo_thenEventNotRemoved() {
    arrangeEvent();

    performConsoleAction(14, "N");

    assertDoesNotThrow(() -> eventService.getEventById(1));
  }

  // ---------- case 15: clear console ----------

  @Test
  void givenAnyState_whenClearConsole_thenNoThrow() {
    assertDoesNotThrow(() -> performConsoleAction(15, ""));
  }

  // ---------- default: invalid action ----------

  @Test
  void givenInvalidAction_whenPerformAction_thenNoThrow() {
    assertDoesNotThrow(() -> performConsoleAction(99, ""));
  }

  @Test
  void givenInvalidAction_whenPerformAction_thenInvalidMessagePrinted() {
    String output = captureOutput(() -> performConsoleAction(99, ""));

    assertTrue(output.contains("Введено некорректное значение!"));
  }
}
