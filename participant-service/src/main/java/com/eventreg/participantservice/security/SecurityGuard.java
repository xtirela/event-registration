package com.eventreg.participantservice.security;

import com.eventreg.participantservice.exception.ParticipantNotFoundException;
import com.eventreg.participantservice.model.Participant;
import com.eventreg.participantservice.repository.ParticipantRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Helper that checks ownership and roles against the current JWT principal. */
@Component("securityGuard")
@RequiredArgsConstructor
public class SecurityGuard {

  private final ParticipantRepository participantRepository;

  /** Returns true if the current user owns the given participant profile. */
  public boolean isParticipantOwner(Long participantId) {
    String currentKeycloakId = getCurrentKeycloakId();

    Participant participant =
        participantRepository
            .findById(participantId)
            .orElseThrow(
                () -> new ParticipantNotFoundException(participantId, "isParticipantOwner"));

    return participant.getKeycloakId().equals(currentKeycloakId);
  }

  /** Returns true if the current user has the specified role. */
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
