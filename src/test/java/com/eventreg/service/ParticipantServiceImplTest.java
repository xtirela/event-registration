package com.eventreg.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventreg.dto.request.create.ParticipantCreateRequest;
import com.eventreg.dto.request.update.ParticipantUpdateRequest;
import com.eventreg.exception.ParticipantNotFoundException;
import com.eventreg.exception.UserNotFoundException;
import com.eventreg.mapper.ParticipantMapper;
import com.eventreg.model.Participant;
import com.eventreg.model.User;
import com.eventreg.model.enums.ParticipantGender;
import com.eventreg.repository.ParticipantRepository;
import com.eventreg.service.implementation.ParticipantServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Unit tests for {@link ParticipantServiceImpl}. Repository and collaborating {@link UserService}
 * are mocked; the {@link ParticipantMapper} is a real MapStruct instance.
 */
@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ParticipantServiceImplTest {

  @Mock private ParticipantRepository participantRepository;

  @Mock private UserService userService;

  private ParticipantMapper participantMapper;

  private ParticipantServiceImpl participantService;

  @BeforeEach
  void setUp() {
    participantMapper = Mappers.getMapper(ParticipantMapper.class);
    participantService =
        new ParticipantServiceImpl(participantRepository, userService, participantMapper);
  }

  private ParticipantCreateRequest sampleCreateRequest(Long userId) {
    return ParticipantCreateRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .age(25)
        .participantGender(ParticipantGender.MALE)
        .userId(userId)
        .build();
  }

  private Participant participant(long id) {
    return Participant.builder()
        .id(id)
        .firstName("John")
        .lastName("Doe")
        .age(25)
        .participantGender(ParticipantGender.MALE)
        .user(User.builder().id(1L).username("u").email("u@x.com").password("password123").build())
        .build();
  }

  private void stubSaveReturnsArgument() {
    when(participantRepository.save(any(Participant.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  // ---------- createParticipant ----------

  @Test
  void givenValidCreateRequestWhenCreateParticipantThenReturnsParticipantWithRequestedFirstName() {
    User user =
        User.builder().id(1L).username("u").email("u@x.com").password("password123").build();
    when(userService.findById(1L)).thenReturn(user);
    stubSaveReturnsArgument();

    Participant result = participantService.createParticipant(sampleCreateRequest(1L));

    assertThat(result.getFirstName()).isEqualTo("John");
  }

  @Test
  void givenValidCreateRequestWhenCreateParticipantThenSetsUserFetchedFromUserService() {
    User user =
        User.builder().id(1L).username("u").email("u@x.com").password("password123").build();
    when(userService.findById(1L)).thenReturn(user);
    stubSaveReturnsArgument();

    Participant result = participantService.createParticipant(sampleCreateRequest(1L));

    assertThat(result.getUser()).isEqualTo(user);
  }

  @Test
  void givenValidCreateRequestWhenCreateParticipantThenPersistsParticipant() {
    User user =
        User.builder().id(1L).username("u").email("u@x.com").password("password123").build();
    when(userService.findById(1L)).thenReturn(user);
    stubSaveReturnsArgument();

    participantService.createParticipant(sampleCreateRequest(1L));

    verify(participantRepository).save(any(Participant.class));
  }

  @Test
  void givenMissingUserWhenCreateParticipantThenThrowsUserNotFoundException() {
    when(userService.findById(9L)).thenThrow(new UserNotFoundException(9L, "findById"));

    org.junit.jupiter.api.Assertions.assertThrows(
        UserNotFoundException.class,
        () -> participantService.createParticipant(sampleCreateRequest(9L)));
  }

  // ---------- updateParticipant ----------

  @Test
  void givenExistingParticipantWhenUpdateParticipantThenReturnsUpdatedLastName() {
    Participant existing = participant(1L);
    when(participantRepository.findById(1L)).thenReturn(Optional.of(existing));
    stubSaveReturnsArgument();
    ParticipantUpdateRequest request = ParticipantUpdateRequest.builder().lastName("Smith").build();

    Participant result = participantService.updateParticipant(1L, request);

    assertThat(result.getLastName()).isEqualTo("Smith");
  }

  @Test
  void givenExistingParticipantWhenUpdateParticipantThenPersistsParticipant() {
    Participant existing = participant(1L);
    when(participantRepository.findById(1L)).thenReturn(Optional.of(existing));
    stubSaveReturnsArgument();
    ParticipantUpdateRequest request = ParticipantUpdateRequest.builder().lastName("Smith").build();

    participantService.updateParticipant(1L, request);

    verify(participantRepository).save(any(Participant.class));
  }

  @Test
  void givenMissingParticipantWhenUpdateParticipantThenThrowsParticipantNotFoundException() {
    when(participantRepository.findById(7L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        ParticipantNotFoundException.class,
        () -> participantService.updateParticipant(7L, ParticipantUpdateRequest.builder().build()));
  }

  // ---------- deleteParticipant ----------

  @Test
  void givenExistingParticipantWhenDeleteParticipantThenDeletesParticipant() {
    Participant existing = participant(1L);
    when(participantRepository.findById(1L)).thenReturn(Optional.of(existing));

    participantService.deleteParticipant(1L);

    verify(participantRepository).delete(existing);
  }

  @Test
  void givenMissingParticipantWhenDeleteParticipantThenThrowsParticipantNotFoundException() {
    when(participantRepository.findById(7L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        ParticipantNotFoundException.class, () -> participantService.deleteParticipant(7L));
  }

  // ---------- findById ----------

  @Test
  void givenExistingParticipantWhenFindByIdThenReturnsParticipant() {
    Participant existing = participant(1L);
    when(participantRepository.findById(1L)).thenReturn(Optional.of(existing));

    Participant result = participantService.findById(1L);

    assertThat(result.getId()).isEqualTo(1L);
  }

  @Test
  void givenMissingParticipantWhenFindByIdThenThrowsParticipantNotFoundException() {
    when(participantRepository.findById(7L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        ParticipantNotFoundException.class, () -> participantService.findById(7L));
  }

  // ---------- findAll ----------

  @Test
  void givenParticipantsWhenFindAllWithSpecThenReturnsPageFromRepository() {
    Participant participant = participant(1L);
    when(participantRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(participant)));

    Page<Participant> result =
        participantService.findAll(mock(Specification.class), Pageable.ofSize(10));

    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  void givenParticipantsWhenFindAllThenReturnsPageFromRepository() {
    Participant participant = participant(1L);
    when(participantRepository.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(participant)));

    Page<Participant> result = participantService.findAll(Pageable.ofSize(10));

    assertThat(result.getTotalElements()).isEqualTo(1);
  }
}
