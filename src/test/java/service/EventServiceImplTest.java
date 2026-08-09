package service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dto.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import model.enums.EventGenderRequirement;
import model.enums.EventRegRequestStatus;
import model.enums.ParticipantGender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.implementation.EventServiceImpl;

public class EventServiceImplTest {

  public EventService eventService;

  @BeforeEach
  public void setUp() {

    eventService = new EventServiceImpl();
    String[] eventInput = {"Test event", "2030-01-01T12:00:00+00:00", "10", "1", "100", "NONE"};

    EventCreateRequest eventCreateRequest =
        EventCreateRequest.builder()
            .eventName(eventInput[0])
            .eventDate(OffsetDateTime.parse(eventInput[1]))
            .eventDuration(Duration.ofHours(Long.parseLong(eventInput[2])))
            .ageRequired(Integer.parseInt(eventInput[3]))
            .maxParticipantAmount(Integer.parseInt(eventInput[4]))
            .genderRequirement(EventGenderRequirement.fromString(eventInput[5]))
            .build();

    eventService.createEvent(eventCreateRequest);

    String[] eventInput2 = {
      "Test event two", "2040-01-01T12:00:00+00:00", "100", "10", "10", "NONE"
    };

    EventCreateRequest eventCreateRequest2 =
        EventCreateRequest.builder()
            .eventName(eventInput2[0])
            .eventDate(OffsetDateTime.parse(eventInput2[1]))
            .eventDuration(Duration.ofHours(Long.parseLong(eventInput2[2])))
            .ageRequired(Integer.parseInt(eventInput2[3]))
            .maxParticipantAmount(Integer.parseInt(eventInput2[4]))
            .genderRequirement(EventGenderRequirement.fromString(eventInput2[5]))
            .build();
    eventService.createEvent(eventCreateRequest2);

    String[] input = {"John", "Doe", "john@test.com", "25", "MALE"};

    ParticipantCreateRequest participantCreateRequest =
        ParticipantCreateRequest.builder()
            .firstName(input[0])
            .lastName(input[1])
            .email(input[2])
            .age(Integer.parseInt(input[3]))
            .participantGender(ParticipantGender.fromString(input[4]))
            .build();

    eventService.createParticipant(participantCreateRequest);

    String[] input2 = {"Jane", "Smith", "jane@test.com", "25", "FEMALE"};

    ParticipantCreateRequest participantCreateRequest2 =
        ParticipantCreateRequest.builder()
            .firstName(input2[0])
            .lastName(input2[1])
            .email(input2[2])
            .age(Integer.parseInt(input2[3]))
            .participantGender(ParticipantGender.fromString(input2[4]))
            .build();

    eventService.createParticipant(participantCreateRequest2);

    int[] regInput = {1, 1};

    EventRegRequest eventRegRequest =
        EventRegRequest.builder().participantId(regInput[0]).eventId(regInput[1]).build();

    eventService.registerParticipant(eventRegRequest);

    int[] regInput2 = {2, 1};

    EventRegRequest eventRegRequest2 =
        EventRegRequest.builder().participantId(regInput2[0]).eventId(regInput2[1]).build();

    eventService.registerParticipant(eventRegRequest2);

    int[] regInput3 = {2, 2};

    EventRegRequest eventRegRequest3 =
        EventRegRequest.builder().participantId(regInput3[0]).eventId(regInput3[1]).build();

    eventService.registerParticipant(eventRegRequest3);
  }

  @Test
  public void givenSetupData_WhenGetParticipantById_ThenGetParticipant() {
    ParticipantResponse participantResponse = eventService.getParticipantById(1);

    assertEquals("John", participantResponse.getFirstName());
    assertEquals("Doe", participantResponse.getLastName());
    assertEquals("john@test.com", participantResponse.getEmail());
    assertEquals(25, participantResponse.getAge());
    assertEquals(ParticipantGender.MALE, participantResponse.getParticipantGender());

    ParticipantResponse participantResponse2 = eventService.getParticipantById(2);
    assertEquals("Jane", participantResponse2.getFirstName());
    assertEquals("Smith", participantResponse2.getLastName());
    assertEquals("jane@test.com", participantResponse2.getEmail());
    assertEquals(25, participantResponse2.getAge());
    assertEquals(ParticipantGender.FEMALE, participantResponse2.getParticipantGender());
  }

  @Test
  public void givenInvalidData_WhenGetParticipantById_ThenGetNotFound() {
    ParticipantResponse participantResponse = eventService.getParticipantById(-1);
    assertEquals("NOT FOUND", participantResponse.getFirstName());

    assertDoesNotThrow(() -> eventService.getParticipantById(-872332331));
    assertDoesNotThrow(() -> eventService.getParticipantById(872332331));
  }

  @Test
  public void givenSetupData_WhenGetParticipants_ThenGetParticipants() {
    List<ParticipantResponse> participantResponses = eventService.getParticipants();

    assertEquals(2, participantResponses.size());

    ParticipantResponse participantResponse = participantResponses.get(0);

    assertEquals("John", participantResponse.getFirstName());
    assertEquals("Doe", participantResponse.getLastName());
    assertEquals("john@test.com", participantResponse.getEmail());
    assertEquals(25, participantResponse.getAge());
    assertEquals(ParticipantGender.MALE, participantResponse.getParticipantGender());

    ParticipantResponse participantResponse2 = participantResponses.get(1);
    assertEquals("Jane", participantResponse2.getFirstName());
    assertEquals("Smith", participantResponse2.getLastName());
    assertEquals("jane@test.com", participantResponse2.getEmail());
    assertEquals(25, participantResponse2.getAge());
    assertEquals(ParticipantGender.FEMALE, participantResponse2.getParticipantGender());
  }

