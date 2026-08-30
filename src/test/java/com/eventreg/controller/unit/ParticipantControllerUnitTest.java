package com.eventreg.controller.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventreg.controller.ParticipantController;
import com.eventreg.dto.request.create.ParticipantCreateRequest;
import com.eventreg.dto.request.update.ParticipantUpdateRequest;
import com.eventreg.dto.response.ParticipantResponse;
import com.eventreg.exception.ParticipantNotFoundException;
import com.eventreg.mapper.ParticipantMapper;
import com.eventreg.model.Participant;
import com.eventreg.model.User;
import com.eventreg.model.enums.ParticipantGender;
import com.eventreg.service.ParticipantService;
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

/** Unit tests for {@link ParticipantController}. */
@WebMvcTest(ParticipantController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ParticipantControllerUnitTest extends BaseControllerUnitTest {

  @Autowired MockMvc mockMvc;

  @Autowired ObjectMapper objectMapper;

  @MockitoBean private ParticipantService participantService;
  @MockitoBean private ParticipantMapper participantMapper;

  private ParticipantCreateRequest validCreateRequest() {
    return ParticipantCreateRequest.builder()
        .firstName("Ivan")
        .lastName("Ivanov")
        .age(25)
        .participantGender(ParticipantGender.MALE)
        .userId(1L)
        .build();
  }

  private Participant participant() {
    User user = User.builder().id(1L).username("ivan").build();
    return Participant.builder()
        .id(1L)
        .firstName("Ivan")
        .lastName("Ivanov")
        .age(25)
        .participantGender(ParticipantGender.MALE)
        .user(user)
        .build();
  }

  private ParticipantResponse participantResponse() {
    return ParticipantResponse.builder()
        .id(1L)
        .firstName("Ivan")
        .lastName("Ivanov")
        .age(25)
        .participantGender(ParticipantGender.MALE)
        .userId(1L)
        .build();
  }

  @Test
  void createParticipant_shouldReturnCreatedStatus() throws Exception {
    ParticipantCreateRequest request = validCreateRequest();
    when(participantService.createParticipant(any(ParticipantCreateRequest.class)))
        .thenReturn(participant());
    when(participantMapper.participantToParticipantResponse(any(Participant.class)))
        .thenReturn(participantResponse());

    mockMvc
        .perform(
            post("/api/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void createParticipant_shouldReturnFirstNameInBody() throws Exception {
    ParticipantCreateRequest request = validCreateRequest();
    when(participantService.createParticipant(any(ParticipantCreateRequest.class)))
        .thenReturn(participant());
    when(participantMapper.participantToParticipantResponse(any(Participant.class)))
        .thenReturn(participantResponse());

    mockMvc
        .perform(
            post("/api/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(jsonPath("$.firstName").value("Ivan"));
  }

  @Test
  void createParticipant_shouldReturnAgeInBody() throws Exception {
    ParticipantCreateRequest request = validCreateRequest();
    when(participantService.createParticipant(any(ParticipantCreateRequest.class)))
        .thenReturn(participant());
    when(participantMapper.participantToParticipantResponse(any(Participant.class)))
        .thenReturn(participantResponse());

    mockMvc
        .perform(
            post("/api/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(jsonPath("$.age").value(25));
  }

  @Test
  void createParticipant_shouldReturnBadRequest_whenFirstNameBlank() throws Exception {
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName(" ")
            .age(25)
            .participantGender(ParticipantGender.MALE)
            .userId(1L)
            .build();

    mockMvc
        .perform(
            post("/api/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createParticipant_shouldReturnBadRequest_whenAgeNegative() throws Exception {
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName("Ivan")
            .age(-1)
            .participantGender(ParticipantGender.MALE)
            .userId(1L)
            .build();

    mockMvc
        .perform(
            post("/api/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createParticipant_shouldReturnBadRequest_whenAgeTooHigh() throws Exception {
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName("Ivan")
            .age(151)
            .participantGender(ParticipantGender.MALE)
            .userId(1L)
            .build();

    mockMvc
        .perform(
            post("/api/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createParticipant_shouldReturnBadRequest_whenGenderNull() throws Exception {
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder().firstName("Ivan").age(25).userId(1L).build();

    mockMvc
        .perform(
            post("/api/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createParticipant_shouldReturnBadRequest_whenUserIdNull() throws Exception {
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName("Ivan")
            .age(25)
            .participantGender(ParticipantGender.MALE)
            .build();

    mockMvc
        .perform(
            post("/api/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createParticipant_shouldAcceptBoundaryAges() throws Exception {
    ParticipantCreateRequest request =
        ParticipantCreateRequest.builder()
            .firstName("Ivan")
            .age(150)
            .participantGender(ParticipantGender.MALE)
            .userId(1L)
            .build();
    when(participantService.createParticipant(any(ParticipantCreateRequest.class)))
        .thenReturn(participant());
    when(participantMapper.participantToParticipantResponse(any(Participant.class)))
        .thenReturn(participantResponse());

    mockMvc
        .perform(
            post("/api/participants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void findAllParticipants_shouldReturnOkStatus() throws Exception {
    when(participantService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

    mockMvc.perform(get("/api/participants")).andExpect(status().isOk());
  }

  @Test
  void findParticipantById_shouldReturnOkStatus() throws Exception {
    when(participantService.findById(1L)).thenReturn(participant());
    when(participantMapper.participantToParticipantResponse(any(Participant.class)))
        .thenReturn(participantResponse());

    mockMvc.perform(get("/api/participants/1")).andExpect(status().isOk());
  }

  @Test
  void findParticipantById_shouldReturnFirstNameInBody() throws Exception {
    when(participantService.findById(1L)).thenReturn(participant());
    when(participantMapper.participantToParticipantResponse(any(Participant.class)))
        .thenReturn(participantResponse());

    mockMvc.perform(get("/api/participants/1")).andExpect(jsonPath("$.firstName").value("Ivan"));
  }

  @Test
  void findParticipantById_shouldReturnNotFound_whenServiceThrows() throws Exception {
    when(participantService.findById(999L))
        .thenThrow(new ParticipantNotFoundException(999L, "findById"));

    mockMvc.perform(get("/api/participants/999")).andExpect(status().isNotFound());
  }

  @Test
  void updateParticipant_shouldReturnOkStatus() throws Exception {
    ParticipantUpdateRequest request = ParticipantUpdateRequest.builder().firstName("Ivan").build();
    when(participantService.updateParticipant(eq(1L), any(ParticipantUpdateRequest.class)))
        .thenReturn(participant());
    when(participantMapper.participantToParticipantResponse(any(Participant.class)))
        .thenReturn(participantResponse());

    mockMvc
        .perform(
            patch("/api/participants/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void updateParticipant_shouldReturnFirstNameInBody() throws Exception {
    ParticipantUpdateRequest request = ParticipantUpdateRequest.builder().firstName("Ivan").build();
    when(participantService.updateParticipant(eq(1L), any(ParticipantUpdateRequest.class)))
        .thenReturn(participant());
    when(participantMapper.participantToParticipantResponse(any(Participant.class)))
        .thenReturn(participantResponse());

    mockMvc
        .perform(
            patch("/api/participants/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(jsonPath("$.firstName").value("Ivan"));
  }

  @Test
  void updateParticipant_shouldReturnNotFound_whenServiceThrows() throws Exception {
    ParticipantUpdateRequest request = ParticipantUpdateRequest.builder().firstName("Ivan").build();
    when(participantService.updateParticipant(eq(999L), any(ParticipantUpdateRequest.class)))
        .thenThrow(new ParticipantNotFoundException(999L, "updateParticipant"));

    mockMvc
        .perform(
            patch("/api/participants/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteParticipant_shouldReturnNoContent() throws Exception {
    mockMvc.perform(delete("/api/participants/1")).andExpect(status().isNoContent());
  }

  @Test
  void deleteParticipant_shouldReturnNotFound_whenServiceThrows() throws Exception {
    doThrow(new ParticipantNotFoundException(999L, "deleteParticipant"))
        .when(participantService)
        .deleteParticipant(999L);

    mockMvc.perform(delete("/api/participants/999")).andExpect(status().isNotFound());
  }
}
