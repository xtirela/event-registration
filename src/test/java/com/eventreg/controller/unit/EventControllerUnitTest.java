package com.eventreg.controller.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventreg.controller.EventController;
import com.eventreg.dto.request.create.EventCreateRequest;
import com.eventreg.dto.request.update.EventUpdateRequest;
import com.eventreg.dto.response.EventResponse;
import com.eventreg.exception.EventNotFoundException;
import com.eventreg.mapper.EventMapper;
import com.eventreg.model.Event;
import com.eventreg.model.User;
import com.eventreg.model.enums.EventGenderRequirement;
import com.eventreg.model.enums.EventReservationStatus;
import com.eventreg.model.enums.EventStatus;
import com.eventreg.service.EventService;
import com.eventreg.service.implementation.IcsService;
import com.eventreg.specification.EventSpecificationCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Unit tests for {@link EventController}. */
@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
public class EventControllerUnitTest extends BaseControllerUnitTest {

  @Autowired MockMvc mockMvc;

  @Autowired ObjectMapper objectMapper;

  @MockitoBean private EventSpecificationCreator eventSpecificationCreator;
  @MockitoBean private EventService eventService;
  @MockitoBean private EventMapper eventMapper;
  @MockitoBean private IcsService icsService;

  private EventCreateRequest validCreateRequest() {
    return EventCreateRequest.builder()
        .eventName("Tech Conference")
        .eventDescription("Annual tech meetup")
        .eventDate(OffsetDateTime.now().plusDays(10))
        .eventDuration(Duration.ofHours(2))
        .location("Main Hall")
        .ageRequired(18)
        .eventGenderRequirement(EventGenderRequirement.NONE)
        .maxParticipantAmount(100)
        .confirmationRequired(false)
        .waitlistWhenAllReserved(false)
        .organizerId(1L)
        .build();
  }

  private Event event() {
    User organizer = User.builder().id(1L).username("organizer").build();
    return Event.builder()
        .id(1L)
        .eventName("Tech Conference")
        .eventDescription("Annual tech meetup")
        .eventDate(OffsetDateTime.now().plusDays(10))
        .eventDuration(Duration.ofHours(2))
        .location("Main Hall")
        .ageRequired(18)
        .eventGenderRequirement(EventGenderRequirement.NONE)
        .maxParticipantAmount(100)
        .currentParticipantAmount(0)
        .eventStatus(EventStatus.PLANNED)
        .eventReservationStatus(EventReservationStatus.RESERVATIONS_OPEN)
        .organizer(organizer)
        .build();
  }

  private Page<Event> emptyPage() {
    return new PageImpl<>(List.of());
  }

  private EventResponse eventResponse() {
    return EventResponse.builder()
        .id(1L)
        .eventName("Tech Conference")
        .eventDescription("Annual tech meetup")
        .eventDate(OffsetDateTime.now().plusDays(10))
        .eventDuration(Duration.ofHours(2))
        .location("Main Hall")
        .ageRequired(18)
        .eventGenderRequirement(EventGenderRequirement.NONE)
        .maxParticipantAmount(100)
        .currentParticipantAmount(0)
        .currentWaitingQueueParticipantAmount(0)
        .eventStatus(EventStatus.PLANNED)
        .eventReservationStatus(EventReservationStatus.RESERVATIONS_OPEN)
        .confirmationRequired(false)
        .organizerId(1L)
        .build();
  }

