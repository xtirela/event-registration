package com.eventreg.eventregistrationservice.controller;

import com.eventreg.eventregistrationservice.model.enums.EventRegistrationStatus;
import com.eventreg.eventregistrationservice.service.EventRegistrationService;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Internal endpoints used by the event service for queue and counter coordination. */
@AllArgsConstructor
@RestController
@RequestMapping("/internal/registrations")
@Hidden
public class InternalEventRegistrationController {
  private final EventRegistrationService eventRegistrationService;

  /** Returns the number of waiting registrations for the given event. */
  @GetMapping("/waiting/{eventId}")
  public ResponseEntity<Long> getCurrentParticipantAmountForEventWithStatusWaiting(
      @PathVariable("eventId") Long eventId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(eventRegistrationService.getCurrentParticipantAmountForEventWaiting(eventId));
  }

  /** Returns the number of accepted registrations for the given event. */
  @GetMapping("/accepted/{eventId}")
  public ResponseEntity<Long> getCurrentParticipantAmountForEventWithStatusAccepted(
      @PathVariable("eventId") Long eventId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(eventRegistrationService.getCurrentParticipantAmountForEventAccepted(eventId));
  }

  /** Returns the waiting queue identifiers for the given event up to the given limit. */
  @GetMapping("/{eventId}/waiting-queue")
  public ResponseEntity<List<Long>> getWaitingQueue(
      @PathVariable("eventId") Long eventId,
      @RequestParam(value = "limit", defaultValue = "100") int limit) {
    return ResponseEntity.ok(eventRegistrationService.getWaitingQueueIds(eventId, limit));
  }

  /** Promotes the given registrations to the provided status and description. */
  @PostMapping("/promote")
  public ResponseEntity<Void> promote(
      @RequestParam("eventId") Long eventId,
      @RequestParam("status") String status,
      @RequestParam("description") String description,
      @RequestBody List<Long> registrationIds) {
    eventRegistrationService.promoteRegistrations(
        eventId, registrationIds, EventRegistrationStatus.fromString(status), description);
    return ResponseEntity.ok().build();
  }
}
