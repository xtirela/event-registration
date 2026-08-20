package repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicInteger;
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
import repository.implementation.Jdbc.EventRegistrationRepositoryJdbc;
import repository.implementation.Jdbc.EventRepositoryJdbc;
import repository.implementation.Jdbc.ParticipantRepositoryJdbc;

/**
 * Интеграционные тесты скорости CRUD-операций JDBC-репозиториев на реальной PostgreSQL. NFR: время
 * отклика одной операции не превышает 50 мс (локальная PostgreSQL в Docker).
 */
@Testcontainers
public class DbPerformanceTest {

  private static final Duration MAX_OPERATION_TIME = Duration.ofMillis(50);
  private static final int ITERATIONS = 30;

  @Container
  private static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:18")
          .withDatabaseName("perf_db")
          .withUsername("perf")
          .withPassword("perf");

  private EventRepositoryJdbc eventRepo;
  private ParticipantRepositoryJdbc participantRepo;
  private EventRegistrationRepositoryJdbc registrationRepo;

  private Event fixtureEvent;
  private Participant fixtureParticipant;
  private EventRegistration fixtureRegistration;

  @BeforeAll
  static void setUpDatabase() throws SQLException {
    System.setProperty("db.url", postgres.getJdbcUrl());
    System.setProperty("db.user", postgres.getUsername());
    System.setProperty("db.password", postgres.getPassword());

    try (Connection connection =
        DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
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

  @BeforeEach
  void setUp() {
    eventRepo = new EventRepositoryJdbc();
    participantRepo = new ParticipantRepositoryJdbc();
    registrationRepo = new EventRegistrationRepositoryJdbc();

    fixtureEvent = eventRepo.save(event("Fixture Event " + System.nanoTime()));
    fixtureParticipant =
        participantRepo.save(participant("fixture@" + System.nanoTime() + ".test"));
    fixtureRegistration =
        registrationRepo.save(registration(fixtureParticipant.getId(), fixtureEvent.getId()));
  }

  @Test
  void eventSave_isFast() {
    AtomicInteger counter = new AtomicInteger();
    assertAvgUnder(
        MAX_OPERATION_TIME, () -> eventRepo.save(event("E" + counter.incrementAndGet())));
  }

  @Test
  void eventFindById_isFast() {
    assertAvgUnder(MAX_OPERATION_TIME, () -> eventRepo.findById(fixtureEvent.getId()));
  }

  @Test
  void eventUpdate_isFast() {
    assertAvgUnder(MAX_OPERATION_TIME, () -> eventRepo.update(fixtureEvent));
  }

  @Test
  void eventDelete_isFast() {
    AtomicInteger counter = new AtomicInteger();
    assertAvgUnder(
        MAX_OPERATION_TIME,
        () -> {
          Event saved = eventRepo.save(event("Del" + counter.incrementAndGet()));
          eventRepo.delete(saved.getId());
        });
  }

  @Test
  void participantSave_isFast() {
    AtomicInteger counter = new AtomicInteger();
    assertAvgUnder(
        MAX_OPERATION_TIME,
        () -> participantRepo.save(participant("p" + counter.incrementAndGet() + "@test.com")));
  }

  @Test
  void participantFindById_isFast() {
    assertAvgUnder(MAX_OPERATION_TIME, () -> participantRepo.findById(fixtureParticipant.getId()));
  }

  @Test
  void participantUpdate_isFast() {
    assertAvgUnder(MAX_OPERATION_TIME, () -> participantRepo.update(fixtureParticipant));
  }

  @Test
  void participantDelete_isFast() {
    AtomicInteger counter = new AtomicInteger();
    assertAvgUnder(
        MAX_OPERATION_TIME,
        () -> {
          Participant saved =
              participantRepo.save(participant("del" + counter.incrementAndGet() + "@test.com"));
          participantRepo.delete(saved.getId());
        });
  }

  @Test
  void registrationSave_isFast() {
    assertAvgUnder(
        MAX_OPERATION_TIME,
        () ->
            registrationRepo.save(registration(fixtureParticipant.getId(), fixtureEvent.getId())));
  }

  @Test
  void registrationFindById_isFast() {
    assertAvgUnder(
        MAX_OPERATION_TIME, () -> registrationRepo.findById(fixtureRegistration.getId()));
  }

  @Test
  void registrationUpdate_isFast() {
    assertAvgUnder(MAX_OPERATION_TIME, () -> registrationRepo.update(fixtureRegistration));
  }

  @Test
  void registrationDelete_isFast() {
    AtomicInteger counter = new AtomicInteger();
    assertAvgUnder(
        MAX_OPERATION_TIME,
        () -> {
          EventRegistration saved =
              registrationRepo.save(registration(fixtureParticipant.getId(), fixtureEvent.getId()));
          registrationRepo.delete(saved.getId());
        });
  }

  private void assertAvgUnder(Duration max, Runnable operation) {
    operation.run();

    long totalNanos = 0;
    for (int i = 0; i < ITERATIONS; i++) {
      long start = System.nanoTime();
      operation.run();
      totalNanos += System.nanoTime() - start;
    }

    double avgMillis = totalNanos / (double) ITERATIONS / 1_000_000.0;
    assertTrue(
        avgMillis <= max.toMillis(),
        "avg operation time "
            + String.format("%.2f", avgMillis)
            + "ms exceeds "
            + max.toMillis()
            + "ms");
  }

  private Event event(String name) {
    return Event.builder()
        .eventName(name)
        .location("Main Hall")
        .eventDate(OffsetDateTime.now().plusDays(10))
        .eventDuration(Duration.ofHours(2))
        .ageRequired(18)
        .eventGenderRequirement(EventGenderRequirement.NONE)
        .currentParticipantAmount(0)
        .maxParticipantAmount(100)
        .eventStatus(EventStatus.PLANNED)
        .eventRegistrationStatus(EventRegistrationStatus.RESERVATIONS_OPEN)
        .createdAt(OffsetDateTime.now())
        .build();
  }

  private Participant participant(String email) {
    return Participant.builder()
        .firstName("John")
        .lastName("Doe")
        .email(email)
        .age(25)
        .participantGender(ParticipantGender.MALE)
        .registeredAt(OffsetDateTime.now())
        .build();
  }

  private EventRegistration registration(int participantId, int eventId) {
    return EventRegistration.builder()
        .participantId(participantId)
        .eventId(eventId)
        .eventRegRequestStatus(EventRegRequestStatus.PENDING)
        .description("none")
        .createdAt(OffsetDateTime.now())
        .build();
  }
}
