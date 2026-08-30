package com.eventreg.integration.load;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.eventreg.integration.concurrency.AbstractConcurrencyIntegrationTest;
import com.eventreg.model.Event;
import com.eventreg.model.Participant;
import com.eventreg.model.User;
import com.eventreg.repository.EventRepository;
import com.eventreg.repository.ParticipantRepository;
import com.eventreg.repository.UserRepository;
import com.eventreg.security.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base class for load/performance tests. Boots the full application context against a real
 * PostgreSQL (Testcontainers) through the shared {@link AbstractConcurrencyIntegrationTest}
 * container so {@link com.eventreg.service.implementation.EmailService} runs for real. Provides the
 * MockMvc entrypoint and deterministic timing helpers used by all load test scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class BaseLoadTest extends AbstractConcurrencyIntegrationTest {

  /** Hardcoded number of records used when seeding participants/events for the larger scenarios. */
  protected static final int SEED_COUNT = 1000;

  @Autowired protected WebApplicationContext context;

  @Autowired protected UserRepository userRepository;

  @Autowired protected ParticipantRepository participantRepository;

  @Autowired protected EventRepository eventRepository;

  @Autowired protected JwtService jwtService;

  @Autowired protected ObjectMapper objectMapper;

  @Autowired protected MockMvc mockMvc;

  protected String adminToken;

  /**
   * Wipes the persisted rows so every load test method starts from a clean, deterministic database.
   * Runs in the base class as the first step (before any subclass seeding) so tests are isolated
   * even though they share the single Testcontainers container.
   */
  @BeforeEach
  protected void resetDatabase() {
    cleanDatabase();
  }

  /**
   * Builds the MockMvc instance with Spring Security wired in and initializes an admin bearer token
   * used to authorize every request (admin bypasses all ownership checks and has every permission).
   */
  protected void initLoadTest() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    adminToken = bearerHeader(adminUser());
  }

  private User adminUser() {
    return userRepository
        .findByUsername("admin")
        .orElseGet(
            () -> {
              User admin =
                  User.builder()
                      .username("admin")
                      .email("admin@example.com")
                      .password("admin123")
                      .role(com.eventreg.model.enums.RBAC.Role.ADMIN)
                      .build();
              return userRepository.save(admin);
            });
  }

  protected String bearerHeader(User user) {
    return "Bearer " + jwtService.generateToken(user);
  }

  protected MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder) {
    return builder.header(HttpHeaders.AUTHORIZATION, adminToken);
  }

  /**
   * Runs {@code requestCount} requests, recording each duration in nanoseconds. Asserts that every
   * single request completes within {@code max} and returns the expected status.
   */
  protected List<Long> runRequests(
      int requestCount, ThrowingRequestHandler handler, Duration max, int expectedStatus)
      throws Exception {
    List<Long> durations = new ArrayList<>(requestCount);
    for (int i = 0; i < requestCount; i++) {
      long start = System.nanoTime();
      MvcResult result = handler.handle(i);
      long elapsedNanos = System.nanoTime() - start;
      assertStatus(result, expectedStatus);
      durations.add(elapsedNanos);
      assertUnder(elapsedNanos, max, "request " + i);
    }
    return durations;
  }

  /**
   * Runs {@code requestCount} requests concurrently via a fixed thread pool and asserts that the
   * total elapsed time for all requests is within {@code maxTotal}.
   */
  protected void runConcurrently(
      int requestCount, ThrowingRequestHandler handler, Duration maxTotal, int expectedStatus)
      throws Exception {
    java.util.concurrent.ExecutorService executor =
        java.util.concurrent.Executors.newFixedThreadPool(requestCount);
    java.util.concurrent.atomic.AtomicBoolean failure =
        new java.util.concurrent.atomic.AtomicBoolean();
    java.util.concurrent.CountDownLatch ready =
        new java.util.concurrent.CountDownLatch(requestCount);
    java.util.concurrent.CountDownLatch startGate = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.CountDownLatch done =
        new java.util.concurrent.CountDownLatch(requestCount);
    List<Throwable> errors = java.util.Collections.synchronizedList(new ArrayList<>());

    for (int i = 0; i < requestCount; i++) {
      final int index = i;
      executor.submit(
          () -> {
            ready.countDown();
            try {
              startGate.await();
              MvcResult result = handler.handle(index);
              if (result.getResponse().getStatus() != expectedStatus) {
                failure.set(true);
              }
            } catch (Throwable t) {
              failure.set(true);
              errors.add(t);
            } finally {
              done.countDown();
            }
          });
    }

    ready.await();
    long start = System.nanoTime();
    startGate.countDown();
    done.await();
    long elapsedTotal = System.nanoTime() - start;
    executor.shutdown();

    if (failure.get()) {
      throw new AssertionError(
          "concurrent request batch " + requestCount + " had failures: " + errors);
    }
    assertUnder(elapsedTotal, maxTotal, "concurrent request batch " + requestCount);
  }

  /** Asserts that a single measured duration is below the given maximum. */
  protected void assertUnder(long elapsedNanos, Duration max, String label) {
    long elapsedMillis = elapsedNanos / 1_000_000;
    if (elapsedMillis > max.toMillis()) {
      throw new AssertionError(
          label + " took " + elapsedMillis + "ms, exceeding " + max.toMillis() + "ms");
    }
  }

  /** Asserts that the average of the provided durations is below the given maximum. */
  protected void assertAverageUnder(List<Long> durations, Duration max, String label) {
    double avgMillis =
        durations.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
    if (avgMillis > max.toMillis()) {
      throw new AssertionError(
          label
              + " average took "
              + String.format("%.2f", avgMillis)
              + "ms, exceeding "
              + max.toMillis()
              + "ms");
    }
  }

  protected void assertStatus(MvcResult result, int expectedStatus) throws Exception {
    int actual = result.getResponse().getStatus();
    if (actual != expectedStatus) {
      throw new AssertionError(
          "expected HTTP "
              + expectedStatus
              + " but was "
              + actual
              + ": "
              + result.getResponse().getContentAsString());
    }
  }

  // ---------- shared seeding helpers (deterministic, no randomness) ----------

  protected User seedUser(int index, com.eventreg.model.enums.RBAC.Role role) {
    User user =
        User.builder()
            .username("user_" + index)
            .email("user_" + index + "@example.com")
            .password("password123")
            .role(role)
            .build();
    return userRepository.save(user);
  }

  protected Participant seedParticipant(int index, User user) {
    Participant participant =
        Participant.builder()
            .firstName("First" + index)
            .lastName("Last" + index)
            .age(25)
            .participantGender(com.eventreg.model.enums.ParticipantGender.MALE)
            .user(user)
            .build();
    return participantRepository.save(participant);
  }

  protected Event seedEvent(int index, User organizer) {
    Event event =
        Event.builder()
            .eventName("Event " + index)
            .eventDescription("Description " + index)
            .eventDate(java.time.OffsetDateTime.now().plusDays(30))
            .eventDuration(Duration.ofHours(2))
            .location("Hall " + (index % 10))
            .ageRequired(18)
            .eventGenderRequirement(com.eventreg.model.enums.EventGenderRequirement.NONE)
            .currentParticipantAmount(0)
            .currentWaitingQueueParticipantAmount(0)
            .maxParticipantAmount(100)
            .eventStatus(com.eventreg.model.enums.EventStatus.PLANNED)
            .eventReservationStatus(
                com.eventreg.model.enums.EventReservationStatus.RESERVATIONS_OPEN)
            .confirmationRequired(false)
            .waitlistWhenAllReserved(false)
            .organizer(organizer)
            .build();
    return eventRepository.save(event);
  }

  @FunctionalInterface
  protected interface ThrowingRequestHandler {
    MvcResult handle(int index) throws Exception;
  }
}
