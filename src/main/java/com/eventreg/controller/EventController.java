package com.eventreg.controller;

import com.eventreg.annotation.swagger.CollectionErrors;
import com.eventreg.annotation.swagger.EventCreatedDto;
import com.eventreg.annotation.swagger.EventResponseDto;
import com.eventreg.annotation.swagger.IcsFileDto;
import com.eventreg.annotation.swagger.NoContentResponseDto;
import com.eventreg.annotation.swagger.PageResponseDto;
import com.eventreg.annotation.swagger.ResourceErrors;
import com.eventreg.annotation.swagger.StandardErrors;
import com.eventreg.annotation.swagger.StandardWriteErrors;
import com.eventreg.dto.request.create.EventCreateRequest;
import com.eventreg.dto.request.update.EventUpdateRequest;
import com.eventreg.dto.response.EventResponse;
import com.eventreg.mapper.EventMapper;
import com.eventreg.model.Event;
import com.eventreg.security.SecurityGuard;
import com.eventreg.service.EventService;
import com.eventreg.service.implementation.IcsService;
import com.eventreg.specification.EventSpecificationCreator;
import com.eventreg.specification.Filter;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

  private final IcsService icsService;

  @Operation(description = "Download an .ics calendar file for an event")
  @IcsFileDto
  @ResourceErrors
  @GetMapping("/{id}/ics")
  public ResponseEntity<byte[]> downloadIcs(
      @Parameter(
              description = "ID of the event to download the calendar for",
              required = true,
              in = ParameterIn.PATH)
          @PathVariable
          Long id) {
    Event event = eventService.findById(id);

    byte[] icsBytes =
        icsService.generateIcsFile(
            event.getEventName(),
            event.getEventDescription(),
            event.getEventDate().toLocalDateTime(),
            event.getEventDate().plusNanos(event.getEventDuration().toNanos()).toLocalDateTime());

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=event.ics")
        .contentType(MediaType.parseMediaType("text/calendar"))
        .body(icsBytes);
  }

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
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(eventMapper.eventToEventResponse(eventService.createEvent(eventCreateRequest)));
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
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
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
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.ok(
        eventMapper.eventToEventResponse(eventService.updateEvent(id, eventUpdateRequest)));
  }
}
