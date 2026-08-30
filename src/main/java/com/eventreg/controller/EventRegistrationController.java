package com.eventreg.controller;

import com.eventreg.annotation.swagger.CollectionErrors;
import com.eventreg.annotation.swagger.EventRegistrationCreatedDto;
import com.eventreg.annotation.swagger.EventRegistrationResponseDto;
import com.eventreg.annotation.swagger.NoContentResponseDto;
import com.eventreg.annotation.swagger.PageResponseDto;
import com.eventreg.annotation.swagger.ResourceErrors;
import com.eventreg.annotation.swagger.StandardErrors;
import com.eventreg.annotation.swagger.StandardWriteErrors;
import com.eventreg.dto.request.create.EventRegistrationCreateRequest;
import com.eventreg.dto.request.update.EventRegistrationStatusUpdateRequest;
import com.eventreg.dto.response.EventRegistrationResponse;
import com.eventreg.mapper.EventRegistrationMapper;
import com.eventreg.model.enums.EventRegistrationStatus;
import com.eventreg.security.SecurityGuard;
import com.eventreg.service.EventRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Event registration management",
    description =
        "Operations for registering participants for events and changing their registration status")
@AllArgsConstructor
@RestController
@RequestMapping("/api/registrations")
public class EventRegistrationController {

  private final EventRegistrationService eventRegistrationService;
  private final SecurityGuard securityGuard;
  private final EventRegistrationMapper eventRegistrationMapper;

  @Operation(
      description =
          "Register a participant for an event, the registration status is derived from the event reservation state")
  @EventRegistrationCreatedDto
  @StandardWriteErrors
  @PreAuthorize(
      "hasAuthority('REGISTRATION_CREATE') && @securityGuard.isParticipantOwner(#eventRegistrationCreateRequest.getParticipantId())")
  @PostMapping
  public ResponseEntity<EventRegistrationResponse> createEventRegistration(
      @Parameter(
              description = "Create registration request",
              required = true,
              in = ParameterIn.DEFAULT)
          @Valid
          @RequestBody
          EventRegistrationCreateRequest eventRegistrationCreateRequest,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
                eventRegistrationService.createEventRegistration(eventRegistrationCreateRequest)));
  }

  @Operation(
      description = "Register a participant directly, skipping the pending confirmation state")
  @EventRegistrationCreatedDto
  @StandardErrors
  @PreAuthorize("hasAuthority('PARTICIPANT_ADD_DIRECTLY')")
  @PostMapping("/direct")
  public ResponseEntity<EventRegistrationResponse> createEventRegistrationNoPending(
      @Parameter(
              description = "Create registration request",
              required = true,
              in = ParameterIn.DEFAULT)
          @Valid
          @RequestBody
          EventRegistrationCreateRequest eventRegistrationCreateRequest,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    eventRegistrationCreateRequest.setSkipPending(true);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
                eventRegistrationService.createEventRegistration(eventRegistrationCreateRequest)));
  }

  @Operation(description = "Get a page of event registrations")
  @PageResponseDto
  @CollectionErrors
  @PreAuthorize("hasAuthority('REGISTRATION_VIEW')")
  @GetMapping
  public ResponseEntity<Page<EventRegistrationResponse>> findAll(
      @PageableDefault(
              size = 20,
              sort = "id",
              direction = org.springframework.data.domain.Sort.Direction.DESC)
          Pageable pageable) {
    return ResponseEntity.ok(
        eventRegistrationService
            .findAll(pageable)
            .map(eventRegistrationMapper::eventRegistrationToEventRegistrationResponse));
  }

  @Operation(description = "Get an event registration by its id")
  @EventRegistrationResponseDto
  @ResourceErrors
  @PreAuthorize("hasAuthority('REGISTRATION_VIEW')")
  @GetMapping("/{id}")
  public ResponseEntity<EventRegistrationResponse> findById(
      @Parameter(
              description = "ID of the registration to fetch",
              required = true,
              in = ParameterIn.PATH)
          @PathVariable
          Long id) {
    return ResponseEntity.ok(
        eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            eventRegistrationService.findById(id)));
  }

  @Operation(description = "Accept a participant registration for an event")
  @EventRegistrationResponseDto
  @ResourceErrors
  @PreAuthorize(
      "hasAuthority('REGISTRATION_ACCEPT') && @securityGuard.isEventOrganizerForRegistration(#id)")
  @PatchMapping("/{id}/accept")
  public ResponseEntity<EventRegistrationResponse> acceptRegistration(
      @Parameter(
              description = "ID of the registration to accept",
              required = true,
              in = ParameterIn.PATH)
          @PathVariable
          Long id) {
    return ResponseEntity.ok(
        eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            eventRegistrationService.changeRegistrationRequestStatus(
                id, EventRegistrationStatus.ACCEPTED, "Registration accepted by organizer")));
  }

  @Operation(description = "Deny a participant registration for an event")
  @EventRegistrationResponseDto
  @ResourceErrors
  @PreAuthorize(
      "hasAuthority('REGISTRATION_DENY') && @securityGuard.isEventOrganizerForRegistration(#id)")
  @PatchMapping("/{id}/deny")
  public ResponseEntity<EventRegistrationResponse> denyRegistration(
      @Parameter(
              description = "ID of the registration to deny",
              required = true,
              in = ParameterIn.PATH)
          @PathVariable
          Long id) {
    return ResponseEntity.ok(
        eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            eventRegistrationService.changeRegistrationRequestStatus(
                id, EventRegistrationStatus.DENIED, "Registration denied by event organizer")));
  }

  @Operation(description = "Cancel a participant registration for an event")
  @EventRegistrationResponseDto
  @ResourceErrors
  @PreAuthorize("hasAuthority('REGISTRATION_CANCEL') && @securityGuard.isOwnerForRegistration(#id)")
  @PatchMapping("/{id}/cancel")
  public ResponseEntity<EventRegistrationResponse> cancelRegistration(
      @Parameter(
              description = "ID of the registration to cancel",
              required = true,
              in = ParameterIn.PATH)
          @PathVariable
          Long id) {
    return ResponseEntity.ok(
        eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            eventRegistrationService.changeRegistrationRequestStatus(
                id, EventRegistrationStatus.CANCELLED, "Registration cancelled by participant")));
  }

  @Operation(description = "Change the status of an event registration with a custom description")
  @EventRegistrationResponseDto
  @StandardErrors
  @PreAuthorize("hasAuthority('REGISTRATION_CHANGE_STATUS')")
  @PatchMapping("/{id}/status")
  public ResponseEntity<EventRegistrationResponse> changeStatus(
      @Parameter(
              description = "ID of the registration to update",
              required = true,
              in = ParameterIn.PATH)
          @PathVariable
          Long id,
      @Parameter(description = "Change status request", required = true, in = ParameterIn.DEFAULT)
          @Valid
          @RequestBody
          EventRegistrationStatusUpdateRequest changeStatusRequest,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.ok(
        eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            eventRegistrationService.changeRegistrationRequestStatus(
                id,
                changeStatusRequest.getEventRegistrationStatus(),
                changeStatusRequest.getDescription())));
  }

  @Operation(description = "Delete an event registration")
  @NoContentResponseDto
  @ResourceErrors
  @PreAuthorize("hasAuthority('REGISTRATION_DELETE')")
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteEventRegistration(
      @Parameter(
              description = "ID of the registration to delete",
              required = true,
              in = ParameterIn.PATH)
          @PathVariable
          Long id,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    eventRegistrationService.deleteEventRegistration(id);
    return ResponseEntity.noContent().build();
  }
}
