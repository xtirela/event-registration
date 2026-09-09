package com.eventreg.participantservice.controller;

import com.eventreg.participantservice.annotation.swagger.CollectionErrors;
import com.eventreg.participantservice.annotation.swagger.NoContentResponseDto;
import com.eventreg.participantservice.annotation.swagger.PageResponseDto;
import com.eventreg.participantservice.annotation.swagger.ParticipantCreatedDto;
import com.eventreg.participantservice.annotation.swagger.ParticipantResponseDto;
import com.eventreg.participantservice.annotation.swagger.ResourceErrors;
import com.eventreg.participantservice.annotation.swagger.StandardErrors;
import com.eventreg.participantservice.annotation.swagger.StandardWriteErrors;
import com.eventreg.participantservice.dto.request.create.ParticipantCreateRequest;
import com.eventreg.participantservice.dto.request.update.ParticipantUpdateRequest;
import com.eventreg.participantservice.dto.response.ParticipantResponse;
import com.eventreg.participantservice.mapper.ParticipantMapper;
import com.eventreg.participantservice.security.SecurityGuard;
import com.eventreg.participantservice.service.ParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/** REST controller for participant profile management. */
@Tag(
    name = "Participant profile management",
    description = "Operations for creating and managing a user's participant profile")
@Slf4j
@RestController
@RequestMapping("/api/participants")
@AllArgsConstructor
public class ParticipantController {

  private final ParticipantService participantService;
  private final SecurityGuard securityGuard;
  private final ParticipantMapper participantMapper;

  /** Creates a new participant profile for the authenticated user. */
  @Operation(description = "Create a participant profile for a specified user")
  @ParticipantCreatedDto
  @StandardWriteErrors
  @PostMapping
  @PreAuthorize("hasAuthority('PARTICIPANT_CREATE')")
  public ResponseEntity<ParticipantResponse> createParticipant(
      @Parameter(
              description = "Create participant request",
              required = true,
              in = ParameterIn.DEFAULT)
          @Valid
          @RequestBody
          ParticipantCreateRequest participantCreateRequest,
      @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
      @AuthenticationPrincipal(expression = "subject") String keycloakId) {
    log.info("recieved key cock id = {}", keycloakId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            participantMapper.participantToParticipantResponse(
                participantService.createParticipant(participantCreateRequest, keycloakId)));
  }

  /** Returns a paginated list of participant profiles. */
  @Operation(description = "Get a page of participant profiles")
  @PageResponseDto
  @CollectionErrors
  @PreAuthorize("hasAuthority('PARTICIPANT_VIEW')")
  @GetMapping
  public ResponseEntity<Page<ParticipantResponse>> findAll(
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(
            participantService
                .findAll(pageable)
                .map(participantMapper::participantToParticipantResponse));
  }

  /** Retrieves a single participant profile by ID. */
  @Operation(description = "Get a participant profile by its id")
  @ParticipantResponseDto
  @ResourceErrors
  @PreAuthorize("hasAuthority('PARTICIPANT_VIEW')")
  @GetMapping("/{id}")
  public ResponseEntity<ParticipantResponse> findById(
      @Parameter(
              description = "ID of the participant profile to fetch",
              required = true,
              in = ParameterIn.PATH)
          @PathVariable
          Long id) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(participantMapper.participantToParticipantResponse(participantService.findById(id)));
  }

  /** Deletes a participant profile owned by the current user. */
  @Operation(description = "Delete a participant profile by its id")
  @NoContentResponseDto
  @ResourceErrors
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('PARTICIPANT_DELETE') && @securityGuard.isParticipantOwner(#id)")
  public ResponseEntity<Void> deleteParticipant(
      @Parameter(
              description = "ID of the participant profile to delete",
              required = true,
              in = ParameterIn.PATH)
          @PathVariable
          Long id,
      @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
    participantService.deleteParticipant(id);
    return ResponseEntity.noContent().build();
  }

  /** Partially updates a participant profile owned by the current user. */
  @Operation(description = "Update a participant profile by its id")
  @ParticipantResponseDto
  @StandardErrors
  @PatchMapping("/{id}")
  @PreAuthorize("hasAuthority('PARTICIPANT_UPDATE')  && @securityGuard.isParticipantOwner(#id)")
  public ResponseEntity<ParticipantResponse> updateParticipant(
      @Parameter(
              description = "ID of the participant profile to update",
              required = true,
              in = ParameterIn.PATH)
          @PathVariable
          Long id,
      @Parameter(
              description = "Update participant request",
              required = true,
              in = ParameterIn.DEFAULT)
          @Valid
          @RequestBody
          ParticipantUpdateRequest participantUpdateRequest,
      @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(
            participantMapper.participantToParticipantResponse(
                participantService.updateParticipant(id, participantUpdateRequest)));
  }
}
