package com.eventreg.integration;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventreg.controller.EventController;
import com.eventreg.dto.request.create.EventCreateRequest;
import com.eventreg.dto.request.update.EventUpdateRequest;
import com.eventreg.model.Event;
import com.eventreg.model.User;
import com.eventreg.model.enums.EventGenderRequirement;
import com.eventreg.model.enums.EventRegistrationStatus;
import com.eventreg.model.enums.EventStatus;
import com.eventreg.model.enums.RBAC.Role;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Integration tests for {@link EventController}. */
@Testcontainers
public class EventControllerIntegrationTest extends BaseControllerIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  private EventCreateRequest createEventRequest(User organizer, String eventName) {
    return EventCreateRequest.builder()
        .eventName(eventName)
        .eventDescription("Annual tech meetup")
        .eventDate(OffsetDateTime.now().plusDays(30))
        .eventDuration(Duration.ofHours(2))
        .location("Moscow, Tverskaya 1")
        .ageRequired(18)
        .eventGenderRequirement(EventGenderRequirement.NONE)
        .maxParticipantAmount(100)
        .confirmationRequired(false)
        .waitlistWhenAllReserved(false)
        .organizerId(organizer.getId())
        .build();
  }

  private User admin() {
    return createUser("admin2", Role.ADMIN);
  }

  // ---------- createEvent ----------

  @Test
  void shouldReturnCreatedEvent_WhenOrganiserCreatesEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);

    mockMvc
        .perform(
            as(
                postJson("/api/events", createEventRequest(organiser, "Tech Conference")),
                organiser))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.eventName").value("Tech Conference"))
        .andExpect(jsonPath("$.eventReservationStatus").value("RESERVATIONS_OPEN"))
        .andExpect(jsonPath("$.ageRequired").value(18))
        .andExpect(jsonPath("$.maxParticipantAmount").value(100))
        .andExpect(jsonPath("$.confirmationRequired").value(false))
        .andExpect(jsonPath("$.organizerId").value(organiser.getId()));
  }

  @Test
  void shouldReturnCreatedEvent_WhenAdminCreatesEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    User admin = admin();

    mockMvc
        .perform(
            as(postJson("/api/events", createEventRequest(organiser, "Tech Conference")), admin))
        .andExpect(status().isCreated());
  }

  @Test
  void shouldReturnForbidden_WhenParticipantCreatesEvent() throws Exception {
    User participant = createUser("alice", Role.PARTICIPANT);

    mockMvc
        .perform(
            as(
                postJson("/api/events", createEventRequest(participant, "Tech Conference")),
                participant))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenAnonymousCreatesEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);

    mockMvc
        .perform(postJson("/api/events", createEventRequest(organiser, "Tech Conference")))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnNotFound_WhenOrganizerUserDoesNotExist() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    EventCreateRequest request = createEventRequest(organiser, "Tech Conference");
    request.setOrganizerId(99999L);

    mockMvc
        .perform(as(postJson("/api/events", request), organiser))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnBadRequest_WhenEventNameIsBlank() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    EventCreateRequest request = createEventRequest(organiser, " ");
    request.setOrganizerId(organiser.getId());

    mockMvc
        .perform(as(postJson("/api/events", request), organiser))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenLocationIsBlank() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    EventCreateRequest request = createEventRequest(organiser, "Tech Conference");
    request.setLocation(" ");

    mockMvc
        .perform(as(postJson("/api/events", request), organiser))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenEventDateIsInThePast() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    EventCreateRequest request = createEventRequest(organiser, "Tech Conference");
    request.setEventDate(OffsetDateTime.now().minusDays(1));

    mockMvc
        .perform(as(postJson("/api/events", request), organiser))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenEventDateIsMissing() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    EventCreateRequest request = createEventRequest(organiser, "Tech Conference");
    request.setEventDate(null);

    mockMvc
        .perform(as(postJson("/api/events", request), organiser))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenEventDurationIsMissing() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    EventCreateRequest request = createEventRequest(organiser, "Tech Conference");
    request.setEventDuration(null);

    mockMvc
        .perform(as(postJson("/api/events", request), organiser))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenGenderRequirementIsMissing() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    EventCreateRequest request = createEventRequest(organiser, "Tech Conference");
    request.setEventGenderRequirement(null);

    mockMvc
        .perform(as(postJson("/api/events", request), organiser))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenMaxParticipantAmountIsZero() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    EventCreateRequest request = createEventRequest(organiser, "Tech Conference");
    request.setMaxParticipantAmount(0);

    mockMvc
        .perform(as(postJson("/api/events", request), organiser))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenAgeRequiredIsNegative() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    EventCreateRequest request = createEventRequest(organiser, "Tech Conference");
    request.setAgeRequired(-1);

    mockMvc
        .perform(as(postJson("/api/events", request), organiser))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_WhenOrganizerIdIsMissing() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    EventCreateRequest request = createEventRequest(organiser, "Tech Conference");
    request.setOrganizerId(null);

    mockMvc
        .perform(as(postJson("/api/events", request), organiser))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnCreated_WhenDescriptionIsExactlyOneThousandCharacters() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    EventCreateRequest request = createEventRequest(organiser, "Tech Conference");
    request.setEventDescription("a".repeat(1000));

    mockMvc
        .perform(as(postJson("/api/events", request), organiser))
        .andExpect(status().isCreated());
  }

  @Test
  void shouldReturnBadRequest_WhenDescriptionIsLongerThanOneThousandCharacters() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    EventCreateRequest request = createEventRequest(organiser, "Tech Conference");
    request.setEventDescription("a".repeat(1001));

    mockMvc
        .perform(as(postJson("/api/events", request), organiser))
        .andExpect(status().isBadRequest());
  }

  // ---------- findAll ----------

  @Test
  void shouldReturnAllEvents_WhenAuthenticated() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    createEvent("Tech Conference", organiser, 10, true, false);
    createEvent("Spring Meetup", organiser, 10, true, false);
    User participant = createUser("alice", Role.PARTICIPANT);

    mockMvc
        .perform(as(get("/api/events"), participant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void shouldReturnForbidden_WhenAnonymousListsEvents() throws Exception {
    mockMvc.perform(get("/api/events")).andExpect(status().isForbidden());
  }

  @Test
  void shouldSortEventsByDateDescending_ByDefault() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event early = createEvent("Early Event", organiser, 10, true, false);
    Event later = createEvent("Later Event", organiser, 10, true, false);
    early.setEventDate(OffsetDateTime.now().plusDays(1));
    later.setEventDate(OffsetDateTime.now().plusDays(30));
    eventRepository.save(early);
    eventRepository.save(later);
    User participant = createUser("alice", Role.PARTICIPANT);

    mockMvc
        .perform(as(get("/api/events"), participant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].eventName").value("Later Event"));
  }

  @Test
  void shouldReturnSecondPage_WhenPagingEvents() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    createEvent("Event One", organiser, 10, true, false);
    createEvent("Event Two", organiser, 10, true, false);
    createEvent("Event Three", organiser, 10, true, false);
    User participant = createUser("alice", Role.PARTICIPANT);

    mockMvc
        .perform(as(get("/api/events?page=1&size=1"), participant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(1))
        .andExpect(jsonPath("$.numberOfElements").value(1));
  }

  // ---------- findById ----------

  @Test
  void shouldReturnEvent_WhenAuthenticatedRequestsById() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    User participant = createUser("alice", Role.PARTICIPANT);

    mockMvc
        .perform(as(get("/api/events/{id}", event.getId()), participant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventName").value("Tech Conference"))
        .andExpect(jsonPath("$.organizerId").value(organiser.getId()))
        .andExpect(jsonPath("$.currentParticipantAmount").value(0));
  }

  @Test
  void shouldReturnNotFound_WhenEventDoesNotExist() throws Exception {
    User participant = createUser("alice", Role.PARTICIPANT);

    mockMvc.perform(as(get("/api/events/99999"), participant)).andExpect(status().isNotFound());
  }

  @Test
  void shouldReturnForbidden_WhenAnonymousRequestsEvent() throws Exception {
    mockMvc.perform(get("/api/events/99999")).andExpect(status().isForbidden());
  }

  // ---------- downloadIcs ----------

  @Test
  void shouldReturnCalendarFile_WhenAuthenticatedRequestsIcs() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    User participant = createUser("alice", Role.PARTICIPANT);

    mockMvc
        .perform(as(get("/api/events/{id}/ics", event.getId()), participant))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", "attachment; filename=event.ics"))
        .andExpect(content().contentType("text/calendar"))
        .andExpect(content().string(containsString("BEGIN:VCALENDAR")));
  }

  @Test
  void shouldReturnForbidden_WhenAnonymousRequestsIcs() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);

    mockMvc.perform(get("/api/events/{id}/ics", event.getId())).andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnNotFound_WhenIcsRequestedForNonexistentEvent() throws Exception {
    User participant = createUser("alice", Role.PARTICIPANT);

    mockMvc.perform(as(get("/api/events/99999/ics"), participant)).andExpect(status().isNotFound());
  }

  // ---------- search ----------

  @Test
  void shouldReturnMatchingEvents_WhenSearchingBySubstring() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    createEvent("Java Conference", organiser, 10, true, false);
    createEvent("Spring Meetup", organiser, 10, true, false);
    User participant = createUser("alice", Role.PARTICIPANT);
    Map<String, Object> filter =
        Map.of(
            "type", "SEARCH",
            "key", "eventName",
            "operation", "LIKE",
            "value", "conference");

    mockMvc
        .perform(as(postJson("/api/events/search", filter), participant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].eventName").value("Java Conference"));
  }

  @Test
  void shouldReturnMatchingEvents_WhenFilteringByExactName() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    createEvent("Java Conference", organiser, 10, true, false);
    createEvent("Spring Meetup", organiser, 10, true, false);
    User participant = createUser("alice", Role.PARTICIPANT);
    Map<String, Object> filter =
        Map.of(
            "type", "SEARCH",
            "key", "eventName",
            "operation", "EQ",
            "value", "Java Conference");

    mockMvc
        .perform(as(postJson("/api/events/search", filter), participant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].eventName").value("Java Conference"));
  }

  @Test
  void shouldReturnMatchingEvents_WhenUsingFullTextSearch() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    createEvent("Java Conference", organiser, 10, true, false);
    createEvent("Spring Meetup", organiser, 10, true, false);
    User participant = createUser("alice", Role.PARTICIPANT);
    Map<String, Object> filter =
        Map.of(
            "type", "SEARCH",
            "key", "eventName",
            "operation", "SEARCH",
            "value", "meetup");

    mockMvc
        .perform(as(postJson("/api/events/search", filter), participant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].eventName").value("Spring Meetup"));
  }

  @Test
  void shouldReturnMatchingEvents_WhenUsingOrCombination() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    createEvent("Java Conference", organiser, 10, true, false);
    createEvent("Spring Meetup", organiser, 10, true, false);
    createEvent("Winter Camp", organiser, 10, true, false);
    User participant = createUser("alice", Role.PARTICIPANT);
    Map<String, Object> filter =
        Map.of(
            "type",
            "OR",
            "value",
            List.of(
                Map.of(
                    "type", "SEARCH",
                    "key", "eventName",
                    "operation", "LIKE",
                    "value", "java"),
                Map.of(
                    "type", "SEARCH",
                    "key", "eventName",
                    "operation", "LIKE",
                    "value", "spring")));

    mockMvc
        .perform(as(postJson("/api/events/search", filter), participant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void shouldReturnMatchingEvents_WhenUsingAndCombination() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    createEvent("Java Conference", organiser, 10, true, false);
    createEvent("Spring Meetup", organiser, 10, true, false);
    User participant = createUser("alice", Role.PARTICIPANT);
    Map<String, Object> filter =
        Map.of(
            "type",
            "AND",
            "value",
            List.of(
                Map.of(
                    "type", "SEARCH",
                    "key", "eventName",
                    "operation", "LIKE",
                    "value", "java"),
                Map.of(
                    "type", "SEARCH",
                    "key", "eventName",
                    "operation", "EQ",
                    "value", "Java Conference")));

    mockMvc
        .perform(as(postJson("/api/events/search", filter), participant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void shouldReturnEventsWithFreeSeats_WhenUsingHasFreeSeatsFilter() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    createEvent("Event With Free Seats", organiser, 10, true, false);
    Event full = createEvent("Event Without Free Seats", organiser, 10, true, false);
    full.setCurrentParticipantAmount(10);
    eventRepository.save(full);
    User participant = createUser("alice", Role.PARTICIPANT);
    Map<String, Object> filter =
        Map.of("type", "SEARCH", "key", "eventName", "operation", "HAS_FREE_SEATS", "value", "x");

    mockMvc
        .perform(as(postJson("/api/events/search", filter), participant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void shouldReturnMatchingEvents_WhenUsingInFilter() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    createEvent("Java Conference", organiser, 10, true, false);
    createEvent("Spring Meetup", organiser, 10, true, false);
    createEvent("Winter Camp", organiser, 10, true, false);
    User participant = createUser("alice", Role.PARTICIPANT);
    Map<String, Object> filter =
        Map.of(
            "type", "SEARCH",
            "key", "eventName",
            "operation", "IN",
            "value", List.of("Java Conference", "Spring Meetup"));

    mockMvc
        .perform(as(postJson("/api/events/search", filter), participant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void shouldReturnAllEvents_WhenSearchBodyIsEmpty() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    createEvent("Java Conference", organiser, 10, true, false);
    createEvent("Spring Meetup", organiser, 10, true, false);
    User participant = createUser("alice", Role.PARTICIPANT);

    mockMvc
        .perform(as(post("/api/events/search"), participant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void shouldReturnSecondPage_WhenPagingSearchResults() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    createEvent("Java Conference", organiser, 10, true, false);
    createEvent("Spring Meetup", organiser, 10, true, false);
    createEvent("Winter Camp", organiser, 10, true, false);
    User participant = createUser("alice", Role.PARTICIPANT);
    Map<String, Object> filter =
        Map.of(
            "type", "SEARCH",
            "key", "eventName",
            "operation", "LIKE",
            "value", "e");

    mockMvc
        .perform(as(postJson("/api/events/search?page=1&size=1", filter), participant))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(1))
        .andExpect(jsonPath("$.numberOfElements").value(1));
  }

  // ---------- updateEvent ----------

  @Test
  void shouldReturnUpdatedName_WhenOrganiserUpdatesOwnEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    EventUpdateRequest update =
        EventUpdateRequest.builder().eventName("Tech Conference 2026").build();

    mockMvc
        .perform(as(patchJson("/api/events/{id}", event.getId(), update), organiser))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventName").value("Tech Conference 2026"));
  }

  @Test
  void shouldReturnUpdatedLocation_WhenOrganiserUpdatesOwnEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    EventUpdateRequest update = EventUpdateRequest.builder().location("Saint Petersburg").build();

    mockMvc
        .perform(as(patchJson("/api/events/{id}", event.getId(), update), organiser))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.location").value("Saint Petersburg"));
  }

  @Test
  void shouldReturnUpdatedDate_WhenOrganiserUpdatesOwnEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    EventUpdateRequest update =
        EventUpdateRequest.builder().eventDate(OffsetDateTime.now().plusDays(60)).build();

    mockMvc
        .perform(as(patchJson("/api/events/{id}", event.getId(), update), organiser))
        .andExpect(status().isOk());
  }

  @Test
  void shouldReturnUpdatedMaxParticipantAmount_WhenOrganiserUpdatesOwnEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    EventUpdateRequest update = EventUpdateRequest.builder().maxParticipantAmount(200).build();

    mockMvc
        .perform(as(patchJson("/api/events/{id}", event.getId(), update), organiser))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.maxParticipantAmount").value(200));
  }

  @Test
  void shouldCloseReservations_WhenOrganiserCancelsOwnEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    EventUpdateRequest update =
        EventUpdateRequest.builder().eventStatus(EventStatus.CANCELLED).build();

    mockMvc
        .perform(as(patchJson("/api/events/{id}", event.getId(), update), organiser))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventStatus").value("CANCELLED"))
        .andExpect(jsonPath("$.eventReservationStatus").value("RESERVATIONS_CLOSED"));
  }

  @Test
  void shouldReturnForbidden_WhenOrganiserUpdatesAnotherOrganisersEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    User anotherOrganiser = createUser("organiser2", Role.ORGANISER);
    EventUpdateRequest update = EventUpdateRequest.builder().eventName("Renamed").build();

    mockMvc
        .perform(as(patchJson("/api/events/{id}", event.getId(), update), anotherOrganiser))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenParticipantUpdatesEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    User participant = createUser("alice", Role.PARTICIPANT);
    EventUpdateRequest update = EventUpdateRequest.builder().eventName("Renamed").build();

    mockMvc
        .perform(as(patchJson("/api/events/{id}", event.getId(), update), participant))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenAnonymousUpdatesEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    EventUpdateRequest update = EventUpdateRequest.builder().eventName("Renamed").build();

    mockMvc
        .perform(patchJson("/api/events/{id}", event.getId(), update))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnOk_WhenAdminUpdatesAnotherOrganisersEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    User admin = admin();
    EventUpdateRequest update = EventUpdateRequest.builder().eventName("Renamed By Admin").build();

    mockMvc
        .perform(as(patchJson("/api/events/{id}", event.getId(), update), admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eventName").value("Renamed By Admin"));
  }

  @Test
  void shouldReturnNotFound_WhenUpdatingNonexistentEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    EventUpdateRequest update = EventUpdateRequest.builder().eventName("Renamed").build();

    mockMvc
        .perform(as(patchJson("/api/events/99999", update), organiser))
        .andExpect(status().isNotFound());
  }

  // ---------- deleteEvent ----------

  @Test
  void shouldReturnNoContent_WhenOrganiserDeletesOwnEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);

    mockMvc
        .perform(as(delete("/api/events/{id}", event.getId()), organiser))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnNoContent_WhenAdminDeletesAnotherOrganisersEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    User admin = admin();

    mockMvc
        .perform(as(delete("/api/events/{id}", event.getId()), admin))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnForbidden_WhenOrganiserDeletesAnotherOrganisersEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    User anotherOrganiser = createUser("organiser2", Role.ORGANISER);

    mockMvc
        .perform(as(delete("/api/events/{id}", event.getId()), anotherOrganiser))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnForbidden_WhenParticipantDeletesEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    User participant = createUser("alice", Role.PARTICIPANT);

    mockMvc
        .perform(as(delete("/api/events/{id}", event.getId()), participant))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnNotFound_WhenDeletingNonexistentEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);

    mockMvc.perform(as(delete("/api/events/99999"), organiser)).andExpect(status().isNotFound());
  }

  @Test
  void shouldDeleteActiveRegistrations_WhenDeletingEvent() throws Exception {
    User organiser = createUser("organiser", Role.ORGANISER);
    User alice = createUser("alice", Role.PARTICIPANT);
    Event event = createEvent("Tech Conference", organiser, 10, true, false);
    createRegistration(event, createParticipant(alice), EventRegistrationStatus.ACCEPTED);

    mockMvc
        .perform(as(delete("/api/events/{id}", event.getId()), organiser))
        .andExpect(status().isNoContent());

    Integer remainingRegistrations =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM event_registrations", Integer.class);
    assertEquals(0, remainingRegistrations);
  }
}
