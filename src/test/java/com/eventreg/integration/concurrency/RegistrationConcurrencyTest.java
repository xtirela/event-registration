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
// import com.eventreg.repository.EventRepository;
// import com.eventreg.repository.ParticipantRepository;
// import com.eventreg.repository.UserRepository;
// import com.eventreg.service.EventRegistrationService;
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
/// **
// * Concurrency tests around {@link EventRegistrationService#createEventRegistration} against a
// real
// * PostgreSQL. The scenarios assert the invariants a correct system must keep under parallel load
// * (capacity never exceeded, at most one registration per participant per event); current code may
// * violate them and that is the point — a failing assertion documents a concurrency bug.
// */
// @Testcontainers
// public class RegistrationConcurrencyTest extends AbstractConcurrencyIntegrationTest {
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
//  void shouldNotOverbook_WhenHundredConcurrentRegistrationsToSameEvent() throws Exception {
//    User organizer = userRepository.save(organizer());
//    Event event = eventRepository.save(event(organizer, 25, false));
//
//    List<Participant> participants = seedParticipants(100);
//
//    List<Integer> results =
//        ConcurrencyTestSupport.runConcurrently(
//            100,
//            index ->
//                invokeCreate(
//                    EventRegistrationCreateRequest.builder()
//                        .eventId(event.getId())
//                        .participantId(participants.get(index).getId())
//                        .build()));
//
//    int successes = results.stream().mapToInt(Integer::intValue).sum();
//
//    assertEquals(25, successes, "every concurrent registration call must be processed");
//
//    long acceptedCount = countStatus(event.getId(), EventRegistrationStatus.ACCEPTED);
//    assertTrue(
//        acceptedCount <= event.getMaxParticipantAmount(),
//        "ACCEPTED registrations "
//            + acceptedCount
//            + " must not exceed max capacity "
//            + event.getMaxParticipantAmount());
//  }
//
//  @Test
//  void shouldCreateSingleRegistration_WhenSameParticipantRegistersHundredTimesConcurrently()
//      throws Exception {
//    User organizer = userRepository.save(organizer());
//    User participantUser = userRepository.save(participantUser());
//    Participant participant = participantRepository.save(participant(participantUser));
//    Event event = eventRepository.save(event(organizer, 100, false));
//
//    ConcurrencyTestSupport.runConcurrently(
//        100,
//        index ->
//            invokeCreate(
//                EventRegistrationCreateRequest.builder()
//                    .eventId(event.getId())
//                    .participantId(participant.getId())
//                    .build()));
//
//    long rows =
//        jdbcTemplate.queryForObject(
//            "SELECT COUNT(*) FROM event_registrations WHERE event_id = ? AND participant_id = ?",
//            Long.class,
//            event.getId(),
//            participant.getId());
//    assertEquals(
//        1L,
//        rows,
//        "the same participant must hold a single registration per event even under 100 parallel "
//            + "requests");
//  }
//
//  @Test
//  void shouldKeepRegistrationsIsolated_whenConcurrentAcrossDifferentEvents() throws Exception {
//    User organizer = userRepository.save(organizer());
//    Event eventA = eventRepository.save(event(organizer, 100, false));
//    Event eventB = eventRepository.save(event(organizer, 100, false));
//    List<Participant> participants = seedParticipants(100);
//
//    ConcurrencyTestSupport.runConcurrently(
//        100,
//        index -> {
//          EventRegistrationCreateRequest request;
//          if (index % 2 == 0) {
//            request =
//                EventRegistrationCreateRequest.builder()
//                    .eventId(eventA.getId())
//                    .participantId(participants.get(index).getId())
//                    .build();
//          } else {
//            request =
//                EventRegistrationCreateRequest.builder()
//                    .eventId(eventB.getId())
//                    .participantId(participants.get(index).getId())
//                    .build();
//          }
//          return invokeCreate(request);
//        });
//
//    assertEquals(50L, countStatus(eventA.getId(), EventRegistrationStatus.ACCEPTED));
//    assertEquals(50L, countStatus(eventB.getId(), EventRegistrationStatus.ACCEPTED));
//  }
//
//  @Test
//  void shouldCancelCleanly_whenConcurrentRegistrationAndCancellation() throws Exception {
//    User organizer = userRepository.save(organizer());
//    Event event = eventRepository.save(event(organizer, 100, false));
//    User participantUser = userRepository.save(participantUser());
//    Participant participant = participantRepository.save(participant(participantUser));
//
//    EventRegistration seed =
//        registrationService.createEventRegistration(
//            EventRegistrationCreateRequest.builder()
//                .eventId(event.getId())
//                .participantId(participant.getId())
//                .build());
//
//    AtomicInteger cancelled = new AtomicInteger();
//    List<Integer> createResults =
//        ConcurrencyTestSupport.runConcurrently(
//            100,
//            index -> {
//              if (index == 0) {
//                registrationService.changeRegistrationRequestStatus(
//                    seed.getId(), EventRegistrationStatus.CANCELLED, "concurrent cancel");
//                cancelled.incrementAndGet();
//                return 1;
//              }
//              return invokeCreate(
//                  EventRegistrationCreateRequest.builder()
//                      .eventId(event.getId())
//                      .participantId(participant.getId())
//                      .build());
//            });
//
//    assertEquals(100, createResults.size());
//    assertEquals(1, cancelled.get(), "the cancellation call must have been performed");
//
//    long cancelledRows =
//        jdbcTemplate.queryForObject(
//            "SELECT COUNT(*) FROM event_registrations WHERE event_registration_status = ?",
//            Long.class,
//            EventRegistrationStatus.CANCELLED.name());
//    long activeRows =
//        jdbcTemplate.queryForObject(
//            "SELECT COUNT(*) FROM event_registrations WHERE event_registration_status <> ?",
//            Long.class,
//            EventRegistrationStatus.CANCELLED.name());
//    assertTrue(
//        cancelledRows > 0, "the concurrently cancelled registration should still be recorded");
//    assertTrue(
//        activeRows >= 1,
//        "concurrent registrations fired during the cancel must still be represented");
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
//                          .username("p" + System.nanoTime() + counter.incrementAndGet())
//                          .email("p" + System.nanoTime() + counter.incrementAndGet() + "@e.com")
//                          .password("password123")
//                          .role(com.eventreg.model.enums.RBAC.Role.PARTICIPANT)
//                          .build());
//              return participantRepository.save(participant(user));
//            })
//        .toList();
//  }
//
//  private int invokeCreate(EventRegistrationCreateRequest request) {
//    try {
//      registrationService.createEventRegistration(request);
//      return 1;
//    } catch (RuntimeException e) {
//      return 0;
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
