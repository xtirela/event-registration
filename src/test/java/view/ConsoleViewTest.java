package view;

import static org.junit.jupiter.api.Assertions.*;

import dto.EventRegResponse;
import dto.EventResponse;
import dto.ParticipantResponse;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Scanner;
import model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import service.EventService;
import service.implementation.EventServiceImpl;

public class ConsoleViewTest {
  private EventService eventService;
  private ConsoleView consoleView;

  @BeforeEach
  void setUP() {
    eventService = new EventServiceImpl();
    consoleView = new ConsoleView(eventService);
  }

  void performConsoleAction(int action, String input) {
    System.setIn(new ByteArrayInputStream(input.getBytes()));

    Scanner scanner = new Scanner(System.in);

    consoleView.performAction(action, scanner);
  }

  ParticipantResponse expectedParticipantResponse(String output) {
    String[] expected = output.split(",");

    return ParticipantResponse.builder()
        .firstName(expected[0])
        .lastName(expected[1])
        .email(expected[2])
        .age(Integer.parseInt(expected[3]))
        .participantGender(ParticipantGender.fromString(expected[4]))
        .build();
  }

  EventResponse expectedEventResponse(String output) {
    String[] expected = output.split(",");

    return EventResponse.builder()
        .eventName(expected[0])
        .eventDate(OffsetDateTime.parse(expected[1]))
        .eventDuration(Duration.ofHours(Long.parseLong(expected[2])))
        .ageRequired(Integer.parseInt(expected[3]))
        .currentParticipantAmount(Integer.parseInt(expected[4]))
        .maxParticipantAmount(Integer.parseInt(expected[5]))
        .eventGenderRequirement(EventGenderRequirement.fromString(expected[6]))
        .eventStatus(EventStatus.fromString(expected[7]))
        .eventRegistrationStatus(EventRegistrationStatus.fromString(expected[8]))
        .build();
  }

