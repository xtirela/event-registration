package com.eventreg.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.eventreg.model.Event;
import com.eventreg.model.EventRegistration;
import com.eventreg.model.User;
import com.eventreg.model.enums.EventRegistrationStatus;
import com.eventreg.model.enums.RBAC.Role;
import com.eventreg.service.WaitingQueueScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for the waiting queue promotion performed by {@link
 * WaitingQueueScheduler#updateAllWaitingQueues()}.
 *
 * <p>Because the test bootstrap disables {@code @EnableScheduling}, the scheduler is invoked
 * deterministically and synchronously in each test.
 */
@Testcontainers
public class WaitingQueueIntegrationTest extends BaseControllerIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired WaitingQueueScheduler waitingQueueScheduler;

  private EventRegistration findRegistration(Long id) {
    return eventRegistrationRepository.findById(id).orElseThrow();
  }

  private EventRegistration register(Event event, String username, EventRegistrationStatus status) {
    User user = createUser(username, Role.PARTICIPANT);
    return createRegistration(event, createParticipant(user), status);
  }

  @Test
  void shouldPromoteWaitingParticipant_WhenSpotFreesUp() throws Exception {
    Event event = createEvent("Tech Conference", createUser("org", Role.ORGANISER), 2, true, false);
    EventRegistration accepted = register(event, "alice", EventRegistrationStatus.ACCEPTED);
    EventRegistration waiting = register(event, "bob", EventRegistrationStatus.WAITING);
    eventRegistrationRepository.delete(accepted);

    waitingQueueScheduler.updateAllWaitingQueues();

    assertEquals(
        EventRegistrationStatus.ACCEPTED,
        findRegistration(waiting.getId()).getEventRegistrationStatus());
  }

  @Test
  void shouldLeaveWaitingParticipant_WhenNoSpotFreesUp() throws Exception {
    Event event = createEvent("Tech Conference", createUser("org", Role.ORGANISER), 2, true, false);
    EventRegistration first = register(event, "alice", EventRegistrationStatus.ACCEPTED);
    EventRegistration second = register(event, "bob", EventRegistrationStatus.ACCEPTED);
    EventRegistration waiting = register(event, "carol", EventRegistrationStatus.WAITING);

    waitingQueueScheduler.updateAllWaitingQueues();

    assertEquals(
        EventRegistrationStatus.WAITING,
        findRegistration(waiting.getId()).getEventRegistrationStatus());
    assertEquals(
        EventRegistrationStatus.ACCEPTED,
        findRegistration(second.getId()).getEventRegistrationStatus());
    assertEquals(
        EventRegistrationStatus.ACCEPTED,
        findRegistration(first.getId()).getEventRegistrationStatus());
  }

  @Test
  void shouldPromoteToPending_WhenConfirmationIsRequired() throws Exception {
    Event event = createEvent("Tech Conference", createUser("org", Role.ORGANISER), 2, true, true);
    EventRegistration accepted = register(event, "alice", EventRegistrationStatus.ACCEPTED);
    EventRegistration waiting = register(event, "bob", EventRegistrationStatus.WAITING);
    eventRegistrationRepository.delete(accepted);

    waitingQueueScheduler.updateAllWaitingQueues();

    assertEquals(
        EventRegistrationStatus.PENDING,
        findRegistration(waiting.getId()).getEventRegistrationStatus());
  }

  @Test
  void shouldLeavePendingRegistration_AfterSchedulerRun() throws Exception {
    Event event = createEvent("Tech Conference", createUser("org", Role.ORGANISER), 2, true, false);
    register(event, "alice", EventRegistrationStatus.ACCEPTED);
    EventRegistration pending = register(event, "bob", EventRegistrationStatus.PENDING);

    waitingQueueScheduler.updateAllWaitingQueues();

    assertEquals(
        EventRegistrationStatus.PENDING,
        findRegistration(pending.getId()).getEventRegistrationStatus());
  }

  @Test
  void shouldLeaveDeniedRegistration_AfterSchedulerRun() throws Exception {
    Event event = createEvent("Tech Conference", createUser("org", Role.ORGANISER), 2, true, false);
    register(event, "alice", EventRegistrationStatus.ACCEPTED);
    EventRegistration denied = register(event, "bob", EventRegistrationStatus.DENIED);

    waitingQueueScheduler.updateAllWaitingQueues();

    assertEquals(
        EventRegistrationStatus.DENIED,
        findRegistration(denied.getId()).getEventRegistrationStatus());
  }
}
