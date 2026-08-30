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

import com.eventreg.controller.EventRegistrationController;
import com.eventreg.dto.request.create.EventRegistrationCreateRequest;
import com.eventreg.dto.request.update.EventRegistrationStatusUpdateRequest;
import com.eventreg.dto.response.EventRegistrationResponse;
import com.eventreg.exception.EventRegistrationNotFoundException;
import com.eventreg.mapper.EventRegistrationMapper;
import com.eventreg.model.Event;
import com.eventreg.model.EventRegistration;
import com.eventreg.model.Participant;
import com.eventreg.model.enums.EventRegistrationStatus;
import com.eventreg.service.EventRegistrationService;
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

/** Unit tests for {@link EventRegistrationController}. */
@WebMvcTest(EventRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class EventRegistrationControllerUnitTest extends BaseControllerUnitTest {

  @Autowired MockMvc mockMvc;

  @Autowired ObjectMapper objectMapper;

  @MockitoBean private EventRegistrationService eventRegistrationService;
  @MockitoBean private EventRegistrationMapper eventRegistrationMapper;

  private EventRegistrationCreateRequest validCreateRequest() {
    return EventRegistrationCreateRequest.builder().eventId(1L).participantId(2L).build();
  }

  private EventRegistration eventRegistration() {
    return EventRegistration.builder()
        .id(1L)
        .eventRegistrationStatus(EventRegistrationStatus.PENDING)
        .description("none")
        .event(Event.builder().id(1L).build())
        .participant(Participant.builder().id(2L).build())
        .build();
  }

  private EventRegistrationResponse response(EventRegistrationStatus status) {
    return EventRegistrationResponse.builder()
        .id(1L)
        .eventId(1L)
        .participantId(2L)
        .eventRegistrationStatus(status)
        .description("none")
        .build();
  }

  @Test
  void createEventRegistration_shouldReturnCreatedStatus() throws Exception {
    EventRegistrationCreateRequest request = validCreateRequest();
    when(eventRegistrationService.createEventRegistration(
            any(EventRegistrationCreateRequest.class)))
        .thenReturn(eventRegistration());
    when(eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            any(EventRegistration.class)))
        .thenReturn(response(EventRegistrationStatus.PENDING));

    mockMvc
        .perform(
            post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void createEventRegistration_shouldReturnRegistrationIdInBody() throws Exception {
    EventRegistrationCreateRequest request = validCreateRequest();
    when(eventRegistrationService.createEventRegistration(
            any(EventRegistrationCreateRequest.class)))
        .thenReturn(eventRegistration());
    when(eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            any(EventRegistration.class)))
        .thenReturn(response(EventRegistrationStatus.PENDING));

    mockMvc
        .perform(
            post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void createEventRegistration_shouldReturnBadRequest_whenEventIdNull() throws Exception {
    EventRegistrationCreateRequest request =
        EventRegistrationCreateRequest.builder().participantId(2L).build();

    mockMvc
        .perform(
            post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createEventRegistration_shouldReturnBadRequest_whenParticipantIdNull() throws Exception {
    EventRegistrationCreateRequest request =
        EventRegistrationCreateRequest.builder().eventId(1L).build();

    mockMvc
        .perform(
            post("/api/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void directCreateEventRegistration_shouldReturnCreatedStatus() throws Exception {
    EventRegistrationCreateRequest request = validCreateRequest();
    when(eventRegistrationService.createEventRegistration(
            any(EventRegistrationCreateRequest.class)))
        .thenReturn(eventRegistration());
    when(eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            any(EventRegistration.class)))
        .thenReturn(response(EventRegistrationStatus.ACCEPTED));

    mockMvc
        .perform(
            post("/api/registrations/direct")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void directCreateEventRegistration_shouldReturnStatusInBody() throws Exception {
    EventRegistrationCreateRequest request = validCreateRequest();
    when(eventRegistrationService.createEventRegistration(
            any(EventRegistrationCreateRequest.class)))
        .thenReturn(eventRegistration());
    when(eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            any(EventRegistration.class)))
        .thenReturn(response(EventRegistrationStatus.ACCEPTED));

    mockMvc
        .perform(
            post("/api/registrations/direct")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(jsonPath("$.eventRegistrationStatus").value("ACCEPTED"));
  }

  @Test
  void directCreateEventRegistration_shouldReturnBadRequest_whenEventIdNull() throws Exception {
    EventRegistrationCreateRequest request =
        EventRegistrationCreateRequest.builder().participantId(2L).build();

    mockMvc
        .perform(
            post("/api/registrations/direct")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void findAllRegistrations_shouldReturnOkStatus() throws Exception {
    when(eventRegistrationService.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    mockMvc.perform(get("/api/registrations")).andExpect(status().isOk());
  }

  @Test
  void findRegistrationById_shouldReturnOkStatus() throws Exception {
    when(eventRegistrationService.findById(1L)).thenReturn(eventRegistration());
    when(eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            any(EventRegistration.class)))
        .thenReturn(response(EventRegistrationStatus.PENDING));

    mockMvc.perform(get("/api/registrations/1")).andExpect(status().isOk());
  }

  @Test
  void findRegistrationById_shouldReturnRegistrationIdInBody() throws Exception {
    when(eventRegistrationService.findById(1L)).thenReturn(eventRegistration());
    when(eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            any(EventRegistration.class)))
        .thenReturn(response(EventRegistrationStatus.PENDING));

    mockMvc.perform(get("/api/registrations/1")).andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void findRegistrationById_shouldReturnNotFound_whenServiceThrows() throws Exception {
    when(eventRegistrationService.findById(999L))
        .thenThrow(new EventRegistrationNotFoundException(999L, "findById"));

    mockMvc.perform(get("/api/registrations/999")).andExpect(status().isNotFound());
  }

  @Test
  void acceptRegistration_shouldReturnOkStatus() throws Exception {
    when(eventRegistrationService.changeRegistrationRequestStatus(
            eq(1L), eq(EventRegistrationStatus.ACCEPTED), any(String.class)))
        .thenReturn(eventRegistration());
    when(eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            any(EventRegistration.class)))
        .thenReturn(response(EventRegistrationStatus.ACCEPTED));

    mockMvc.perform(patch("/api/registrations/1/accept")).andExpect(status().isOk());
  }

  @Test
  void acceptRegistration_shouldReturnStatusInBody() throws Exception {
    when(eventRegistrationService.changeRegistrationRequestStatus(
            eq(1L), eq(EventRegistrationStatus.ACCEPTED), any(String.class)))
        .thenReturn(eventRegistration());
    when(eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            any(EventRegistration.class)))
        .thenReturn(response(EventRegistrationStatus.ACCEPTED));

    mockMvc
        .perform(patch("/api/registrations/1/accept"))
        .andExpect(jsonPath("$.eventRegistrationStatus").value("ACCEPTED"));
  }

  @Test
  void acceptRegistration_shouldReturnNotFound_whenServiceThrows() throws Exception {
    when(eventRegistrationService.changeRegistrationRequestStatus(
            eq(999L), eq(EventRegistrationStatus.ACCEPTED), any(String.class)))
        .thenThrow(new EventRegistrationNotFoundException(999L, "accept"));

    mockMvc.perform(patch("/api/registrations/999/accept")).andExpect(status().isNotFound());
  }

  @Test
  void denyRegistration_shouldReturnStatusInBody() throws Exception {
    when(eventRegistrationService.changeRegistrationRequestStatus(
            eq(1L), eq(EventRegistrationStatus.DENIED), any(String.class)))
        .thenReturn(eventRegistration());
    when(eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            any(EventRegistration.class)))
        .thenReturn(response(EventRegistrationStatus.DENIED));

    mockMvc
        .perform(patch("/api/registrations/1/deny"))
        .andExpect(jsonPath("$.eventRegistrationStatus").value("DENIED"));
  }

  @Test
  void denyRegistration_shouldReturnNotFound_whenServiceThrows() throws Exception {
    when(eventRegistrationService.changeRegistrationRequestStatus(
            eq(999L), eq(EventRegistrationStatus.DENIED), any(String.class)))
        .thenThrow(new EventRegistrationNotFoundException(999L, "deny"));

    mockMvc.perform(patch("/api/registrations/999/deny")).andExpect(status().isNotFound());
  }

  @Test
  void cancelRegistration_shouldReturnStatusInBody() throws Exception {
    when(eventRegistrationService.changeRegistrationRequestStatus(
            eq(1L), eq(EventRegistrationStatus.CANCELLED), any(String.class)))
        .thenReturn(eventRegistration());
    when(eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            any(EventRegistration.class)))
        .thenReturn(response(EventRegistrationStatus.CANCELLED));

    mockMvc
        .perform(patch("/api/registrations/1/cancel"))
        .andExpect(jsonPath("$.eventRegistrationStatus").value("CANCELLED"));
  }

  @Test
  void cancelRegistration_shouldReturnNotFound_whenServiceThrows() throws Exception {
    when(eventRegistrationService.changeRegistrationRequestStatus(
            eq(999L), eq(EventRegistrationStatus.CANCELLED), any(String.class)))
        .thenThrow(new EventRegistrationNotFoundException(999L, "cancel"));

    mockMvc.perform(patch("/api/registrations/999/cancel")).andExpect(status().isNotFound());
  }

  @Test
  void changeStatus_shouldReturnOkStatus() throws Exception {
    EventRegistrationStatusUpdateRequest request =
        EventRegistrationStatusUpdateRequest.builder()
            .eventRegistrationStatus(EventRegistrationStatus.ACCEPTED)
            .description("Approved")
            .build();
    when(eventRegistrationService.changeRegistrationRequestStatus(
            eq(1L), eq(EventRegistrationStatus.ACCEPTED), eq("Approved")))
        .thenReturn(eventRegistration());
    when(eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            any(EventRegistration.class)))
        .thenReturn(response(EventRegistrationStatus.ACCEPTED));

    mockMvc
        .perform(
            patch("/api/registrations/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void changeStatus_shouldReturnStatusInBody() throws Exception {
    EventRegistrationStatusUpdateRequest request =
        EventRegistrationStatusUpdateRequest.builder()
            .eventRegistrationStatus(EventRegistrationStatus.ACCEPTED)
            .description("Approved")
            .build();
    when(eventRegistrationService.changeRegistrationRequestStatus(
            eq(1L), eq(EventRegistrationStatus.ACCEPTED), eq("Approved")))
        .thenReturn(eventRegistration());
    when(eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            any(EventRegistration.class)))
        .thenReturn(response(EventRegistrationStatus.ACCEPTED));

    mockMvc
        .perform(
            patch("/api/registrations/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(jsonPath("$.eventRegistrationStatus").value("ACCEPTED"));
  }

  @Test
  void changeStatus_shouldReturnBadRequest_whenStatusNull() throws Exception {
    EventRegistrationStatusUpdateRequest request =
        EventRegistrationStatusUpdateRequest.builder().description("Approved").build();

    mockMvc
        .perform(
            patch("/api/registrations/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void changeStatus_shouldReturnNotFound_whenServiceThrows() throws Exception {
    EventRegistrationStatusUpdateRequest request =
        EventRegistrationStatusUpdateRequest.builder()
            .eventRegistrationStatus(EventRegistrationStatus.ACCEPTED)
            .description("Approved")
            .build();
    when(eventRegistrationService.changeRegistrationRequestStatus(
            eq(999L), eq(EventRegistrationStatus.ACCEPTED), eq("Approved")))
        .thenThrow(new EventRegistrationNotFoundException(999L, "changeStatus"));

    mockMvc
        .perform(
            patch("/api/registrations/999/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteEventRegistration_shouldReturnNoContent() throws Exception {
    mockMvc.perform(delete("/api/registrations/1")).andExpect(status().isNoContent());
  }

  @Test
  void deleteEventRegistration_shouldReturnNotFound_whenServiceThrows() throws Exception {
    doThrow(new EventRegistrationNotFoundException(999L, "deleteEventRegistration"))
        .when(eventRegistrationService)
        .deleteEventRegistration(999L);

    mockMvc.perform(delete("/api/registrations/999")).andExpect(status().isNotFound());
  }
}
