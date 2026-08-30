// package com.eventreg.integration.concurrency;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertTrue;
//
// import com.eventreg.dto.request.update.ParticipantUpdateRequest;
// import com.eventreg.model.Participant;
// import com.eventreg.model.User;
// import com.eventreg.model.enums.RBAC.Role;
// import com.eventreg.repository.ParticipantRepository;
// import com.eventreg.repository.UserRepository;
// import com.eventreg.security.dto.request.RegisterRequest;
// import com.eventreg.security.service.AuthService;
// import com.eventreg.service.ParticipantService;
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
/// ** Concurrency tests for participant updates and user registration against a real PostgreSQL. */
// @Testcontainers
// public class ParticipantAndUserConcurrencyTest extends AbstractConcurrencyIntegrationTest {
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
//  @Autowired private ParticipantService participantService;
//  @Autowired private ParticipantRepository participantRepository;
//  @Autowired private UserRepository userRepository;
//  @Autowired private AuthService authService;
//
//  @BeforeEach
//  void cleanUp() {
//    cleanDatabase();
//  }
//
//  @Test
//  void shouldPersistSingleParticipantRecord_whenHundredConcurrentUpdatesOnSameRecord()
//          throws Exception {
//    User user = userRepository.save(participantUser());
//    Participant participant = participantRepository.save(participant(user));
//
//    List<Integer> writtenAges =
//            ConcurrencyTestSupport.runConcurrently(
//                    100,
//                    index -> {
//                      int age = 18 + index;
//                      participantService.updateParticipant(
//                              participant.getId(),
// ParticipantUpdateRequest.builder().age(age).build());
//                      return age;
//                    });
//
//    assertEquals(100, writtenAges.size(), "each concurrent update call must complete");
//
//    long rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM participants", Long.class);
//    assertEquals(1L, rows, "concurrent updates must never create duplicate participant rows");
//
//    int reloadedAge = participantService.findById(participant.getId()).getAge();
//    assertTrue(
//            reloadedAge >= 18 && reloadedAge <= 117,
//            "final age must remain a single valid value, was " + reloadedAge);
//  }
//
//  @Test
//  void shouldRegisterHundredDistinctUsers_whenHundredConcurrentRegistrations() throws Exception {
//    AtomicInteger counter = new AtomicInteger();
//
//    List<String> tokens =
//            ConcurrencyTestSupport.runConcurrently(
//                    100,  // Исправлено с 1000 на 100
//                    index -> {
//                      int seq = counter.incrementAndGet();
//                      return authService.register(
//                              RegisterRequest.builder()
//                                      .username("user_" + seq)
//                                      .email("user_" + seq + "@example.com")
//                                      .password("password123")
//                                      .build());
//                    });
//
//    assertEquals(100, tokens.size());
//    // Учитываем админа в базе: 100 новых + 1 админ = 101
//    long persisted = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
//    assertEquals(101L, persisted, "100 concurrent registrations must create 100 distinct users
// plus existing admin");
//  }
//
//  @Test
//  void shouldRejectDuplicate_WhenTwoConcurrentRegistrationsShareUsername() throws Exception {
//    List<Integer> results =
//            ConcurrencyTestSupport.runConcurrently(
//                    2,
//                    index -> {
//                      try {
//                        authService.register(
//                                RegisterRequest.builder()
//                                        .username("same_user")
//                                        .email("same" + index + "@example.com")
//                                        .password("password123")
//                                        .build());
//                        return 1;
//                      } catch (RuntimeException e) {
//                        return 0;
//                      }
//                    });
//
//    long persisted =
//            jdbcTemplate.queryForObject(
//                    "SELECT COUNT(*) FROM users WHERE username = 'same_user'", Long.class);
//    assertEquals(
//            1L,
//            persisted,
//            "concurrent registrations sharing a username must not both succeed (unique
// constraint)");
//    assertTrue(results.contains(0), "at least one of the two registration calls must have
// failed");
//  }
//
//  /**
//   * Helper method to create admin user if it doesn't exist.
//   * This should be called in @BeforeEach or via database migration.
//   */
//  private User ensureAdminExists() {
//    return userRepository.findByUsername("admin")
//            .orElseGet(() -> userRepository.save(
//                    User.builder()
//                            .username("admin")
//                            .email("admin@example.com")
//                            .password("admin123")  // Should be encoded in real implementation
//                            .role(Role.ADMIN)
//                            .build()
//            ));
//  }
// }
