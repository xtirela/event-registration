// package com.eventreg.integration.concurrency;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static org.junit.jupiter.api.Assertions.assertTrue;
//
// import com.eventreg.model.Event;
// import com.eventreg.model.Participant;
// import com.eventreg.model.User;
// import com.eventreg.model.enums.RBAC.Role;
// import com.eventreg.repository.EventRepository;
// import com.eventreg.repository.ParticipantRepository;
// import com.eventreg.repository.UserRepository;
// import com.eventreg.security.SecurityGuard;
// import java.util.List;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.authority.SimpleGrantedAuthority;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.test.context.DynamicPropertyRegistry;
// import org.springframework.test.context.DynamicPropertySource;
// import org.testcontainers.containers.PostgreSQLContainer;
// import org.testcontainers.junit.jupiter.Container;
// import org.testcontainers.junit.jupiter.Testcontainers;
//
/// ** Concurrency tests for {@link SecurityGuard} decision methods against a real PostgreSQL. */
// @Testcontainers
// public class SecurityGuardConcurrencyTest extends AbstractConcurrencyIntegrationTest {
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
//  @Autowired private SecurityGuard securityGuard;
//  @Autowired private UserRepository userRepository;
//  @Autowired private ParticipantRepository participantRepository;
//  @Autowired private EventRepository eventRepository;
//
//  @BeforeEach
//  void cleanUp() {
//    cleanDatabase();
//  }
//
//  @Test
//  void shouldReturnConsistentOwnerDecisions_whenHundredConcurrentGuardCalls() throws Exception {
//    User organizer = userRepository.save(organizer());
//    Event event = eventRepository.save(event(organizer, 100, false));
//
//    List<Boolean> results =
//        ConcurrencyTestSupport.runConcurrently(
//            100,
//            index ->
//                as(organizer.getUsername(), () -> securityGuard.isEventOrganizer(event.getId())));
//
//    assertEquals(100, results.size());
//    assertTrue(
//        results.stream().allMatch(Boolean::booleanValue),
//        "every concurrent guard decision for the event owner must be true");
//  }
//
//  @Test
//  void shouldRejectNonOwner_whenConcurrentGuardCallsWithForeignUser() throws Exception {
//    User organizer = userRepository.save(organizer());
//    User other = userRepository.save(participantUser());
//    Event event = eventRepository.save(event(organizer, 100, false));
//
//    List<Boolean> results =
//        ConcurrencyTestSupport.runConcurrently(
//            100,
//            index -> as(other.getUsername(), () ->
// securityGuard.isEventOrganizer(event.getId())));
//
//    assertTrue(
//        results.stream().noneMatch(Boolean::booleanValue),
//        "a user who is not the organizer must never be granted organizer rights");
//  }
//
//  @Test
//  void shouldCheckOwnershipStably_whenConcurrentGuardCallsOnParticipant() throws Exception {
//    User owner = userRepository.save(participantUser());
//    Participant participant = participantRepository.save(participant(owner));
//
//    List<Boolean> results =
//        ConcurrencyTestSupport.runConcurrently(
//            100,
//            index ->
//                as(
//                    owner.getUsername(),
//                    () -> securityGuard.isParticipantOwner(participant.getId())));
//
//    assertEquals(100, results.size());
//    assertFalse(
//        results.stream().anyMatch(b -> !b),
//        "the participant owner must be recognized on every concurrent call");
//  }
//
//  private <T> T as(String username, java.util.function.Supplier<T> supplier) {
//    UsernamePasswordAuthenticationToken authentication =
//        new UsernamePasswordAuthenticationToken(
//            username, null, List.of(new SimpleGrantedAuthority(Role.ADMIN.name())));
//    SecurityContextHolder.getContext().setAuthentication(authentication);
//    try {
//      return supplier.get();
//    } finally {
//      SecurityContextHolder.clearContext();
//    }
//  }
// }
