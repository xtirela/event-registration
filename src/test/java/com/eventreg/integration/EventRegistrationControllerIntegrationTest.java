package com.eventreg.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventreg.controller.EventRegistrationController;
import com.eventreg.dto.request.create.EventRegistrationCreateRequest;
import com.eventreg.dto.request.update.EventRegistrationStatusUpdateRequest;
import com.eventreg.model.Event;
import com.eventreg.model.EventRegistration;
import com.eventreg.model.Participant;
import com.eventreg.model.User;
import com.eventreg.model.enums.EventRegistrationStatus;
import com.eventreg.model.enums.EventStatus;
import com.eventreg.model.enums.RBAC.Role;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Integration tests for {@link EventRegistrationController}. */
@Testcontainers
public class EventRegistrationControllerIntegrationTest extends BaseControllerIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private EventRegistrationCreateRequest registrationRequest(Participant participant, Event event) {
    return EventRegistrationCreateRequest.builder()
        .participantId(participant.getId())
        .eventId(event.getId())
        .build();
  }

  private User admin() {
    return createUser("admin2", Role.ADMIN);
  }

  // ---------- createEventRegistration ----------

  @Test
  void shouldReturnAcceptedRegistration_WhenEventHasFreeSeats() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);

    mockMvc
        .perform(as(postJson("/api/registrations", registrationRequest(participant, event)), alice))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.eventRegistrationStatus").value("ACCEPTED"))
        .andExpect(jsonPath("$.eventId").value(event.getId()))
        .andExpect(jsonPath("$.participantId").value(participant.getId()));
  }

  @Test
  void shouldReturnPendingRegistration_WhenConfirmationIsRequired() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, true);
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);

    mockMvc
        .perform(as(postJson("/api/registrations", registrationRequest(participant, event)), alice))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.eventRegistrationStatus").value("PENDING"));
  }

  @Test
  void shouldReturnWaitingRegistration_WhenEventIsOnWaitlist() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 2, true, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    User bob = createUser("bob", Role.PARTICIPANT);
    User carol = createUser("carol", Role.PARTICIPANT);
    createRegistration(event, createParticipant(alice), EventRegistrationStatus.ACCEPTED);
    createRegistration(event, createParticipant(bob), EventRegistrationStatus.ACCEPTED);
    Participant carolParticipant = createParticipant(carol);

    mockMvc
        .perform(
            as(postJson("/api/registrations", registrationRequest(carolParticipant, event)), carol))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.eventRegistrationStatus").value("WAITING"));
  }

  @Test
  void shouldReturnDeniedRegistration_WhenEventIsFull() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 1, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    User bob = createUser("bob", Role.PARTICIPANT);
    createRegistration(event, createParticipant(alice), EventRegistrationStatus.ACCEPTED);
    Participant bobParticipant = createParticipant(bob);

    mockMvc
        .perform(
            as(postJson("/api/registrations", registrationRequest(bobParticipant, event)), bob))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.eventRegistrationStatus").value("DENIED"));
  }

  @Test
  void shouldReturnDeniedRegistration_WhenEventIsCancelled() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    event.setEventStatus(EventStatus.CANCELLED);
    eventRepository.save(event);
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);

    mockMvc
        .perform(as(postJson("/api/registrations", registrationRequest(participant, event)), alice))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.eventRegistrationStatus").value("DENIED"));
  }

  @Test
  void shouldReturnAcceptedRegistration_WhenParticipantAddedDirectly() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, true);
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);

    mockMvc
        .perform(
            as(
                postJson("/api/registrations/direct", registrationRequest(participant, event)),
                organiser))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.eventRegistrationStatus").value("ACCEPTED"));
  }

  @Test
  void shouldReturnForbidden_WhenParticipantRegistersAnotherParticipantsProfile() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    User bob = createUser("bob", Role.PARTICIPANT);
    Participant bobParticipant = createParticipant(bob);

    mockMvc
        .perform(
            as(postJson("/api/registrations", registrationRequest(bobParticipant, event)), alice))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenAnonymousRegistersForEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);

    mockMvc
        .perform(postJson("/api/registrations", registrationRequest(participant, event)))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenParticipantAddsAnotherDirectly() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, true);
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);

    mockMvc
        .perform(
            as(
                postJson("/api/registrations/direct", registrationRequest(participant, event)),
                alice))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnNotFound_WhenEventDoesNotExist() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);
    EventRegistrationCreateRequest request =
        EventRegistrationCreateRequest.builder()
            .participantId(participant.getId())
            .eventId(99999L)
            .build();

    mockMvc
        .perform(as(postJson("/api/registrations", request), alice))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnNotFound_WhenParticipantDoesNotExist() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    EventRegistrationCreateRequest request =
        EventRegistrationCreateRequest.builder()
            .participantId(99999L)
            .eventId(event.getId())
            .build();

    mockMvc
        .perform(as(postJson("/api/registrations", request), alice))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnBadRequest_WhenParticipantIdIsMissing() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    EventRegistrationCreateRequest request =
        EventRegistrationCreateRequest.builder().eventId(event.getId()).build();

    mockMvc
        .perform(as(postJson("/api/registrations", request), admin()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenEventIdIsMissing() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);
    EventRegistrationCreateRequest request =
        EventRegistrationCreateRequest.builder().participantId(participant.getId()).build();

    mockMvc
        .perform(as(postJson("/api/registrations", request), alice))
        .andExpect(status().isBadRequest());
  }

  // ---------- findAll ----------

  @Test
  void shouldReturnAllRegistrations_WhenAuthenticated() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    User bob = createUser("bob", Role.PARTICIPANT);
    createRegistration(event, createParticipant(alice), EventRegistrationStatus.ACCEPTED);
    createRegistration(event, createParticipant(bob), EventRegistrationStatus.ACCEPTED);

    mockMvc
        .perform(as(get("/api/registrations"), admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void shouldReturnRegistrationsSortedById_WhenSortingById() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    User bob = createUser("bob", Role.PARTICIPANT);
    createRegistration(event, createParticipant(alice), EventRegistrationStatus.ACCEPTED);
    createRegistration(event, createParticipant(bob), EventRegistrationStatus.ACCEPTED);

    mockMvc
        .perform(as(get("/api/registrations?sort=id,asc"), admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void shouldReturnForbidden_WhenAnonymousListsRegistrations() throws Exception {
    mockMvc.perform(get("/api/registrations")).andExpect(status().isForbidden());
  }

  // ---------- findById ----------

  @Test
  void shouldReturnRegistration_WhenAuthenticatedRequestsById() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);
    EventRegistration registration =
        createRegistration(event, participant, EventRegistrationStatus.ACCEPTED);

    mockMvc
        .perform(as(get("/api/registrations/{id}", registration.getId()), alice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventRegistrationStatus").value("ACCEPTED"))
        .andExpect(jsonPath("$.eventId").value(event.getId()))
        .andExpect(jsonPath("$.participantId").value(participant.getId()));
  }

  @Test
  void shouldReturnNotFound_WhenRegistrationDoesNotExist() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);

    mockMvc.perform(as(get("/api/registrations/99999"), alice)).andExpect(status().isNotFound());
  }

  // ---------- acceptRegistration ----------

  @Test
  void shouldMoveToAccepted_WhenOrganiserAcceptsRegistration() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, true);
    User alice = createUser("alice", Role.PARTICIPANT);
    EventRegistration registration =
        createRegistration(event, createParticipant(alice), EventRegistrationStatus.PENDING);

    mockMvc
        .perform(as(patch("/api/registrations/{id}/accept", registration.getId()), organiser))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventRegistrationStatus").value("ACCEPTED"));
  }

  @Test
  void shouldMoveToDenied_WhenOrganiserDeniesRegistration() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, true);
    User alice = createUser("alice", Role.PARTICIPANT);
    EventRegistration registration =
        createRegistration(event, createParticipant(alice), EventRegistrationStatus.PENDING);

    mockMvc
        .perform(as(patch("/api/registrations/{id}/deny", registration.getId()), organiser))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventRegistrationStatus").value("DENIED"));
  }

  @Test
  void shouldNotMoveToDenied_WhenOrganiserDeniesOtherOrganisersRegistration() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, true);

    User organiser2 = createUser("organiser2", Role.ORGANISER);
    Event event2 = createEvent("Tech Conference2", organiser2, 10, false, true);

    User alice = createUser("alice", Role.PARTICIPANT);
    EventRegistration registration =
        createRegistration(event, createParticipant(alice), EventRegistrationStatus.PENDING);

    mockMvc
        .perform(as(patch("/api/registrations/{id}/deny", registration.getId()), organiser2))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldNotMoveToCancelled_WhenOrganiserCancelsRegistration() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    EventRegistration registration =
        createRegistration(event, createParticipant(alice), EventRegistrationStatus.ACCEPTED);

    mockMvc
        .perform(as(patch("/api/registrations/{id}/cancel", registration.getId()), organiser))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenAnotherOrganiserAcceptsRegistration() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, true);
    User alice = createUser("alice", Role.PARTICIPANT);
    EventRegistration registration =
        createRegistration(event, createParticipant(alice), EventRegistrationStatus.PENDING);
    User anotherOrganiser = createUser("organiser2", Role.ORGANISER);

    mockMvc
        .perform(
            as(patch("/api/registrations/{id}/accept", registration.getId()), anotherOrganiser))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenParticipantAcceptsRegistration() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, true);
    User alice = createUser("alice", Role.PARTICIPANT);
    EventRegistration registration =
        createRegistration(event, createParticipant(alice), EventRegistrationStatus.PENDING);

    mockMvc
        .perform(as(patch("/api/registrations/{id}/accept", registration.getId()), alice))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldMoveToCancelled_WhenParticipantCancelsOwnRegistration() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    EventRegistration registration =
        createRegistration(event, createParticipant(alice), EventRegistrationStatus.ACCEPTED);

    mockMvc
        .perform(as(patch("/api/registrations/{id}/cancel", registration.getId()), alice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventRegistrationStatus").value("CANCELLED"));
  }

  @Test
  void shouldReturnNotFound_WhenAcceptingNonexistentRegistration() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);

    mockMvc
        .perform(as(patch("/api/registrations/99999/accept"), organiser))
        .andExpect(status().isNotFound());
  }

  // ---------- changeStatus ----------

  @Test
  void shouldReturnUpdatedStatus_WhenAdminChangesRegistrationStatus() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    EventRegistration registration =
        createRegistration(event, createParticipant(alice), EventRegistrationStatus.ACCEPTED);
    User admin = admin();
    EventRegistrationStatusUpdateRequest request =
        EventRegistrationStatusUpdateRequest.builder()
            .eventRegistrationStatus(EventRegistrationStatus.CANCELLED)
            .description("admin decision")
            .build();

    mockMvc
        .perform(
            as(patchJson("/api/registrations/{id}/status", registration.getId(), request), admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventRegistrationStatus").value("CANCELLED"))
        .andExpect(jsonPath("$.description").value("admin decision"));
  }

  @Test
  void shouldReturnBadRequest_WhenStatusIsMissing() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    EventRegistration registration =
        createRegistration(event, createParticipant(alice), EventRegistrationStatus.ACCEPTED);
    User admin = admin();
    EventRegistrationStatusUpdateRequest request =
        EventRegistrationStatusUpdateRequest.builder().build();

    mockMvc
        .perform(
            as(patchJson("/api/registrations/{id}/status", registration.getId(), request), admin))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnForbidden_WhenParticipantChangesRegistrationStatus() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    EventRegistration registration =
        createRegistration(event, createParticipant(alice), EventRegistrationStatus.ACCEPTED);
    EventRegistrationStatusUpdateRequest request =
        EventRegistrationStatusUpdateRequest.builder()
            .eventRegistrationStatus(EventRegistrationStatus.CANCELLED)
            .build();

    mockMvc
        .perform(
            as(patchJson("/api/registrations/{id}/status", registration.getId(), request), alice))
        .andExpect(status().isForbidden());
  }

  // ---------- deleteEventRegistration ----------

  @Test
  void shouldReturnNoContent_WhenAdminDeletesRegistration() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    EventRegistration registration =
        createRegistration(event, createParticipant(alice), EventRegistrationStatus.ACCEPTED);
    User admin = admin();

    mockMvc
        .perform(as(delete("/api/registrations/{id}", registration.getId()), admin))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnForbidden_WhenParticipantDeletesRegistration() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    EventRegistration registration =
        createRegistration(event, createParticipant(alice), EventRegistrationStatus.ACCEPTED);

    mockMvc
        .perform(as(delete("/api/registrations/{id}", registration.getId()), alice))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenOrganiserDeletesRegistration() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, false, false);
    User alice = createUser("alice", Role.PARTICIPANT);
    EventRegistration registration =
        createRegistration(event, createParticipant(alice), EventRegistrationStatus.ACCEPTED);

    mockMvc
        .perform(as(delete("/api/registrations/{id}", registration.getId()), organiser))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnNotFound_WhenAdminDeletesNonexistentRegistration() throws Exception {
    User admin = admin();

    mockMvc.perform(as(delete("/api/registrations/99999"), admin)).andExpect(status().isNotFound());
  }
}
