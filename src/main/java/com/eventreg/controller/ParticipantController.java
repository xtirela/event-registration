package com.eventreg.controller;

import com.eventreg.annotation.swagger.CollectionErrors;
import com.eventreg.annotation.swagger.NoContentResponseDto;
import com.eventreg.annotation.swagger.PageResponseDto;
import com.eventreg.annotation.swagger.ParticipantCreatedDto;
import com.eventreg.annotation.swagger.ParticipantResponseDto;
import com.eventreg.annotation.swagger.ResourceErrors;
import com.eventreg.annotation.swagger.StandardErrors;
import com.eventreg.annotation.swagger.StandardWriteErrors;
import com.eventreg.dto.request.create.ParticipantCreateRequest;
import com.eventreg.dto.request.update.ParticipantUpdateRequest;
import com.eventreg.dto.response.ParticipantResponse;
import com.eventreg.mapper.ParticipantMapper;
import com.eventreg.security.SecurityGuard;
import com.eventreg.service.ParticipantService;
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
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Participant profile management",
    description =
        "Operations relater to creating a profile for a user/performing operations with the profile")
@Slf4j
@RestController
@RequestMapping("/api/participants")
@AllArgsConstructor
public class ParticipantController {

  private final ParticipantService participantService;
  private final SecurityGuard securityGuard;
  private final ParticipantMapper participantMapper;

  @Operation(description = "Create a participant profile for a specified user")
  @ParticipantCreatedDto
  @StandardWriteErrors
  @PostMapping
  @PreAuthorize(
      "hasAuthority('PARTICIPANT_CREATE') && @securityGuard.isSelf(#participantCreateRequest.getUserId())")
  public ResponseEntity<ParticipantResponse> createParticipant(
      @Parameter(
              description = "Create participant request",
              required = true,
              in = ParameterIn.DEFAULT)
          @Valid
          @RequestBody
          ParticipantCreateRequest participantCreateRequest,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            participantMapper.participantToParticipantResponse(
                participantService.createParticipant(participantCreateRequest)));
  }

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

  //  @PreAuthorize("hasAuthority('PARTICIPANT_VIEW')")
  //  @PostMapping("/search")
  //  public ResponseEntity<Page<ParticipantResponse>> search(
  //      @Valid @RequestBody(required = false) Filter filter,
  //      @PageableDefault(size = 20) Pageable pageable) {
  //    Specification<Participant> spec = specificationCreator.create(filter);
  //
  //    return ResponseEntity.status(HttpStatus.OK)
  //        .body(
  //            participantService
  //                .findAll(spec, pageable)
  //                .map(participantMapper::participantToParticipantResponse));
  //  }

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

  @Operation(description = "Delete a participant profile by its id")
  @NoContentResponseDto
  @ResourceErrors
  // TODO: добавить бин-guard что user_id = participant_user_id
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('PARTICIPANT_DELETE') && @securityGuard.isParticipantOwner(#id)")
  public ResponseEntity<Void> deleteParticipant(
      @Parameter(
              description = "ID of the participant profile to delete",
              required = true,
              in = ParameterIn.PATH)
          @PathVariable
          Long id,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    participantService.deleteParticipant(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(description = "Update a participant profile by its id")
  @ParticipantResponseDto
  @StandardErrors
  // TODO: добавить бин-guard что user_id = participant_user_id
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
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(
            participantMapper.participantToParticipantResponse(
                participantService.updateParticipant(id, participantUpdateRequest)));
  }
}
