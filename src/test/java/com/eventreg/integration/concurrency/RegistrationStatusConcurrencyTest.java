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
// import java.util.HashSet;
// import java.util.Set;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.test.context.DynamicPropertyRegistry;
// import org.springframework.test.context.DynamicPropertySource;
// import org.testcontainers.containers.PostgreSQLContainer;
// import org.testcontainers.junit.jupiter.Container;
// import org.testcontainers.junit.jupiter.Testcontainers;
//
/// ** Concurrency tests for {@link EventRegistrationService#changeRegistrationRequestStatus}. */
// @Testcontainers
// public class RegistrationStatusConcurrencyTest extends AbstractConcurrencyIntegrationTest {
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
//  private static final Set<String> VALID_STATUSES =
//      Set.of(
//          EventRegistrationStatus.ACCEPTED.name(),
//          EventRegistrationStatus.DENIED.name(),
//          EventRegistrationStatus.CANCELLED.name(),
//          EventRegistrationStatus.PENDING.name());
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
//  void shouldKeepSingleRow_whenHundredConcurrentStatusChangesOnSameRegistration() throws Exception
// {
//    User organizer = userRepository.save(organizer());
//    User participantUser = userRepository.save(participantUser());
//    Participant participant = participantRepository.save(participant(participantUser));
//    Event event = eventRepository.save(event(organizer, 100, false));
//
//    Set<EventRegistrationStatus> statuses = new HashSet<>(VALID_STATUSES.size());
//    statuses.add(EventRegistrationStatus.ACCEPTED);
//    statuses.add(EventRegistrationStatus.DENIED);
//    statuses.add(EventRegistrationStatus.CANCELLED);
//    statuses.add(EventRegistrationStatus.PENDING);
//
//    EventRegistration registration =
//        registrationService.createEventRegistration(
//            EventRegistrationCreateRequest.builder()
//                .eventId(event.getId())
//                .participantId(participant.getId())
//                .build());
//
//    ConcurrencyTestSupport.runConcurrently(
//        100,
//        index -> {
//          EventRegistrationStatus status =
//              new java.util.ArrayList<>(statuses).get(index % statuses.size());
//          registrationService.changeRegistrationRequestStatus(
//              registration.getId(), status, "concurrent status change");
//          return status;
//        });
//
//    long rows =
//        jdbcTemplate.queryForObject(
//            "SELECT COUNT(*) FROM event_registrations WHERE id = ?",
//            Long.class,
//            registration.getId());
//    assertEquals(1L, rows, "concurrent status changes must not create or drop registration rows");
//
//    String finalStatus =
//        jdbcTemplate.queryForObject(
//            "SELECT event_registration_status FROM event_registrations WHERE id = ?",
//            String.class,
//            registration.getId());
//    assertTrue(
//        VALID_STATUSES.contains(finalStatus),
//        "final status must be one of the statuses written concurrently, was " + finalStatus);
//  }
// }
