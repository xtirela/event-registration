package com.eventreg.controller.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventreg.exception.DuplicateException;
import com.eventreg.security.controller.AuthController;
import com.eventreg.security.dto.request.LoginRequest;
import com.eventreg.security.dto.request.RegisterRequest;
import com.eventreg.security.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Unit tests for {@link AuthController}. */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerUnitTest extends BaseControllerUnitTest {

  @Autowired MockMvc mockMvc;

  @Autowired ObjectMapper objectMapper;

  @MockitoBean private AuthService authService;

  @Test
  void login_shouldReturnOkStatus() throws Exception {
    LoginRequest request = new LoginRequest();
    request.setUsername("ivanov");
    request.setPassword("password123");
    when(authService.login(any(LoginRequest.class))).thenReturn("jwt-token");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void login_shouldReturnTokenInBody() throws Exception {
    LoginRequest request = new LoginRequest();
    request.setUsername("ivanov");
    request.setPassword("password123");
    when(authService.login(any(LoginRequest.class))).thenReturn("jwt-token");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(jsonPath("$.token").value("jwt-token"));
  }

  @Test
  void login_shouldReturnBadRequest_whenBodyMalformed() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_shouldReturnOkStatus() throws Exception {
    RegisterRequest request =
        RegisterRequest.builder()
            .username("ivanov")
            .email("ivan@example.com")
            .password("password123")
            .build();
    when(authService.register(any(RegisterRequest.class))).thenReturn("jwt-token");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void register_shouldReturnTokenInBody() throws Exception {
    RegisterRequest request =
        RegisterRequest.builder()
            .username("ivanov")
            .email("ivan@example.com")
            .password("password123")
            .build();
    when(authService.register(any(RegisterRequest.class))).thenReturn("jwt-token");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(jsonPath("$.token").value("jwt-token"));
  }

  @Test
  void register_shouldReturnConflict_whenServiceThrows() throws Exception {
    RegisterRequest request =
        RegisterRequest.builder()
            .username("ivanov")
            .email("ivan@example.com")
            .password("password123")
            .build();
    when(authService.register(any(RegisterRequest.class)))
        .thenThrow(new DuplicateException("username already exists", "register"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void register_shouldReturnBadRequest_whenUsernameBlank() throws Exception {
    RegisterRequest request =
        RegisterRequest.builder()
            .username(" ")
            .email("ivan@example.com")
            .password("password123")
            .build();

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_shouldReturnBadRequest_whenUsernameTooShort() throws Exception {
    RegisterRequest request =
        RegisterRequest.builder()
            .username("ab")
            .email("ivan@example.com")
            .password("password123")
            .build();

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_shouldReturnBadRequest_whenUsernameTooLong() throws Exception {
    RegisterRequest request =
        RegisterRequest.builder()
            .username("a".repeat(51))
            .email("ivan@example.com")
            .password("password123")
            .build();

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_shouldReturnBadRequest_whenEmailInvalid() throws Exception {
    RegisterRequest request =
        RegisterRequest.builder()
            .username("ivanov")
            .email("not-an-email")
            .password("password123")
            .build();

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_shouldReturnBadRequest_whenPasswordTooShort() throws Exception {
    RegisterRequest request =
        RegisterRequest.builder()
            .username("ivanov")
            .email("ivan@example.com")
            .password("1234567")
            .build();

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_shouldAcceptBoundaryUsernameLength() throws Exception {
    RegisterRequest request =
        RegisterRequest.builder()
            .username("abc")
            .email("ivan@example.com")
            .password("password123")
            .build();
    when(authService.register(any(RegisterRequest.class))).thenReturn("jwt-token");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }
}
