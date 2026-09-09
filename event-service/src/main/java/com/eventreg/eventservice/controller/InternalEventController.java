package com.eventreg.eventservice.controller;

import com.eventreg.eventservice.service.EventService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/events")
@Hidden
@RequiredArgsConstructor
public class InternalEventController {

  private final EventService eventService;

  @GetMapping("/{eventId}/keycloakId")
  public String getOrganizerKeycloakId(@PathVariable Long eventId) {
    return eventService.findById(eventId).getOrganizerKeycloakId();
  }

  @GetMapping("/{eventId}/registration-decision")
  public String getRegistrationDecision(@PathVariable Long eventId) {
    return eventService.decideRegistrationStatus(eventId);
  }
}
