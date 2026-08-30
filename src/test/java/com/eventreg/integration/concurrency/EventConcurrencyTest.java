// package com.eventreg.integration.concurrency;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertThrows;
//
// import com.eventreg.dto.request.create.EventCreateRequest;
// import com.eventreg.exception.EventNotFoundException;
// import com.eventreg.model.Event;
// import com.eventreg.model.User;
// import com.eventreg.model.enums.EventGenderRequirement;
// import com.eventreg.repository.EventRepository;
// import com.eventreg.repository.UserRepository;
// import com.eventreg.service.EventService;
// import java.time.Duration;
// import java.time.OffsetDateTime;
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
/// ** Concurrency tests for event creation and concurrent delete + access against real PostgreSQL.
// */
// @Testcontainers
// public class EventConcurrencyTest extends AbstractConcurrencyIntegrationTest {
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
//  @Autowired private EventService eventService;
//  @Autowired private EventRepository eventRepository;
//  @Autowired private UserRepository userRepository;
//
//  @BeforeEach
//  void cleanUp() {
//    cleanDatabase();
//  }
//
//  @Test
//  void shouldCreateHundredDistinctEvents_whenHundredConcurrentCreateRequests() throws Exception {
//    User organizer = userRepository.save(organizer());
//    AtomicInteger counter = new AtomicInteger();
//
//    List<Long> createdIds =
//        ConcurrencyTestSupport.runConcurrently(
//            1000,
//            index -> {
//              int seq = counter.incrementAndGet();
//              Event event =
//                  eventService.createEvent(
//                      EventCreateRequest.builder()
//                          .eventName("Concurrent event " + seq)
//                          .eventDate(OffsetDateTime.now().plusDays(30))
//                          .eventDuration(Duration.ofHours(2))
//                          .location("Test Hall")
//                          .ageRequired(18)
//                          .eventGenderRequirement(EventGenderRequirement.NONE)
//                          .maxParticipantAmount(1000)
//                          .organizerId(organizer.getId())
//                          .build());
//              return event.getId();
//            });
//
//    assertEquals(1000, createdIds.size());
//    long persisted = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events", Long.class);
//    assertEquals(
//        1000L, persisted, "all 1000 concurrent create requests must produce distinct events");
//  }
//
//  @Test
//  void shouldLeaveNoGhostEvent_whenConcurrentDeleteAndAccessOnSameEvent() throws Exception {
//    User organizer = userRepository.save(organizer());
//    Event event = eventRepository.save(event(organizer, 1000, false));
//
//    List<Integer> results =
//        ConcurrencyTestSupport.runConcurrently(
//            1000,
//            index -> {
//              try {
//                if (index == 0) {
//                  eventService.deleteEvent(event.getId());
//                  return 1;
//                }
//                eventService.findById(event.getId());
//                return 1;
//              } catch (EventNotFoundException e) {
//                return 0;
//              }
//            });
//
//    assertEquals(1000, results.size(), "all concurrent accesses must terminate without
// corruption");
//
//    assertThrows(
//        EventNotFoundException.class,
//        () -> eventService.findById(event.getId()),
//        "after the concurrent delete the event must no longer exist");
//  }
// }