  EventRegResponse expectedEventRegResponse(String output) {
    String[] expected = output.split(",");

    return EventRegResponse.builder()
        .firstName(expected[0])
        .lastName(expected[1])
        .email(expected[2])
        .age(Integer.parseInt(expected[3]))
        .participantGender(ParticipantGender.fromString(expected[4]))
        .eventName((expected[5]))
        .eventRegRequestStatus(EventRegRequestStatus.fromString(expected[6]))
        .description("none")
        .build();
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/participant_create_request_valid.csv", numLinesToSkip = 1)
  public void givenValidData_WhenCreateParticipant_ThenReturnParticipantResponse(
      String input, String output) {
    performConsoleAction(1, input);

    ParticipantResponse expectedResponse = expectedParticipantResponse(output);

    ParticipantResponse actualResponse = eventService.getParticipantById(1);

    assertEquals(expectedResponse, actualResponse);
  }

  @ParameterizedTest
  @CsvFileSource(
      resources = "/view/csv/participant_create_request_edge_cases.csv",
      numLinesToSkip = 1)
  public void givenEdgeCasesData_WhenCreateParticipant_ThenReturnParticipantResponse(
      String input, String output) {
    performConsoleAction(1, input);

    ParticipantResponse expectedResponse = expectedParticipantResponse(output);

    ParticipantResponse actualResponse = eventService.getParticipantById(1);

    assertEquals(expectedResponse, actualResponse);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/participant_create_request_invalid.csv", numLinesToSkip = 1)
  public void givenInvalidData_WhenCreateParticipant_ThenReturnParticipantResponse(
      String input, String output) {
    performConsoleAction(1, input);

    ParticipantResponse expectedResponse = expectedParticipantResponse(output);

    ParticipantResponse actualResponse = eventService.getParticipantById(1);

    assertNotEquals(expectedResponse, actualResponse);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/event_create_request_valid.csv", numLinesToSkip = 1)
  public void givenValidData_WhenCreateEvent_ThenReturnEventResponse(String input, String output) {
    performConsoleAction(2, input);

    EventResponse expectedResponse = expectedEventResponse(output);

    EventResponse actualResponse = eventService.getEventById(1);

    assertEquals(expectedResponse, actualResponse);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/event_create_request_edge_cases.csv", numLinesToSkip = 1)
  public void givenEdgeCaseData_WhenCreateEvent_ThenReturnEventResponse(
      String input, String output) {
    performConsoleAction(2, input);

    EventResponse expectedResponse = expectedEventResponse(output);

    EventResponse actualResponse = eventService.getEventById(1);

    assertEquals(expectedResponse, actualResponse);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/event_create_request_invalid.csv", numLinesToSkip = 1)
  public void givenInvalidData_WhenCreateEvent_ThenReturnEventResponseWithError(
      String input, String output) {
    performConsoleAction(2, input);

    EventResponse expectedResponse = expectedEventResponse(output);

    EventResponse actualResponse = eventService.getEventById(1);

    assertNotEquals(expectedResponse, actualResponse);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/event_reg_request_valid.csv", numLinesToSkip = 1)
  public void givenValidData_WhenRegisterParticipant_ThenReturnEventRegResponse(
      String participantCreateInput,
      String eventCreateInput,
      String eventRegInput,
      String eventRegOutput) {
    performConsoleAction(1, participantCreateInput);

    performConsoleAction(2, eventCreateInput);

    performConsoleAction(3, eventRegInput);

    EventRegResponse expectedResponse = expectedEventRegResponse(eventRegOutput);

    EventRegResponse actualResponse = eventService.getRegistrationRequestById(1);
    actualResponse.setDescription("none");

    assertEquals(expectedResponse, actualResponse);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/event_reg_request_edge_cases.csv", numLinesToSkip = 1)
  public void givenEdgeCaseData_WhenRegisterParticipant_ThenReturnEventRegResponse(
      String participantCreateInput,
      String eventCreateInput,
      String eventRegInput,
      String eventRegOutput) {
    performConsoleAction(1, participantCreateInput);

    performConsoleAction(2, eventCreateInput);

    performConsoleAction(3, eventRegInput);

    EventRegResponse expectedResponse = expectedEventRegResponse(eventRegOutput);

    EventRegResponse actualResponse = eventService.getRegistrationRequestById(1);
    actualResponse.setDescription("none");

    assertEquals(expectedResponse, actualResponse);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/event_reg_request_invalid.csv", numLinesToSkip = 1)
  public void givenInvalidData_WhenRegisterParticipant_ThenReturnEventRegResponseError(
      String participantCreateInput, String eventCreateInput, String eventRegInput) {
    performConsoleAction(1, participantCreateInput);

    performConsoleAction(2, eventCreateInput);

    performConsoleAction(3, eventRegInput);

    assertDoesNotThrow(() -> eventService.getRegistrationRequestById(1));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/cancel_reg_request_valid.csv", numLinesToSkip = 1)
  public void givenValidData_WhenCancelRegistration_ThenRegistrationCancelled(
      String participantCreateInput,
      String eventCreateInput,
      String eventRegInput,
      String cancelRegInput,
      String eventRegOutput)
      throws InterruptedException {
    performConsoleAction(1, participantCreateInput);

    performConsoleAction(2, eventCreateInput);

    EventResponse response1 = eventService.getEventById(1);
    assertEquals(0, response1.getCurrentParticipantAmount());

    performConsoleAction(3, eventRegInput);

    EventResponse response2 = eventService.getEventById(1);
    assertEquals(1, response2.getCurrentParticipantAmount());

    performConsoleAction(4, cancelRegInput);

    EventRegResponse expectedResponse = expectedEventRegResponse(eventRegOutput);

    EventRegResponse actualResponse = eventService.getRegistrationRequestById(1);
    actualResponse.setDescription("none");

    assertEquals(expectedResponse, actualResponse);

    EventResponse response3 = eventService.getEventById(1);
    assertEquals(0, response3.getCurrentParticipantAmount());
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/cancel_reg_request_edge_cases.csv", numLinesToSkip = 1)
  public void givenEdgeCaseData_WhenCancelRegistration_ThenRegistrationCancelled(
      String participantCreateInput,
      String eventCreateInput,
      String eventRegInput,
      String cancelRegInput,
      String eventRegOutput)
      throws InterruptedException {
    performConsoleAction(1, participantCreateInput);

    performConsoleAction(2, eventCreateInput);

    EventResponse response1 = eventService.getEventById(1);
    assertEquals(0, response1.getCurrentParticipantAmount());

    performConsoleAction(3, eventRegInput);

    EventResponse response2 = eventService.getEventById(1);
    assertEquals(1, response2.getCurrentParticipantAmount());

    performConsoleAction(4, cancelRegInput);

    EventRegResponse expectedResponse = expectedEventRegResponse(eventRegOutput);

    EventRegResponse actualResponse = eventService.getRegistrationRequestById(1);
    actualResponse.setDescription("none");

    assertEquals(expectedResponse, actualResponse);

    EventResponse response3 = eventService.getEventById(1);
    assertEquals(0, response3.getCurrentParticipantAmount());
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/cancel_reg_request_invalid.csv", numLinesToSkip = 1)
  public void givenInvalidData_WhenCancelRegistration_ThenRegistrationNotCancelled(
      String participantCreateInput,
      String eventCreateInput,
      String eventRegInput,
      String cancelRegInput,
      String eventRegOutput)
      throws InterruptedException {
    performConsoleAction(1, participantCreateInput);

    performConsoleAction(2, eventCreateInput);

    performConsoleAction(3, eventRegInput);

    performConsoleAction(4, cancelRegInput);

    EventRegResponse expectedResponse = expectedEventRegResponse(eventRegOutput);

    EventRegResponse actualResponse = eventService.getRegistrationRequestById(1);
    actualResponse.setDescription("none");

    assertNotEquals(expectedResponse, actualResponse);
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/get_event_by_id_valid.csv", numLinesToSkip = 1)
  public void givenValidData_WhenGetEventById_ThenReturnEvent(
      String eventCreateInput, String eventIdInput) {
    performConsoleAction(2, eventCreateInput);

    assertDoesNotThrow(() -> performConsoleAction(5, eventIdInput));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/get_event_by_id_invalid.csv", numLinesToSkip = 1)
  public void givenInvalidData_WhenGetEventById_ThenReturnError(
      String eventCreateInput, String eventIdInput) {
    performConsoleAction(2, eventCreateInput);

    assertDoesNotThrow(() -> performConsoleAction(5, eventIdInput));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/get_all_events_valid.csv", numLinesToSkip = 1)
  public void givenValidData_WhenGetAllEvents_ThenReturnEventList(
      String eventCreateInput1, String eventCreateInput2, String expectedOutput) {
    performConsoleAction(2, eventCreateInput1);
    performConsoleAction(2, eventCreateInput2);
    assertDoesNotThrow(() -> performConsoleAction(6, ""));

    List<EventResponse> events = eventService.getEvents();

    assertEquals(2, events.size());
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/get_participant_by_id_valid.csv", numLinesToSkip = 1)
  public void givenValidData_WhenGetParticipantById_ThenReturnParticipant(
      String participantCreateInput, String participantIdInput, String expectedOutput) {
    performConsoleAction(1, participantCreateInput);

    assertDoesNotThrow(() -> performConsoleAction(7, participantIdInput));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/get_participant_by_id_invalid.csv", numLinesToSkip = 1)
  public void givenInvalidData_WhenGetParticipantById_ThenReturnError(
      String participantCreateInput, String participantIdInput, String expectedOutput) {
    performConsoleAction(1, participantCreateInput);

    assertDoesNotThrow(() -> performConsoleAction(7, participantIdInput));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/get_all_participants_valid.csv", numLinesToSkip = 1)
  public void givenValidData_WhenGetAllParticipants_ThenReturnParticipantList(
      String participantCreateInput1, String participantCreateInput2, String expectedOutput) {
    performConsoleAction(1, participantCreateInput1);
    performConsoleAction(1, participantCreateInput2);
    assertDoesNotThrow(() -> performConsoleAction(8, ""));

    List<ParticipantResponse> participants = eventService.getParticipants();

    assertNotNull(participants);
    assertEquals(2, participants.size());
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/get_registration_by_id_valid.csv", numLinesToSkip = 1)
  public void givenValidData_WhenGetRegistrationById_ThenReturnRegistration(
      String participantCreateInput,
      String eventCreateInput,
      String eventRegInput,
      String requestIdInput,
      String expectedOutput) {
    performConsoleAction(1, participantCreateInput);
    performConsoleAction(2, eventCreateInput);
    performConsoleAction(3, eventRegInput);

    assertDoesNotThrow(() -> performConsoleAction(9, requestIdInput));
  }

  // Case 9: Get Registration Request By ID - Invalid
  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/get_registration_by_id_invalid.csv", numLinesToSkip = 1)
  public void givenInvalidData_WhenGetRegistrationById_ThenReturnError(
      String participantCreateInput,
      String eventCreateInput,
      String eventRegInput,
      String requestIdInput,
      String expectedOutput) {
    performConsoleAction(1, participantCreateInput);
    performConsoleAction(2, eventCreateInput);
    performConsoleAction(3, eventRegInput);

    assertDoesNotThrow(() -> performConsoleAction(9, requestIdInput));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/get_all_registrations_valid.csv", numLinesToSkip = 1)
  public void givenValidData_WhenGetAllRegistrations_ThenReturnRegistrationList(
      String participantCreateInput1,
      String eventCreateInput1,
      String eventRegInput1,
      String participantCreateInput2,
      String eventCreateInput2,
      String eventRegInput2,
      String expectedOutput) {
    performConsoleAction(1, participantCreateInput1);
    performConsoleAction(2, eventCreateInput1);
    performConsoleAction(3, eventRegInput1);
    performConsoleAction(1, participantCreateInput2);
    performConsoleAction(2, eventCreateInput2);
    performConsoleAction(3, eventRegInput2);

    assertDoesNotThrow(() -> performConsoleAction(10, ""));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/get_participant_events_valid.csv", numLinesToSkip = 1)
  public void givenValidData_WhenGetParticipantEvents_ThenReturnEventList(
      String participantCreateInput,
      String eventCreateInput1,
      String eventRegInput1,
      String eventCreateInput2,
      String eventRegInput2,
      String participantIdInput,
      String expectedOutput) {
    performConsoleAction(1, participantCreateInput);
    performConsoleAction(2, eventCreateInput1);
    performConsoleAction(3, eventRegInput1);
    performConsoleAction(2, eventCreateInput2);
    performConsoleAction(3, eventRegInput2);
    assertDoesNotThrow(() -> performConsoleAction(11, participantIdInput));
  }

  @ParameterizedTest
  @CsvFileSource(resources = "/view/csv/get_participant_events_invalid.csv", numLinesToSkip = 1)
  public void givenInvalidData_WhenGetParticipantEvents_ThenReturnEmptyList(
      String participantCreateInput,
      String eventCreateInput,
      String eventRegInput,
      String participantIdInput,
      String expectedOutput) {
    performConsoleAction(1, participantCreateInput);
    performConsoleAction(2, eventCreateInput);
    performConsoleAction(3, eventRegInput);
    assertDoesNotThrow(() -> performConsoleAction(11, participantIdInput));
  }

  @ParameterizedTest
  @CsvFileSource(
      resources = "/view/csv/get_all_registered_participants_valid.csv",
      numLinesToSkip = 1)
  public void givenValidData_WhenGetAllRegisteredParticipants_ThenReturnAllPairs(
      String participantCreateInput1,
      String eventCreateInput1,
      String eventRegInput1,
      String participantCreateInput2,
      String eventCreateInput2,
      String eventRegInput2,
      String expectedOutput) {
    performConsoleAction(1, participantCreateInput1);
    performConsoleAction(2, eventCreateInput1);
    performConsoleAction(3, eventRegInput1);
    performConsoleAction(1, participantCreateInput2);
    performConsoleAction(2, eventCreateInput2);
    performConsoleAction(3, eventRegInput2);
    assertDoesNotThrow(() -> performConsoleAction(12, ""));
  }
}