  @Test
  void createEvent_shouldReturnCreatedStatus() throws Exception {
    EventCreateRequest request = validCreateRequest();
    when(eventService.createEvent(any(EventCreateRequest.class))).thenReturn(event());
    when(eventMapper.eventToEventResponse(any(Event.class))).thenReturn(eventResponse());

    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void createEvent_shouldReturnEventIdInBody() throws Exception {
    EventCreateRequest request = validCreateRequest();
    when(eventService.createEvent(any(EventCreateRequest.class))).thenReturn(event());
    when(eventMapper.eventToEventResponse(any(Event.class))).thenReturn(eventResponse());

    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void createEvent_shouldReturnEventNameInBody() throws Exception {
    EventCreateRequest request = validCreateRequest();
    when(eventService.createEvent(any(EventCreateRequest.class))).thenReturn(event());
    when(eventMapper.eventToEventResponse(any(Event.class))).thenReturn(eventResponse());

    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(jsonPath("$.eventName").value("Tech Conference"));
  }

  @Test
  void createEvent_shouldReturnBadRequest_whenEventNameBlank() throws Exception {
    EventCreateRequest request =
        EventCreateRequest.builder()
            .eventName(" ")
            .eventDate(OffsetDateTime.now().plusDays(10))
            .eventDuration(Duration.ofHours(2))
            .location("Main Hall")
            .ageRequired(18)
            .eventGenderRequirement(EventGenderRequirement.NONE)
            .maxParticipantAmount(100)
            .organizerId(1L)
            .build();

    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createEvent_shouldReturnBadRequest_whenEventDateNull() throws Exception {
    EventCreateRequest request =
        EventCreateRequest.builder()
            .eventName("Tech Conference")
            .eventDuration(Duration.ofHours(2))
            .location("Main Hall")
            .ageRequired(18)
            .eventGenderRequirement(EventGenderRequirement.NONE)
            .maxParticipantAmount(100)
            .organizerId(1L)
            .build();

    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createEvent_shouldReturnBadRequest_whenEventDateInPast() throws Exception {
    EventCreateRequest request =
        EventCreateRequest.builder()
            .eventName("Tech Conference")
            .eventDate(OffsetDateTime.now().minusDays(1))
            .eventDuration(Duration.ofHours(2))
            .location("Main Hall")
            .ageRequired(18)
            .eventGenderRequirement(EventGenderRequirement.NONE)
            .maxParticipantAmount(100)
            .organizerId(1L)
            .build();

    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createEvent_shouldReturnBadRequest_whenLocationBlank() throws Exception {
    EventCreateRequest request =
        EventCreateRequest.builder()
            .eventName("Tech Conference")
            .eventDate(OffsetDateTime.now().plusDays(10))
            .eventDuration(Duration.ofHours(2))
            .location(" ")
            .ageRequired(18)
            .eventGenderRequirement(EventGenderRequirement.NONE)
            .maxParticipantAmount(100)
            .organizerId(1L)
            .build();

    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createEvent_shouldReturnBadRequest_whenGenderRequirementNull() throws Exception {
    EventCreateRequest request =
        EventCreateRequest.builder()
            .eventName("Tech Conference")
            .eventDate(OffsetDateTime.now().plusDays(10))
            .eventDuration(Duration.ofHours(2))
            .location("Main Hall")
            .ageRequired(18)
            .maxParticipantAmount(100)
            .organizerId(1L)
            .build();

    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createEvent_shouldReturnBadRequest_whenOrganizerIdNegative() throws Exception {
    EventCreateRequest request =
        EventCreateRequest.builder()
            .eventName("Tech Conference")
            .eventDate(OffsetDateTime.now().plusDays(10))
            .eventDuration(Duration.ofHours(2))
            .location("Main Hall")
            .ageRequired(18)
            .eventGenderRequirement(EventGenderRequirement.NONE)
            .maxParticipantAmount(100)
            .organizerId(-5L)
            .build();

    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createEvent_shouldReturnBadRequest_whenMaxParticipantAmountZero() throws Exception {
    EventCreateRequest request =
        EventCreateRequest.builder()
            .eventName("Tech Conference")
            .eventDate(OffsetDateTime.now().plusDays(10))
            .eventDuration(Duration.ofHours(2))
            .location("Main Hall")
            .ageRequired(18)
            .eventGenderRequirement(EventGenderRequirement.NONE)
            .maxParticipantAmount(0)
            .organizerId(1L)
            .build();

    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createEvent_shouldAcceptMinimumValidAgeAndCapacity() throws Exception {
    EventCreateRequest request =
        EventCreateRequest.builder()
            .eventName("Tech Conference")
            .eventDate(OffsetDateTime.now().plusDays(10))
            .eventDuration(Duration.ofHours(2))
            .location("Main Hall")
            .ageRequired(0)
            .eventGenderRequirement(EventGenderRequirement.NONE)
            .maxParticipantAmount(1)
            .organizerId(1L)
            .build();
    when(eventService.createEvent(any(EventCreateRequest.class))).thenReturn(event());
    when(eventMapper.eventToEventResponse(any(Event.class))).thenReturn(eventResponse());

    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  void createEvent_shouldReturnNotFound_whenServiceThrows() throws Exception {
    EventCreateRequest request = validCreateRequest();
    when(eventService.createEvent(any(EventCreateRequest.class)))
        .thenThrow(new EventNotFoundException(1L, "createEvent"));

    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateEvent_shouldReturnOkStatus() throws Exception {
    EventUpdateRequest request = EventUpdateRequest.builder().eventName("Updated").build();
    when(eventService.updateEvent(eq(1L), any(EventUpdateRequest.class))).thenReturn(event());
    when(eventMapper.eventToEventResponse(any(Event.class))).thenReturn(eventResponse());

    mockMvc
        .perform(
            patch("/api/events/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  void updateEvent_shouldReturnEventNameInBody() throws Exception {
    EventUpdateRequest request = EventUpdateRequest.builder().eventName("Updated").build();
    EventResponse response = EventResponse.builder().id(1L).eventName("Updated").build();
    when(eventService.updateEvent(eq(1L), any(EventUpdateRequest.class))).thenReturn(event());
    when(eventMapper.eventToEventResponse(any(Event.class))).thenReturn(response);

    mockMvc
        .perform(
            patch("/api/events/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(jsonPath("$.eventName").value("Updated"));
  }

  @Test
  void updateEvent_shouldReturnNotFound_whenServiceThrows() throws Exception {
    EventUpdateRequest request = EventUpdateRequest.builder().eventName("Updated").build();
    when(eventService.updateEvent(eq(1L), any(EventUpdateRequest.class)))
        .thenThrow(new EventNotFoundException(1L, "updateEvent"));

    mockMvc
        .perform(
            patch("/api/events/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  @Test
  void findEventById_shouldReturnOkStatus() throws Exception {
    when(eventService.findById(1L)).thenReturn(event());
    when(eventMapper.eventToEventResponse(any(Event.class))).thenReturn(eventResponse());

    mockMvc.perform(get("/api/events/1")).andExpect(status().isOk());
  }

  @Test
  void findEventById_shouldReturnEventIdInBody() throws Exception {
    when(eventService.findById(1L)).thenReturn(event());
    when(eventMapper.eventToEventResponse(any(Event.class))).thenReturn(eventResponse());

    mockMvc.perform(get("/api/events/1")).andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void findEventById_shouldReturnNotFound_whenServiceThrows() throws Exception {
    when(eventService.findById(999L)).thenThrow(new EventNotFoundException(999L, "findById"));

    mockMvc.perform(get("/api/events/999")).andExpect(status().isNotFound());
  }

  @Test
  void deleteEvent_shouldReturnNoContent() throws Exception {
    mockMvc.perform(delete("/api/events/1")).andExpect(status().isNoContent());
  }

  @Test
  void deleteEvent_shouldReturnNotFound_whenServiceThrows() throws Exception {
    doThrow(new EventNotFoundException(999L, "deleteEvent")).when(eventService).deleteEvent(999L);

    mockMvc.perform(delete("/api/events/999")).andExpect(status().isNotFound());
  }

  @Test
  void findAllEvents_shouldReturnOkStatus() throws Exception {
    when(eventService.findAll(any(Pageable.class))).thenReturn(emptyPage());

    mockMvc.perform(get("/api/events")).andExpect(status().isOk());
  }

  @Test
  void searchEvents_shouldReturnOkStatus() throws Exception {
    when(eventSpecificationCreator.create(any())).thenReturn((root, query, builder) -> null);
    when(eventService.findAll(any(), any(Pageable.class))).thenReturn(emptyPage());

    mockMvc.perform(post("/api/events/search")).andExpect(status().isOk());
  }

  @Test
  void downloadIcs_shouldReturnCalendarContentType() throws Exception {
    Event withDate = event();
    withDate.setEventDate(OffsetDateTime.now().plusDays(1));
    when(eventService.findById(1L)).thenReturn(withDate);
    when(icsService.generateIcsFile(any(), any(), any(), any()))
        .thenReturn("BEGIN:VCALENDAR".getBytes());

    mockMvc
        .perform(get("/api/events/1/ics"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("text/calendar"));
  }

  @Test
  void downloadIcs_shouldReturnNotFound_whenServiceThrows() throws Exception {
    when(eventService.findById(999L)).thenThrow(new EventNotFoundException(999L, "findById"));

    mockMvc.perform(get("/api/events/999/ics")).andExpect(status().isNotFound());
  }
}
