// package com.eventreg.integration.concurrency;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertTrue;
//
// import com.eventreg.dto.request.create.EventRegistrationCreateRequest;
// import com.eventreg.model.Event;
// import com.eventreg.model.EventRegistration;
// import com.eventreg.model.Participant;
// import com.eventreg.model.User;
// import com.eventreg.model.enums.EventRegistrationStatus;
// import com.eventreg.model.enums.EventReservationStatus;
// import com.eventreg.repository.EventRepository;
// import com.eventreg.repository.ParticipantRepository;
// import com.eventreg.repository.UserRepository;
// import com.eventreg.service.EventRegistrationService;
// import com.eventreg.service.WaitingQueueScheduler;
// import java.util.List;
// import java.util.concurrent.atomic.AtomicInteger;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.test.context.DynamicPropertyRegistry;
// import org.springframework.test.context.DynamicPropertySource;
// import org.testcontainers.containers.PostgreSQLContainer;
// import org.testcontainers.junit.jupiter.Container;
// import org.testcontainers.junit.jupiter.Testcontainers;
//
/// ** Concurrency tests for the event waiting queue and its scheduler against real PostgreSQL. */
// @Testcontainers
// public class WaitingQueueConcurrencyTest extends AbstractConcurrencyIntegrationTest {
//
//  @Container
//  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");
//
//  @DynamicPropertySource
//  static void databaseProperties(DynamicPropertyRegistry registry) {
//    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
//    registry.add("spring.datasource.username", POSTGRES::getUsername);
//    registry.add("spring.datasource.password", POSTGRES::getPassword);
//  }
//
//  @Autowired private EventRegistrationService registrationService;
//  @Autowired private WaitingQueueScheduler scheduler;
//  @Autowired private EventRepository eventRepository;
//  @Autowired private ParticipantRepository participantRepository;
//  @Autowired private UserRepository userRepository;
//
//  @BeforeEach
//  void cleanUp() {
//    cleanDatabase();
//  }
//
//  @Test
//  void shouldNotOverbookAccepted_whenHundredConcurrentRegistrationsOnWaitlistEvent()
//      throws Exception {
//    User organizer = userRepository.save(organizer());
//    Event event = eventRepository.save(waitlistEvent(organizer, 10));
//
//    List<Participant> participants = seedParticipants(100);
//
//    ConcurrencyTestSupport.runConcurrently(
//        100,
//        index -> {
//          registerQuietly(
//              EventRegistrationCreateRequest.builder()
//                  .eventId(event.getId())
//                  .participantId(participants.get(index).getId())
//                  .build());
//          return 1;
//        });
//
//    long accepted = countStatus(event.getId(), EventRegistrationStatus.ACCEPTED);
//    long waiting = countStatus(event.getId(), EventRegistrationStatus.WAITING);
//
//    assertTrue(
//        accepted <= event.getMaxParticipantAmount(),
//        "ACCEPTED ("
//            + accepted
//            + ") must never exceed capacity "
//            + event.getMaxParticipantAmount());
//    assertEquals(
//        100L,
//        accepted + waiting,
//        "every registration must end up either ACCEPTED or WAITING, but rejected");
//  }
//
//  @Test
//  void shouldPromoteWaitingUser_whenSpotFreedAndSchedulerRuns() throws Exception {
//    User organizer = userRepository.save(organizer());
//    Event event = eventRepository.save(waitlistEvent(organizer, 2));
//
//    List<Participant> participants = seedParticipants(5);
//
//    for (int i = 0; i < 2; i++) {
//      registerQuietly(
//          EventRegistrationCreateRequest.builder()
//              .eventId(event.getId())
//              .participantId(participants.get(i).getId())
//              .build());
//    }
//    for (int i = 2; i < 5; i++) {
//      registerQuietly(
//          EventRegistrationCreateRequest.builder()
//              .eventId(event.getId())
//              .participantId(participants.get(i).getId())
//              .build());
//    }
//
//    assertEquals(2L, countStatus(event.getId(), EventRegistrationStatus.ACCEPTED));
//    assertEquals(3L, countStatus(event.getId(), EventRegistrationStatus.WAITING));
//
//    EventRegistration acceptedReg =
//        registrationService
//            .findAll(org.springframework.data.domain.Pageable.unpaged())
//            .getContent()
//            .stream()
//            .filter(er ->
// er.getEventRegistrationStatus().equals(EventRegistrationStatus.ACCEPTED))
//            .findFirst()
//            .orElseThrow();
//
//    registrationService.changeRegistrationRequestStatus(
//        acceptedReg.getId(), EventRegistrationStatus.CANCELLED, "cancel to free a spot");
//
//    scheduler.updateAllWaitingQueues();
//
//    assertEquals(2L, countStatus(event.getId(), EventRegistrationStatus.ACCEPTED));
//    assertEquals(2L, countStatus(event.getId(), EventRegistrationStatus.WAITING));
//
//    Event reloaded = eventRepository.findById(event.getId()).orElseThrow();
//    assertEquals(EventReservationStatus.WAITLIST, reloaded.getEventReservationStatus());
//  }
//
//  private Event waitlistEvent(User organizer, int maxParticipants) {
//    Event event = event(organizer, maxParticipants, false);
//    event.setWaitlistWhenAllReserved(true);
//    return event;
//  }
//
//  private List<Participant> seedParticipants(int amount) {
//    AtomicInteger counter = new AtomicInteger();
//    return java.util.stream.IntStream.range(0, amount)
//        .mapToObj(
//            i -> {
//              User user =
//                  userRepository.save(
//                      User.builder()
//                          .username("wq" + System.nanoTime() + counter.incrementAndGet())
//                          .email("wq" + System.nanoTime() + counter.incrementAndGet() + "@e.com")
//                          .password("password123")
//                          .role(com.eventreg.model.enums.RBAC.Role.PARTICIPANT)
//                          .build());
//              return participantRepository.save(participant(user));
//            })
//        .toList();
//  }
//
//  private void registerQuietly(EventRegistrationCreateRequest request) {
//    try {
//      registrationService.createEventRegistration(request);
//    } catch (RuntimeException ignored) {
//      // capacity race may reject some registrations as DENIED
//    }
//  }
//
//  private long countStatus(Long eventId, EventRegistrationStatus status) {
//    return jdbcTemplate.queryForObject(
//        "SELECT COUNT(*) FROM event_registrations WHERE event_id = ? AND event_registration_status
// "
//            + "= ?",
//        Long.class,
//        eventId,
//        status.name());
//  }
// }
