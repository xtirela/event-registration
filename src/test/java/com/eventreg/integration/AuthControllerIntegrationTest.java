package com.eventreg.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventreg.model.enums.RBAC.Role;
import com.eventreg.security.dto.request.LoginRequest;
import com.eventreg.security.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Integration tests for {@link com.eventreg.security.controller.AuthController}. */
@Testcontainers
public class AuthControllerIntegrationTest extends BaseControllerIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private RegisterRequest registerRequest(String username, String password) {
    return RegisterRequest.builder()
        .username(username)
        .email(username + "@example.com")
        .password(password)
        .isOrganizer(false)
        .build();
  }

  @Test
  void shouldReturnJwtToken_WhenRegisteringValidParticipant() throws Exception {
    RegisterRequest request = registerRequest("alice", "password123");

    mockMvc
        .perform(json(post("/api/auth/register"), request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void shouldReturnJwtToken_WhenRegisteringValidOrganizer() throws Exception {
    RegisterRequest request =
        RegisterRequest.builder()
            .username("organizer")
            .email("organizer@example.com")
            .password("password123")
            .isOrganizer(true)
            .build();

    mockMvc
        .perform(json(post("/api/auth/register"), request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void shouldReturnJwtToken_WhenLoginWithValidCredentials() throws Exception {
    createUser("alice", Role.PARTICIPANT);
    LoginRequest login = new LoginRequest();
    login.setUsername("alice");
    login.setPassword("password123");

    mockMvc
        .perform(json(post("/api/auth/login"), login))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void shouldAccessProtectedEndpoint_WhenCarryingTokenFromLogin() throws Exception {
    createUser("alice", Role.PARTICIPANT);
    LoginRequest login = new LoginRequest();
    login.setUsername("alice");
    login.setPassword("password123");

    String token =
        objectMapper
            .readTree(
                mockMvc
                    .perform(json(post("/api/auth/login"), login))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("token")
            .asText();

    mockMvc
        .perform(get("/api/events").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void shouldReturnForbidden_WhenAccessingProtectedEndpointWithoutAuthentication()
      throws Exception {
    mockMvc.perform(get("/api/events")).andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnBadRequest_WhenPasswordShorterThanEightCharacters() throws Exception {
    RegisterRequest request = registerRequest("alice", "passwor");

    mockMvc.perform(json(post("/api/auth/register"), request)).andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenUsernameShorterThanThreeCharacters() throws Exception {
    RegisterRequest request = registerRequest("al", "password123");

    mockMvc.perform(json(post("/api/auth/register"), request)).andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenUsernameLongerThanFiftyCharacters() throws Exception {
    RegisterRequest request = registerRequest("a".repeat(51), "password123");

    mockMvc.perform(json(post("/api/auth/register"), request)).andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenEmailIsMalformed() throws Exception {
    RegisterRequest request = registerRequest("alice", "password123");
    request.setEmail("not-an-email");

    mockMvc.perform(json(post("/api/auth/register"), request)).andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenUsernameIsBlank() throws Exception {
    RegisterRequest request = registerRequest(" ", "password123");

    mockMvc.perform(json(post("/api/auth/register"), request)).andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenPasswordIsBlank() throws Exception {
    RegisterRequest request = registerRequest("alice", " ");

    mockMvc.perform(json(post("/api/auth/register"), request)).andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenEmailIsBlank() throws Exception {
    RegisterRequest request = registerRequest("alice", "password123");
    request.setEmail(" ");

    mockMvc.perform(json(post("/api/auth/register"), request)).andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnOk_WhenPasswordIsExactlyEightCharacters() throws Exception {
    RegisterRequest request = registerRequest("alice", "password");

    mockMvc.perform(json(post("/api/auth/register"), request)).andExpect(status().isOk());
  }

  @Test
  void shouldReturnOk_WhenUsernameIsExactlyThreeCharacters() throws Exception {
    RegisterRequest request = registerRequest("ali", "password123");

    mockMvc.perform(json(post("/api/auth/register"), request)).andExpect(status().isOk());
  }

  @Test
  void shouldReturnOk_WhenUsernameIsExactlyFiftyCharacters() throws Exception {
    RegisterRequest request = registerRequest("a".repeat(50), "password123");

    mockMvc.perform(json(post("/api/auth/register"), request)).andExpect(status().isOk());
  }

  @Test
  void shouldReturnConflict_WhenEmailIsAlreadyRegistered() throws Exception {
    createUserWithEmail("alice", "shared@example.com", Role.PARTICIPANT);
    RegisterRequest request = registerRequest("alice2", "password123");
    request.setEmail("shared@example.com");

    mockMvc.perform(json(post("/api/auth/register"), request)).andExpect(status().isConflict());
  }

  @Test
  void shouldReturnConflict_WhenUsernameIsAlreadyRegistered() throws Exception {
    createUser("alice", Role.PARTICIPANT);
    RegisterRequest request = registerRequest("alice", "password123");

    mockMvc.perform(json(post("/api/auth/register"), request)).andExpect(status().isConflict());
  }

  @Test
  void shouldReturnNotFound_WhenLoginWithUnknownUsername() throws Exception {
    LoginRequest login = new LoginRequest();
    login.setUsername("ghost");
    login.setPassword("password123");

    mockMvc.perform(json(post("/api/auth/login"), login)).andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnUnauthorized_WhenLoginWithWrongPassword() throws Exception {
    createUser("alice", Role.PARTICIPANT);
    LoginRequest login = new LoginRequest();
    login.setUsername("alice");
    login.setPassword("wrong-password");

    mockMvc.perform(json(post("/api/auth/login"), login)).andExpect(status().isUnauthorized());
  }
}
