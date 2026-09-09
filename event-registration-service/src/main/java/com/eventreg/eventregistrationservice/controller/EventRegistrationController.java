package com.eventreg.eventregistrationservice.controller;

import com.eventreg.eventregistrationservice.annotation.swagger.CollectionErrors;
import com.eventreg.eventregistrationservice.annotation.swagger.EventRegistrationCreatedDto;
import com.eventreg.eventregistrationservice.annotation.swagger.EventRegistrationResponseDto;
import com.eventreg.eventregistrationservice.annotation.swagger.NoContentResponseDto;
import com.eventreg.eventregistrationservice.annotation.swagger.PageResponseDto;
import com.eventreg.eventregistrationservice.annotation.swagger.ResourceErrors;
import com.eventreg.eventregistrationservice.annotation.swagger.StandardErrors;
import com.eventreg.eventregistrationservice.annotation.swagger.StandardWriteErrors;
import com.eventreg.eventregistrationservice.dto.request.create.EventRegistrationCreateRequest;
import com.eventreg.eventregistrationservice.dto.request.update.EventRegistrationStatusUpdateRequest;
import com.eventreg.eventregistrationservice.dto.response.EventRegistrationResponse;
import com.eventreg.eventregistrationservice.mapper.EventRegistrationMapper;
import com.eventreg.eventregistrationservice.model.enums.EventRegistrationStatus;
import com.eventreg.eventregistrationservice.security.SecurityGuard;
import com.eventreg.eventregistrationservice.service.EventRegistrationService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for managing event registrations. */
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

  /** Creates a new event registration for a participant. */
  @Operation(
      description =
          "Register a participant for an event, deriving status from the event reservation state")
  @EventRegistrationCreatedDto
  @StandardWriteErrors
  @PreAuthorize("hasAuthority('REGISTRATION_CREATE')")
  @PostMapping
  public ResponseEntity<EventRegistrationResponse> createEventRegistration(
      @Parameter(
              description = "Create registration request",
              required = true,
              in = ParameterIn.DEFAULT)
          @Valid
          @RequestBody
          EventRegistrationCreateRequest request,
      @AuthenticationPrincipal(expression = "subject") String keycloakId,
      @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
                eventRegistrationService.createEventRegistration(request)));
  }

  /** Creates a new event registration directly, skipping the pending state. */
  @Operation(description = "Register a participant directly, skipping pending")
  @EventRegistrationCreatedDto
  @StandardErrors
  @PreAuthorize("hasAuthority('PARTICIPANT_ADD_DIRECTLY')")
  @PostMapping("/direct")
  public ResponseEntity<EventRegistrationResponse> createEventRegistrationNoPending(
      @Valid @RequestBody EventRegistrationCreateRequest request,
      @AuthenticationPrincipal(expression = "subject") String keycloakId,
      @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {

    request.setSkipPending(true);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
                eventRegistrationService.createEventRegistration(request)));
  }

  /** Returns a page of event registrations. */
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

  /** Returns a single event registration by its id. */
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

  /** Accepts a participant registration. */
  @Operation(description = "Accept a participant registration")
  @EventRegistrationResponseDto
  @ResourceErrors
  @PreAuthorize(
      "hasAuthority('REGISTRATION_ACCEPT') && @securityGuard.isEventOrganizerForRegistration(#id)")
  @PatchMapping("/{id}/accept")
  public ResponseEntity<EventRegistrationResponse> acceptRegistration(
      @PathVariable Long id,
      @AuthenticationPrincipal(expression = "subject") String keycloakId,
      @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {

    return ResponseEntity.ok(
        eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            eventRegistrationService.changeRegistrationRequestStatus(
                id, EventRegistrationStatus.ACCEPTED, "Registration accepted by organizer")));
  }

  /** Denies a participant registration. */
  @Operation(description = "Deny a participant registration")
  @EventRegistrationResponseDto
  @ResourceErrors
  @PreAuthorize(
      "hasAuthority('REGISTRATION_DENY') && @securityGuard.isEventOrganizerForRegistration(#id)")
  @PatchMapping("/{id}/deny")
  public ResponseEntity<EventRegistrationResponse> denyRegistration(
      @PathVariable Long id,
      @AuthenticationPrincipal(expression = "subject") String keycloakId,
      @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {

    return ResponseEntity.ok(
        eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            eventRegistrationService.changeRegistrationRequestStatus(
                id, EventRegistrationStatus.DENIED, "Registration denied by event organizer")));
  }

  /** Cancels a participant registration. */
  @Operation(description = "Cancel a participant registration")
  @EventRegistrationResponseDto
  @ResourceErrors
  @PreAuthorize("hasAuthority('REGISTRATION_CANCEL') && @securityGuard.isOwnerForRegistration(#id)")
  @PatchMapping("/{id}/cancel")
  public ResponseEntity<EventRegistrationResponse> cancelRegistration(
      @PathVariable Long id,
      @AuthenticationPrincipal(expression = "subject") String keycloakId,
      @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {

    return ResponseEntity.ok(
        eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            eventRegistrationService.changeRegistrationRequestStatus(
                id, EventRegistrationStatus.CANCELLED, "Registration cancelled by participant")));
  }

  /** Changes the status of an event registration with a custom description. */
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
      @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
    return ResponseEntity.ok(
        eventRegistrationMapper.eventRegistrationToEventRegistrationResponse(
            eventRegistrationService.changeRegistrationRequestStatus(
                id,
                changeStatusRequest.getEventRegistrationStatus(),
                changeStatusRequest.getDescription())));
  }

  /** Deletes an event registration by its id. */
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
      @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
    eventRegistrationService.deleteEventRegistration(id);
    return ResponseEntity.noContent().build();
  }
}
