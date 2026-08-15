package repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import config.RepositoryConfig;
import dto.EventCreateRequest;
import dto.EventRegRequest;
import dto.EventRegResponse;
import dto.EventResponse;
import dto.ParticipantCreateRequest;
import dto.ParticipantResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import model.enums.EventGenderRequirement;
import model.enums.EventRegRequestStatus;
import model.enums.ParticipantGender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import service.EventService;
import service.implementation.EventServiceImpl;

/** Tests for CSV persistence across service instances. */
public class CsvPersistenceTest {

  @TempDir Path tempDir;

  private RepositoryConfig config;

  @BeforeEach
  void setUp() {
    config =
        new RepositoryConfig(
            tempDir.resolve("events.csv").toString(),
            tempDir.resolve("participants.csv").toString(),
            tempDir.resolve("registrations.csv").toString());
  }

  @Test
  void givenPersistedData_whenNewService_thenEventsAndParticipantsRestored() {
    EventService first = new EventServiceImpl(config);
    first.createEvent(event("Tech Conference", 18, 100));
    first.createParticipant(participant("john@test.com", 25));

    EventService second = new EventServiceImpl(config);

    assertEquals("Tech Conference", second.getEventById(1).getEventName());
    assertEquals("john@test.com", second.getParticipantById(1).getEmail());
  }

  @Test
  void givenPersistedRegistration_whenNewService_thenRegistrationRestored() {
    EventService first = new EventServiceImpl(config);
    first.createEvent(event("Tech Conference", 18, 100));
    first.createParticipant(participant("john@test.com", 25));
    first.registerParticipant(new EventRegRequest(1, 1));

    EventService second = new EventServiceImpl(config);

    EventRegResponse restored = second.getRegistrationRequestById(1);
    assertEquals(1, restored.getParticipantId());
    assertEquals(EventRegRequestStatus.ACCEPTED, restored.getEventRegRequestStatus());
  }

  @Test
  void givenWaitingQueue_whenNewService_thenWaitingRegistrationsRestored() {
    EventService first = new EventServiceImpl(config);
    first.createEvent(event("Full Event", 18, 1));
    first.createParticipant(participant("john@test.com", 25));
    first.createParticipant(participant("jane@test.com", 25));
    first.registerParticipant(new EventRegRequest(1, 1));
    first.registerParticipant(new EventRegRequest(2, 1));
    first.changeRegistrationRequestStatus(2, EventRegRequestStatus.WAITING, "waiting", false);

    EventService second = new EventServiceImpl(config);

    List<EventRegResponse> waiting = second.getRegistrationRequestsInWaitingQueue(1);
    assertEquals(1, waiting.size());
    assertEquals(2, waiting.get(0).getRegistrationId());
    assertEquals(EventRegRequestStatus.WAITING, waiting.get(0).getEventRegRequestStatus());
  }

  @Test
  void givenRestoredData_whenCreateNew_thenIdsContinueWithoutCollision() {
    EventService first = new EventServiceImpl(config);
    first.createEvent(event("Tech Conference", 18, 100));
    first.createParticipant(participant("john@test.com", 25));
    first.registerParticipant(new EventRegRequest(1, 1));

    EventService second = new EventServiceImpl(config);
    EventResponse event = second.createEvent(event("Second Event", 18, 50));
    ParticipantResponse participant = second.createParticipant(participant("jane@test.com", 25));
    EventRegResponse registration = second.registerParticipant(new EventRegRequest(2, 2));

    assertNotNull(second.getEventById(event.getEventId()));
    assertNotNull(second.getParticipantById(participant.getParticipantId()));
    assertTrue(second.getRegistrationRequestById(registration.getRegistrationId()) != null);
  }

  private EventCreateRequest event(String name, int ageRequired, int max) {
    return EventCreateRequest.builder()
        .eventName(name)
        .location("Main Hall")
        .eventDate(OffsetDateTime.now().plusDays(30))
        .eventDuration(Duration.ofHours(2))
        .ageRequired(ageRequired)
        .maxParticipantAmount(max)
        .genderRequirement(EventGenderRequirement.NONE)
        .build();
  }

  private ParticipantCreateRequest participant(String email, int age) {
    return ParticipantCreateRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email(email)
        .age(age)
        .participantGender(ParticipantGender.MALE)
        .build();
  }
}
