package com.eventreg.participantservice.controller;

import com.eventreg.participantservice.service.ParticipantService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal controller for cross-service participant lookups. */
@RestController
@RequestMapping("/internal/participants")
@Hidden
@RequiredArgsConstructor
public class InternalParticipantController {
  private final ParticipantService participantService;

  /** Returns the Keycloak subject ID for the given participant. */
  @GetMapping("/{participantId}/keycloakId")
  public String getOrganizerKeycloakId(@PathVariable Long participantId) {
    return participantService.findById(participantId).getKeycloakId();
  }
}
