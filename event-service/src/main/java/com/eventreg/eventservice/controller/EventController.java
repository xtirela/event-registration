package com.eventreg.eventservice.controller;

import com.eventreg.eventservice.annotation.swagger.CollectionErrors;
import com.eventreg.eventservice.annotation.swagger.EventCreatedDto;
import com.eventreg.eventservice.annotation.swagger.EventResponseDto;
import com.eventreg.eventservice.annotation.swagger.NoContentResponseDto;
import com.eventreg.eventservice.annotation.swagger.PageResponseDto;
import com.eventreg.eventservice.annotation.swagger.ResourceErrors;
import com.eventreg.eventservice.annotation.swagger.StandardErrors;
import com.eventreg.eventservice.annotation.swagger.StandardWriteErrors;
import com.eventreg.eventservice.dto.request.create.EventCreateRequest;
import com.eventreg.eventservice.dto.request.update.EventUpdateRequest;
import com.eventreg.eventservice.dto.response.EventResponse;
import com.eventreg.eventservice.mapper.EventMapper;
import com.eventreg.eventservice.model.Event;
import com.eventreg.eventservice.security.SecurityGuard;
import com.eventreg.eventservice.service.EventService;
import com.eventreg.eventservice.specification.EventSpecificationCreator;
import com.eventreg.eventservice.specification.Filter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "Event management",
    description =
        "Operations for creating, viewing, updating and deleting events, searching events and downloading event calendars")
@AllArgsConstructor
@RestController
@RequestMapping("/api/events")
public class EventController {
  private final EventSpecificationCreator specificationCreator;
  private final EventService eventService;
  private final SecurityGuard securityGuard;
  private final EventMapper eventMapper;

  @Operation(description = "Create a new event")
  @EventCreatedDto
  @StandardWriteErrors
  @PreAuthorize("hasAuthority('EVENT_CREATE')")
  @PostMapping
  public ResponseEntity<EventResponse> createEvent(
      @Parameter(description = "Create event request", required = true, in = ParameterIn.DEFAULT)
          @Valid
          @RequestBody
          EventCreateRequest eventCreateRequest,
      @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
      @AuthenticationPrincipal(expression = "subject") String organizerKeycloakId,
      @AuthenticationPrincipal(expression = "claims['email']") String organizerEmail) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            eventMapper.eventToEventResponse(
                eventService.createEvent(eventCreateRequest, organizerKeycloakId, organizerEmail)));
  }

  @Operation(description = "Delete an event")
  @NoContentResponseDto
  @ResourceErrors
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('EVENT_DELETE') && @securityGuard.isEventOrganizer(#id)")
  public ResponseEntity<Void> deleteEvent(
      @Parameter(description = "ID of the event to delete", required = true, in = ParameterIn.PATH)
          @PathVariable
          Long id,
      @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
    eventService.deleteEvent(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(description = "Get a page of events")
  @PageResponseDto
  @CollectionErrors
  @PreAuthorize("hasAuthority('EVENT_VIEW')")
  @GetMapping
  public ResponseEntity<Page<EventResponse>> findAll(
      @PageableDefault(
              size = 20,
              sort = "eventDate",
              direction = org.springframework.data.domain.Sort.Direction.DESC)
          Pageable pageable) {
    return ResponseEntity.ok(eventService.findAll(pageable).map(eventMapper::eventToEventResponse));
  }

  @Operation(description = "Search events by filter criteria")
  @PageResponseDto
  @CollectionErrors
  @PreAuthorize("hasAuthority('EVENT_VIEW')")
  @PostMapping("/search")
  public ResponseEntity<Page<EventResponse>> search(
      @Parameter(
              description = "Filter criteria (optional)",
              required = false,
              in = ParameterIn.DEFAULT)
          @RequestBody(required = false)
          Filter filter,
      @PageableDefault(size = 20) Pageable pageable) {
    Specification<Event> spec = specificationCreator.create(filter);
    return ResponseEntity.ok(
        eventService.findAll(spec, pageable).map(eventMapper::eventToEventResponse));
  }

  @Operation(description = "Get an event by its id")
  @EventResponseDto
  @ResourceErrors
  @PreAuthorize("hasAuthority('EVENT_VIEW')")
  @GetMapping("/{id}")
  public ResponseEntity<EventResponse> findById(
      @Parameter(description = "ID of the event to fetch", required = true, in = ParameterIn.PATH)
          @PathVariable
          Long id) {
    return ResponseEntity.ok(eventMapper.eventToEventResponse(eventService.findById(id)));
  }

  @Operation(description = "Update an event by its id")
  @EventResponseDto
  @StandardErrors
  @PreAuthorize("hasAuthority('EVENT_UPDATE') && @securityGuard.isEventOrganizer(#id)")
  @PatchMapping("/{id}")
  public ResponseEntity<EventResponse> updateEvent(
      @Parameter(description = "ID of the event to update", required = true, in = ParameterIn.PATH)
          @PathVariable
          Long id,
      @Parameter(description = "Update event request", required = true, in = ParameterIn.DEFAULT)
          @Valid
          @RequestBody
          EventUpdateRequest eventUpdateRequest,
      @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
    return ResponseEntity.ok(
        eventMapper.eventToEventResponse(eventService.updateEvent(id, eventUpdateRequest)));
  }
}
