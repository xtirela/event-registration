package com.eventreg.controller.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventreg.controller.UserController;
import com.eventreg.dto.request.update.UserUpdateRequest;
import com.eventreg.dto.response.UserResponse;
import com.eventreg.exception.UserNotFoundException;
import com.eventreg.mapper.UserMapper;
import com.eventreg.model.User;
import com.eventreg.model.enums.RBAC.Role;
import com.eventreg.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Unit tests for {@link UserController}. */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerUnitTest extends BaseControllerUnitTest {

  @Autowired MockMvc mockMvc;

  @Autowired ObjectMapper objectMapper;

  @MockitoBean private UserMapper userMapper;
  @MockitoBean private UserService userService;

  private UserResponse userResponse() {
    return UserResponse.builder()
        .id(1L)
        .username("ivanov")
        .email("ivan@example.com")
        .role(Role.PARTICIPANT)
        .participantId(2L)
        .build();
  }

  private User user() {
    return User.builder()
        .id(1L)
        .username("ivanov")
        .email("ivan@example.com")
        .password("password123")
        .role(Role.PARTICIPANT)
        .build();
  }

  @Test
  void findAllUsers_shouldReturnOkStatus() throws Exception {
    when(userService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

    mockMvc.perform(get("/api/users")).andExpect(status().isOk());
  }

  @Test
  void findUserById_shouldReturnOkStatus() throws Exception {
    when(userService.findById(1L)).thenReturn(user());
    when(userMapper.userToUserResponse(any(User.class))).thenReturn(userResponse());

    mockMvc.perform(get("/api/users/1")).andExpect(status().isOk());
  }

  @Test
  void findUserById_shouldReturnUsernameInBody() throws Exception {
    when(userService.findById(1L)).thenReturn(user());
    when(userMapper.userToUserResponse(any(User.class))).thenReturn(userResponse());

    mockMvc.perform(get("/api/users/1")).andExpect(jsonPath("$.username").value("ivanov"));
  }

  @Test
  void findUserById_shouldReturnNotFound_whenServiceThrows() throws Exception {
    when(userService.findById(999L)).thenThrow(new UserNotFoundException(999L, "findById"));

    mockMvc.perform(get("/api/users/999")).andExpect(status().isNotFound());
  }

  @Test
  void updateUser_shouldReturnOkStatus() throws Exception {
    UserUpdateRequest request =
        UserUpdateRequest.builder().username("ivanov").email("ivan@example.com").build();
    when(userService.updateUser(eq(1L), any(UserUpdateRequest.class))).thenReturn(user());
    when(userMapper.userToUserResponse(any(User.class))).thenReturn(userResponse());

    mockMvc
        .perform(
            patch("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void updateUser_shouldReturnUsernameInBody() throws Exception {
    User updated = user();
    updated.setUsername("ivanov_new");
    UserResponse updatedResponse =
        UserResponse.builder()
            .id(1L)
            .username("ivanov_new")
            .email("ivan@example.com")
            .role(Role.PARTICIPANT)
            .build();
    when(userService.updateUser(eq(1L), any(UserUpdateRequest.class))).thenReturn(updated);
    when(userMapper.userToUserResponse(any(User.class))).thenReturn(updatedResponse);

    UserUpdateRequest request =
        UserUpdateRequest.builder().username("ivanov").email("ivan@example.com").build();
    mockMvc
        .perform(
            patch("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(jsonPath("$.username").value("ivanov_new"));
  }

  @Test
  void updateUser_shouldReturnNotFound_whenServiceThrows() throws Exception {
    UserUpdateRequest request =
        UserUpdateRequest.builder().username("ivanov").email("ivan@example.com").build();
    when(userService.updateUser(eq(999L), any(UserUpdateRequest.class)))
        .thenThrow(new UserNotFoundException(999L, "updateUser"));

    mockMvc
        .perform(
            patch("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteUser_shouldReturnNoContent() throws Exception {
    mockMvc.perform(delete("/api/users/1")).andExpect(status().isNoContent());
  }

  @Test
  void deleteUser_shouldReturnNotFound_whenServiceThrows() throws Exception {
    doThrow(new UserNotFoundException(999L, "deleteUser")).when(userService).deleteUser(999L);

    mockMvc.perform(delete("/api/users/999")).andExpect(status().isNotFound());
  }
}
