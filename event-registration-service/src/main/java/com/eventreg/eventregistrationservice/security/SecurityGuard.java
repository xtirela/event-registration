package com.eventreg.eventregistrationservice.security;

import com.eventreg.eventregistrationservice.exception.EventRegistrationNotFoundException;
import com.eventreg.eventregistrationservice.feign.EventClient;
import com.eventreg.eventregistrationservice.feign.ParticipantClient;
import com.eventreg.eventregistrationservice.model.EventRegistration;
import com.eventreg.eventregistrationservice.repository.EventRegistrationRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Resolves ownership and role checks for registration endpoints. */
@Component("securityGuard")
@RequiredArgsConstructor
public class SecurityGuard {

  private final EventClient eventClient;
  private final ParticipantClient participantClient;

  private final EventRegistrationRepository eventRegistrationRepository;

  /** Returns whether the current user is the owner of the given participant. */
  public boolean isParticipantOwner(Long participantId) {
    String currentKeycloakId = getCurrentKeycloakId();
    String participantKeycloakId = participantClient.getKeycloakId(participantId);
    return participantKeycloakId.equals(currentKeycloakId);
  }

  /** Returns whether the current user organizes the event of the given registration. */
  public boolean isEventOrganizerForRegistration(Long registrationId) {
    String currentKeycloakId = getCurrentKeycloakId();

    EventRegistration registration =
        eventRegistrationRepository
            .findById(registrationId)
            .orElseThrow(
                () ->
                    new EventRegistrationNotFoundException(
                        registrationId, "isEventOrganizerForRegistration"));

    String organizerKeycloakId = eventClient.getOrganizerKeycloakId(registration.getEventId());
    return organizerKeycloakId.equals(currentKeycloakId);
  }

  /** Returns whether the current user owns the given registration. */
  public boolean isOwnerForRegistration(Long registrationId) {
    String currentKeycloakId = getCurrentKeycloakId();

    EventRegistration registration =
        eventRegistrationRepository
            .findById(registrationId)
            .orElseThrow(
                () ->
                    new EventRegistrationNotFoundException(
                        registrationId, "isOwnerForRegistration"));

    String participantKeycloakId = participantClient.getKeycloakId(registration.getParticipantId());
    return participantKeycloakId.equals(currentKeycloakId);
  }

  /** Returns whether the current user has the given role. */
  public boolean hasRole(String role) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null
        && auth.getAuthorities().stream()
            .anyMatch(
                a ->
                    Objects.equals(a.getAuthority(), role)
                        || Objects.equals(a.getAuthority(), "ROLE_" + role));
  }

  private String getCurrentKeycloakId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth.getPrincipal() instanceof Jwt jwt) {
      return jwt.getSubject();
    }
    return auth.getName();
  }
}
