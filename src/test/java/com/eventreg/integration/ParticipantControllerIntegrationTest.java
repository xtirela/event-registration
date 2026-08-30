package com.eventreg.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventreg.controller.ParticipantController;
import com.eventreg.dto.request.create.ParticipantCreateRequest;
import com.eventreg.dto.request.update.ParticipantUpdateRequest;
import com.eventreg.model.Participant;
import com.eventreg.model.User;
import com.eventreg.model.enums.ParticipantGender;
import com.eventreg.model.enums.RBAC.Role;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Integration tests for {@link ParticipantController}. */
@Testcontainers
public class ParticipantControllerIntegrationTest extends BaseControllerIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private ParticipantCreateRequest participantRequest(User user) {
    return ParticipantCreateRequest.builder()
        .firstName("Ivan")
        .lastName("Ivanov")
        .age(25)
        .participantGender(ParticipantGender.MALE)
        .userId(user.getId())
        .build();
  }

  private User admin() {
    return createUser("admin2", Role.ADMIN);
  }

  // ---------- createParticipant ----------

  @Test
  void shouldReturnCreatedParticipant_WhenParticipantCreatesOwnProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);

    mockMvc
        .perform(as(postJson("/api/participants", participantRequest(alice)), alice))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.firstName").value("Ivan"))
        .andExpect(jsonPath("$.lastName").value("Ivanov"))
        .andExpect(jsonPath("$.age").value(25))
        .andExpect(jsonPath("$.userId").value(alice.getId()));
  }

  @Test
  void shouldReturnCreatedParticipant_WhenAdminCreatesProfileForAnotherUser() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User admin = admin();

    mockMvc
        .perform(as(postJson("/api/participants", participantRequest(alice)), admin))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(alice.getId()));
  }

  @Test
  void shouldReturnCreated_WhenOrganiserCreatesOwnProfile() throws Exception {
    User bob = createUser("bob", Role.ORGANISER);

    mockMvc
        .perform(as(postJson("/api/participants", participantRequest(bob)), bob))
        .andExpect(status().isCreated());
  }

  @Test
  void shouldReturnForbidden_WhenParticipantCreatesProfileForAnotherUser() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User bob = createUser("bob", Role.PARTICIPANT);

    mockMvc
        .perform(as(postJson("/api/participants", participantRequest(bob)), alice))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenAnonymousCreatesParticipantProfile() throws Exception {
    User bob = createUser("bob", Role.PARTICIPANT);

    mockMvc
        .perform(postJson("/api/participants", participantRequest(bob)))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnConflict_WhenUserAlreadyHasParticipantProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    createParticipant(alice);

    mockMvc
        .perform(as(postJson("/api/participants", participantRequest(alice)), alice))
        .andExpect(status().isConflict());
  }

  @Test
  void shouldReturnBadRequest_WhenFirstNameIsBlank() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName(" ")
            .lastName("Ivanov")
            .age(25)
            .participantGender(ParticipantGender.MALE)
            .userId(alice.getId())
            .build();

    mockMvc
        .perform(as(postJson("/api/participants", request), alice))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenLastNameLongerThanOneHundredCharacters() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName("Ivan")
            .lastName("a".repeat(101))
            .age(25)
            .participantGender(ParticipantGender.MALE)
            .userId(alice.getId())
            .build();

    mockMvc
        .perform(as(postJson("/api/participants", request), alice))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnCreated_WhenLastNameIsExactlyOneHundredCharacters() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName("Ivan")
            .lastName("a".repeat(100))
            .age(25)
            .participantGender(ParticipantGender.MALE)
            .userId(alice.getId())
            .build();

    mockMvc
        .perform(as(postJson("/api/participants", request), alice))
        .andExpect(status().isCreated());
  }

  @Test
  void shouldReturnBadRequest_WhenAgeIsNegative() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName("Ivan")
            .lastName("Ivanov")
            .age(-1)
            .participantGender(ParticipantGender.MALE)
            .userId(alice.getId())
            .build();

    mockMvc
        .perform(as(postJson("/api/participants", request), alice))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenAgeIsOverOneHundredFifty() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName("Ivan")
            .lastName("Ivanov")
            .age(151)
            .participantGender(ParticipantGender.MALE)
            .userId(alice.getId())
            .build();

    mockMvc
        .perform(as(postJson("/api/participants", request), alice))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnCreated_WhenAgeIsOne() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName("Ivan")
            .lastName("Ivanov")
            .age(1)
            .participantGender(ParticipantGender.MALE)
            .userId(alice.getId())
            .build();

    mockMvc
        .perform(as(postJson("/api/participants", request), alice))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.age").value(1));
  }

  @Test
  void shouldReturnCreated_WhenAgeIsOneHundredFifty() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName("Ivan")
            .lastName("Ivanov")
            .age(150)
            .participantGender(ParticipantGender.MALE)
            .userId(alice.getId())
            .build();

    mockMvc
        .perform(as(postJson("/api/participants", request), alice))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.age").value(150));
  }

  @Test
  void shouldReturnBadRequest_WhenGenderIsMissing() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName("Ivan")
            .lastName("Ivanov")
            .age(25)
            .userId(alice.getId())
            .build();

    mockMvc
        .perform(as(postJson("/api/participants", request), alice))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenUserIdIsMissing() throws Exception {
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName("Ivan")
            .lastName("Ivanov")
            .age(25)
            .participantGender(ParticipantGender.MALE)
            .build();

    mockMvc
        .perform(as(postJson("/api/participants", request), admin()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnNotFound_WhenReferencedUserDoesNotExist() throws Exception {
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName("Ivan")
            .lastName("Ivanov")
            .age(25)
            .participantGender(ParticipantGender.MALE)
            .userId(99999L)
            .build();

    mockMvc
        .perform(as(postJson("/api/participants", request), admin()))
        .andExpect(status().isNotFound());
  }

  // ---------- findAll ----------

  @Test
  void shouldReturnAllParticipants_WhenAuthenticated() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User bob = createUser("bob", Role.PARTICIPANT);
    createParticipant(alice);
    createParticipant(bob);

    mockMvc
        .perform(as(get("/api/participants"), alice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void shouldReturnFirstPage_WhenRequestingParticipantPage() throws Exception {
    createParticipant(createUser("alice", Role.PARTICIPANT));
    createParticipant(createUser("bob", Role.PARTICIPANT));
    createParticipant(createUser("charlie", Role.PARTICIPANT));
    User admin = admin();

    mockMvc
        .perform(as(get("/api/participants?page=0&size=2"), admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.numberOfElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2));
  }

  @Test
  void shouldReturnForbidden_WhenAnonymousRequestsParticipants() throws Exception {
    mockMvc.perform(get("/api/participants")).andExpect(status().isForbidden());
  }

  // ---------- findById ----------

  @Test
  void shouldReturnParticipant_WhenAuthenticatedRequestsById() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);

    mockMvc
        .perform(as(get("/api/participants/{id}", participant.getId()), alice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("FirstName"));
  }

  @Test
  void shouldReturnNotFound_WhenParticipantDoesNotExist() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);

    mockMvc.perform(as(get("/api/participants/99999"), alice)).andExpect(status().isNotFound());
  }

  // ---------- updateParticipant ----------

  @Test
  void shouldReturnUpdatedFirstName_WhenOwnerUpdatesOwnProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);
    ParticipantUpdateRequest update = ParticipantUpdateRequest.builder().firstName("Anna").build();

    mockMvc
        .perform(as(patchJson("/api/participants/{id}", participant.getId(), update), alice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Anna"));
  }

  @Test
  void shouldReturnUpdatedLastName_WhenOwnerUpdatesOwnProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);
    ParticipantUpdateRequest update =
        ParticipantUpdateRequest.builder().lastName("Smirnova").build();

    mockMvc
        .perform(as(patchJson("/api/participants/{id}", participant.getId(), update), alice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lastName").value("Smirnova"));
  }

  @Test
  void shouldReturnUpdatedAge_WhenOwnerUpdatesOwnProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);
    ParticipantUpdateRequest update = ParticipantUpdateRequest.builder().age(30).build();

    mockMvc
        .perform(as(patchJson("/api/participants/{id}", participant.getId(), update), alice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.age").value(30));
  }

  @Test
  void shouldReturnUpdatedGender_WhenOwnerUpdatesOwnProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);
    ParticipantUpdateRequest update =
        ParticipantUpdateRequest.builder().participantGender(ParticipantGender.FEMALE).build();

    mockMvc
        .perform(as(patchJson("/api/participants/{id}", participant.getId(), update), alice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participantGender").value("FEMALE"));
  }

  @Test
  void shouldReturnForbidden_WhenAnotherParticipantUpdatesProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User bob = createUser("bob", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);
    ParticipantUpdateRequest update = ParticipantUpdateRequest.builder().firstName("Anna").build();

    mockMvc
        .perform(as(patchJson("/api/participants/{id}", participant.getId(), update), bob))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenOrganiserUpdatesParticipantOfAnotherUser() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User organiser = createUser("bob", Role.ORGANISER);
    Participant participant = createParticipant(alice);
    ParticipantUpdateRequest update = ParticipantUpdateRequest.builder().firstName("Anna").build();

    mockMvc
        .perform(as(patchJson("/api/participants/{id}", participant.getId(), update), organiser))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnOk_WhenAdminUpdatesParticipantOfAnotherUser() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User admin = admin();
    Participant participant = createParticipant(alice);
    ParticipantUpdateRequest update = ParticipantUpdateRequest.builder().firstName("Anna").build();

    mockMvc
        .perform(as(patchJson("/api/participants/{id}", participant.getId(), update), admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Anna"));
  }

  @Test
  void shouldReturnNotFound_WhenUpdatingNonexistentProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    ParticipantUpdateRequest update = ParticipantUpdateRequest.builder().firstName("Anna").build();

    mockMvc
        .perform(as(patchJson("/api/participants/99999", update), alice))
        .andExpect(status().isNotFound());
  }

  // ---------- deleteParticipant ----------

  @Test
  void shouldReturnNoContent_WhenOwnerDeletesOwnProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);

    mockMvc
        .perform(as(delete("/api/participants/{id}", participant.getId()), alice))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnNoContent_WhenAdminDeletesAnotherUsersProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);
    User admin = admin();

    mockMvc
        .perform(as(delete("/api/participants/{id}", participant.getId()), admin))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnForbidden_WhenAnotherParticipantDeletesProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);
    User bob = createUser("bob", Role.PARTICIPANT);

    mockMvc
        .perform(as(delete("/api/participants/{id}", participant.getId()), bob))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenOrganiserDeletesParticipantOfAnotherUser() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    Participant participant = createParticipant(alice);
    User organiser = createUser("bob", Role.ORGANISER);

    mockMvc
        .perform(as(delete("/api/participants/{id}", participant.getId()), organiser))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnNotFound_WhenDeletingNonexistentProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);

    mockMvc.perform(as(delete("/api/participants/99999"), alice)).andExpect(status().isNotFound());
  }
}
