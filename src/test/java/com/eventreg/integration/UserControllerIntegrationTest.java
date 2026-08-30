package com.eventreg.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventreg.controller.UserController;
import com.eventreg.dto.request.update.UserUpdateRequest;
import com.eventreg.model.User;
import com.eventreg.model.enums.RBAC.Role;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Integration tests for {@link UserController}. */
@Testcontainers
public class UserControllerIntegrationTest extends BaseControllerIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private User admin() {
    return createUser("admin2", Role.ADMIN);
  }

  // ---------- findAll ----------

  @Test
  void shouldReturnAllUsers_WhenAdminRequestsUserList() throws Exception {
    createUser("alice", Role.PARTICIPANT);
    createUser("bob", Role.PARTICIPANT);
    User admin = admin();

    mockMvc
        .perform(as(get("/api/users"), admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(4));
  }

  @Test
  void shouldReturnSecondPage_WhenRequestingPageOne() throws Exception {
    createUser("alice", Role.PARTICIPANT);
    createUser("bob", Role.PARTICIPANT);
    createUser("charlie", Role.PARTICIPANT);
    User admin = admin();

    mockMvc
        .perform(as(get("/api/users?page=1&size=1"), admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(1))
        .andExpect(jsonPath("$.numberOfElements").value(1));
  }

  @Test
  void shouldReturnUsersSortedByUsername_WhenSortingAscending() throws Exception {
    createUser("alice", Role.PARTICIPANT);
    createUser("bob", Role.PARTICIPANT);
    User admin = admin();

    mockMvc
        .perform(as(get("/api/users?sort=username,asc"), admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].username").value("admin"));
  }

  @Test
  void shouldReturnForbidden_WhenParticipantRequestsUserList() throws Exception {
    User participant = createUser("alice", Role.PARTICIPANT);

    mockMvc.perform(as(get("/api/users"), participant)).andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenOrganiserRequestsUserList() throws Exception {
    User organiser = createUser("bob", Role.ORGANISER);

    mockMvc.perform(as(get("/api/users"), organiser)).andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenAnonymousRequestsUserList() throws Exception {
    mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
  }

  // ---------- findById ----------

  @Test
  void shouldReturnOwnUser_WhenParticipantRequestsOwnProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);

    mockMvc
        .perform(as(get("/api/users/{id}", alice.getId()), alice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("alice"));
  }

  @Test
  void shouldReturnOwnUser_WhenOrganiserRequestsOwnProfile() throws Exception {
    User bob = createUser("bob", Role.ORGANISER);

    mockMvc
        .perform(as(get("/api/users/{id}", bob.getId()), bob))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("bob"));
  }

  @Test
  void shouldReturnForbidden_WhenParticipantRequestsAnotherUsersProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User bob = createUser("bob", Role.PARTICIPANT);

    mockMvc
        .perform(as(get("/api/users/{id}", bob.getId()), alice))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenOrganiserRequestsAnotherUsersProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User bob = createUser("bob", Role.ORGANISER);

    mockMvc
        .perform(as(get("/api/users/{id}", alice.getId()), bob))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnUser_WhenAdminRequestsAnotherUsersProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User admin = admin();

    mockMvc
        .perform(as(get("/api/users/{id}", alice.getId()), admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("alice"));
  }

  @Test
  void shouldReturnNotFound_WhenAdminRequestsNonexistentUser() throws Exception {
    User admin = admin();

    mockMvc.perform(as(get("/api/users/99999"), admin)).andExpect(status().isNotFound());
  }

  // ---------- updateUser ----------

  @Test
  void shouldReturnUpdatedEmail_WhenAdminUpdatesAnotherUser() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User admin = admin();
    UserUpdateRequest update = UserUpdateRequest.builder().email("new.e-mail@example.com").build();

    mockMvc
        .perform(as(patchJson("/api/users/{id}", alice.getId(), update), admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("new.e-mail@example.com"));
  }

  @Test
  void shouldReturnUpdatedUsername_WhenAdminUpdatesAnotherUser() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User admin = admin();
    UserUpdateRequest update = UserUpdateRequest.builder().username("alice2").build();

    mockMvc
        .perform(as(patchJson("/api/users/{id}", alice.getId(), update), admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("alice2"));
  }

  @Test
  void shouldReturnUpdatedPassword_WhenAdminUpdatesAnotherUser() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User admin = admin();
    UserUpdateRequest update = UserUpdateRequest.builder().password("newpassword123").build();

    mockMvc
        .perform(as(patchJson("/api/users/{id}", alice.getId(), update), admin))
        .andExpect(status().isOk());
  }

  @Test
  void shouldStorePasswordInPlainText_WhenAdminUpdatesAnotherUser() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User admin = admin();
    UserUpdateRequest update = UserUpdateRequest.builder().password("newpassword123").build();

    mockMvc
        .perform(as(patchJson("/api/users/{id}", alice.getId(), update), admin))
        .andExpect(status().isOk());

    String storedPassword =
        jdbcTemplate.queryForObject(
            "SELECT password FROM users WHERE id = ?", String.class, alice.getId());
    assertEquals("newpassword123", storedPassword);
  }

  @Test
  void shouldReturnForbidden_WhenParticipantUpdatesOwnProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    UserUpdateRequest update = UserUpdateRequest.builder().email("new@example.com").build();

    mockMvc
        .perform(as(patchJson("/api/users/{id}", alice.getId(), update), alice))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenOrganiserUpdatesOwnProfile() throws Exception {
    User bob = createUser("bob", Role.ORGANISER);
    UserUpdateRequest update = UserUpdateRequest.builder().email("new@example.com").build();

    mockMvc
        .perform(as(patchJson("/api/users/{id}", bob.getId(), update), bob))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnNotFound_WhenAdminUpdatesNonexistentUser() throws Exception {
    User admin = admin();
    UserUpdateRequest update = UserUpdateRequest.builder().email("new@example.com").build();

    mockMvc
        .perform(as(patchJson("/api/users/99999", update), admin))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnConflict_WhenAdminSetsEmailAlreadyInUse() throws Exception {
    createUserWithEmail("bob", "used@example.com", Role.PARTICIPANT);
    User alice = createUserWithEmail("alice", "alice@example.com", Role.PARTICIPANT);
    User admin = admin();
    UserUpdateRequest update = UserUpdateRequest.builder().email("used@example.com").build();

    mockMvc
        .perform(as(patchJson("/api/users/{id}", alice.getId(), update), admin))
        .andExpect(status().isConflict());
  }

  @Test
  void shouldReturnConflict_WhenAdminSetsUsernameAlreadyInUse() throws Exception {
    createUser("bob", Role.PARTICIPANT);
    User alice = createUser("alice", Role.PARTICIPANT);
    User admin = admin();
    UserUpdateRequest update = UserUpdateRequest.builder().username("bob").build();

    mockMvc
        .perform(as(patchJson("/api/users/{id}", alice.getId(), update), admin))
        .andExpect(status().isConflict());
  }

  // ---------- deleteUser ----------

  @Test
  void shouldReturnNoContent_WhenAdminDeletesUserWithoutParticipantProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User admin = admin();

    mockMvc
        .perform(as(delete("/api/users/{id}", alice.getId()), admin))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnNoContent_WhenAdminDeletesUserHavingParticipantProfile() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    createParticipant(alice);
    User admin = admin();

    mockMvc
        .perform(as(delete("/api/users/{id}", alice.getId()), admin))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnForbidden_WhenParticipantDeletesAnotherUser() throws Exception {
    User alice = createUser("alice", Role.PARTICIPANT);
    User bob = createUser("bob", Role.PARTICIPANT);

    mockMvc
        .perform(as(delete("/api/users/{id}", bob.getId()), alice))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenOrganiserDeletesOwnUser() throws Exception {
    User bob = createUser("bob", Role.ORGANISER);

    mockMvc
        .perform(as(delete("/api/users/{id}", bob.getId()), bob))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnNotFound_WhenAdminDeletesNonexistentUser() throws Exception {
    User admin = admin();

    mockMvc.perform(as(delete("/api/users/99999"), admin)).andExpect(status().isNotFound());
  }
}