  @Test
  public void givenSetupData_WhenGetEventById_ThenGetEvent() {
    EventResponse eventResponse = eventService.getEventById(1);

    assertEquals("Test event", eventResponse.getEventName());
    assertEquals(OffsetDateTime.parse("2030-01-01T12:00:00+00:00"), eventResponse.getEventDate());
    assertEquals(Duration.ofHours(10), eventResponse.getEventDuration());
    assertEquals(1, eventResponse.getAgeRequired());
    assertEquals(100, eventResponse.getMaxParticipantAmount());
    assertEquals(EventGenderRequirement.NONE, eventResponse.getEventGenderRequirement());

    EventResponse eventResponse2 = eventService.getEventById(2);

    assertEquals("Test event two", eventResponse2.getEventName());
    assertEquals(OffsetDateTime.parse("2040-01-01T12:00:00+00:00"), eventResponse2.getEventDate());
    assertEquals(Duration.ofHours(100), eventResponse2.getEventDuration());
    assertEquals(10, eventResponse2.getAgeRequired());
    assertEquals(10, eventResponse2.getMaxParticipantAmount());
    assertEquals(EventGenderRequirement.NONE, eventResponse2.getEventGenderRequirement());
  }

  @Test
  public void givenInvalidData_WhenGetEventById_ThenGetNotFound() {
    EventResponse eventResponse = eventService.getEventById(-1);
    assertEquals("NOT FOUND", eventResponse.getEventName());

    assertDoesNotThrow(() -> eventService.getEventById(-872332331));
    assertDoesNotThrow(() -> eventService.getEventById(872332331));
  }

  @Test
  public void givenSetupData_WhenGetEvents_ThenGetEvents() {
    List<EventResponse> eventResponses = eventService.getEvents();

    assertEquals(2, eventResponses.size());

    EventResponse eventResponse = eventResponses.get(0);

    assertEquals("Test event", eventResponse.getEventName());
    assertEquals(OffsetDateTime.parse("2030-01-01T12:00:00+00:00"), eventResponse.getEventDate());
    assertEquals(Duration.ofHours(10), eventResponse.getEventDuration());
    assertEquals(1, eventResponse.getAgeRequired());
    assertEquals(100, eventResponse.getMaxParticipantAmount());
    assertEquals(EventGenderRequirement.NONE, eventResponse.getEventGenderRequirement());

    EventResponse eventResponse2 = eventResponses.get(1);

    assertEquals("Test event two", eventResponse2.getEventName());
    assertEquals(OffsetDateTime.parse("2040-01-01T12:00:00+00:00"), eventResponse2.getEventDate());
    assertEquals(Duration.ofHours(100), eventResponse2.getEventDuration());
    assertEquals(10, eventResponse2.getAgeRequired());
    assertEquals(10, eventResponse2.getMaxParticipantAmount());
    assertEquals(EventGenderRequirement.NONE, eventResponse2.getEventGenderRequirement());
  }

  @Test
  public void givenSetupData_WhenGetRegistrationRequestById_ThenGetRegistrationRequest() {
    EventRegResponse eventRegResponse = eventService.getRegistrationRequestById(1);

    assertEquals("John", eventRegResponse.getFirstName());
    assertEquals("Doe", eventRegResponse.getLastName());
    assertEquals(EventRegRequestStatus.ACCEPTED, eventRegResponse.getEventRegRequestStatus());

    EventRegResponse eventRegResponse2 = eventService.getRegistrationRequestById(2);

    assertEquals("Jane", eventRegResponse2.getFirstName());
    assertEquals("Smith", eventRegResponse2.getLastName());
    assertEquals(EventRegRequestStatus.ACCEPTED, eventRegResponse2.getEventRegRequestStatus());

    EventRegResponse eventRegResponse3 = eventService.getRegistrationRequestById(3);

    assertEquals("Jane", eventRegResponse3.getFirstName());
    assertEquals("Smith", eventRegResponse3.getLastName());
    assertEquals(EventRegRequestStatus.ACCEPTED, eventRegResponse3.getEventRegRequestStatus());
  }

  @Test
  public void givenInvalidData_WhenGetRegistrationRequestById_ThenGetNotFound() {
    EventRegResponse eventRegResponse = eventService.getRegistrationRequestById(-1);
    assertEquals(EventRegRequestStatus.NOT_FOUND, eventRegResponse.getEventRegRequestStatus());

    assertDoesNotThrow(() -> eventService.getRegistrationRequestById(-872332331));
    assertDoesNotThrow(() -> eventService.getRegistrationRequestById(872332331));
  }

  @Test
  public void givenSetupData_WhenGetRegistrationRequests_ThenGetRegistrationRequests() {
    List<EventRegResponse> eventRegResponses = eventService.getRegistrationRequests();

    EventRegResponse eventRegResponse = eventRegResponses.get(0);

    assertEquals("John", eventRegResponse.getFirstName());
    assertEquals("Doe", eventRegResponse.getLastName());
    assertEquals(EventRegRequestStatus.ACCEPTED, eventRegResponse.getEventRegRequestStatus());

    EventRegResponse eventRegResponse2 = eventRegResponses.get(1);

    assertEquals("Jane", eventRegResponse2.getFirstName());
    assertEquals("Smith", eventRegResponse2.getLastName());
    assertEquals(EventRegRequestStatus.ACCEPTED, eventRegResponse2.getEventRegRequestStatus());

    EventRegResponse eventRegResponse3 = eventRegResponses.get(2);

    assertEquals("Jane", eventRegResponse3.getFirstName());
    assertEquals("Smith", eventRegResponse3.getLastName());
    assertEquals(EventRegRequestStatus.ACCEPTED, eventRegResponse3.getEventRegRequestStatus());
  }
}
