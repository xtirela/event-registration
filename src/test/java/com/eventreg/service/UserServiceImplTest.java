package com.eventreg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventreg.dto.request.update.UserUpdateRequest;
import com.eventreg.exception.UserNotFoundException;
import com.eventreg.mapper.UserMapper;
import com.eventreg.model.User;
import com.eventreg.model.enums.RBAC.Role;
import com.eventreg.repository.UserRepository;
import com.eventreg.service.implementation.UserServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Unit tests for {@link UserServiceImpl}. The {@link UserRepository} is mocked and the {@link
 * UserMapper} is a real MapStruct instance.
 */
@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class UserServiceImplTest {

  @Mock private UserRepository userRepository;

  private UserMapper userMapper;

  private UserServiceImpl userService;

  @BeforeEach
  void setUp() {
    userMapper = Mappers.getMapper(UserMapper.class);
    userService = new UserServiceImpl(userRepository, userMapper);
  }

  private User user(long id) {
    return User.builder()
        .id(id)
        .username("olduser")
        .email("old@x.com")
        .password("password123")
        .role(Role.PARTICIPANT)
        .build();
  }

  private void stubSaveReturnsArgument() {
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  // ---------- updateUser ----------

  @Test
  void givenExistingUserWhenUpdateUserThenReturnsUpdatedUsername() {
    User existing = user(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    stubSaveReturnsArgument();
    UserUpdateRequest request = UserUpdateRequest.builder().username("newuser").build();

    User result = userService.updateUser(1L, request);

    assertThat(result.getUsername()).isEqualTo("newuser");
  }

  @Test
  void givenExistingUserWhenUpdateUserThenReturnsUpdatedEmail() {
    User existing = user(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    stubSaveReturnsArgument();
    UserUpdateRequest request = UserUpdateRequest.builder().email("new@x.com").build();

    User result = userService.updateUser(1L, request);

    assertThat(result.getEmail()).isEqualTo("new@x.com");
  }

  @Test
  void givenExistingUserWhenUpdateUserThenPersistsUser() {
    User existing = user(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
    stubSaveReturnsArgument();
    UserUpdateRequest request = UserUpdateRequest.builder().username("newuser").build();

    userService.updateUser(1L, request);

    verify(userRepository).save(any(User.class));
  }

  @Test
  void givenMissingUserWhenUpdateUserThenThrowsUserNotFoundException() {
    when(userRepository.findById(7L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        UserNotFoundException.class,
        () -> userService.updateUser(7L, UserUpdateRequest.builder().build()));
  }

  // ---------- deleteUser ----------

  @Test
  void givenExistingUserWhenDeleteUserThenDeletesUser() {
    User existing = user(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

    userService.deleteUser(1L);

    verify(userRepository).delete(existing);
  }

  @Test
  void givenMissingUserWhenDeleteUserThenThrowsUserNotFoundException() {
    when(userRepository.findById(7L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        UserNotFoundException.class, () -> userService.deleteUser(7L));
  }

  // ---------- findById ----------

  @Test
  void givenExistingUserWhenFindByIdThenReturnsUser() {
    User existing = user(1L);
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

    User result = userService.findById(1L);

    assertThat(result.getId()).isEqualTo(1L);
  }

  @Test
  void givenMissingUserWhenFindByIdThenThrowsUserNotFoundException() {
    when(userRepository.findById(7L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        UserNotFoundException.class, () -> userService.findById(7L));
  }

  // ---------- findAll ----------

  @Test
  void givenUsersWhenFindAllWithSpecThenReturnsPageFromRepository() {
    User user = user(1L);
    when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(user)));

    Page<User> result = userService.findAll(mock(Specification.class), Pageable.ofSize(10));

    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  void givenUsersWhenFindAllThenReturnsPageFromRepository() {
    User user = user(1L);
    when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));

    Page<User> result = userService.findAll(Pageable.ofSize(10));

    assertThat(result.getTotalElements()).isEqualTo(1);
  }
}
