package com.eventreg.eventservice.security;

import com.eventreg.eventservice.exception.EventNotFoundException;
import com.eventreg.eventservice.model.Event;
import com.eventreg.eventservice.repository.EventRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("securityGuard")
@RequiredArgsConstructor
public class SecurityGuard {

  private final EventRepository eventRepository;

  public boolean isEventOrganizer(Long eventId) {
    String currentKeycloakId = getCurrentKeycloakId();

    Event event =
        eventRepository
            .findById(eventId)
            .orElseThrow(() -> new EventNotFoundException(eventId, "isEventOrganizer"));

    return event.getOrganizerKeycloakId().equals(currentKeycloakId);
  }

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
