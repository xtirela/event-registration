package com.eventreg.security;

import com.eventreg.exception.UserNotFoundException;
import com.eventreg.model.Event;
import com.eventreg.model.EventRegistration;
import com.eventreg.model.Participant;
import com.eventreg.model.User;
import com.eventreg.model.enums.RBAC.Role;
import com.eventreg.repository.UserRepository;
import com.eventreg.service.EventRegistrationService;
import com.eventreg.service.EventService;
import com.eventreg.service.ParticipantService;
import com.eventreg.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("securityGuard")
@RequiredArgsConstructor
public class SecurityGuard {
  private final ParticipantService participantService;
  private final UserService userService;
  private final EventService eventService;
  private final UserRepository userRepository;
  private final EventRegistrationService eventRegistrationService;

  public boolean isSelf(Long userId) {
    String currentUsername = getCurrentUsername();

    User user =
        userRepository
            .findByUsername(currentUsername)
            .orElseThrow(() -> new UserNotFoundException(userId, "isSelf"));

    if (user.getRole().equals(Role.ADMIN)) {
      return true;
    }

    return user.getId().equals(userId);
  }

  public boolean isParticipantOwner(Long participantId) {
    String currentUsername = getCurrentUsername();

    User user =
        userRepository
            .findByUsername(currentUsername)
            .orElseThrow(() -> new UserNotFoundException(currentUsername, "isSelf"));

    if (user.getRole().equals(Role.ADMIN)) {
      return true;
    }

    Participant participant = participantService.findById(participantId);
    return participant.getUser().getId().equals(user.getId());
  }

  public boolean isEventOrganizer(Long eventId) {
    String currentUsername = getCurrentUsername();

    User user =
        userRepository
            .findByUsername(currentUsername)
            .orElseThrow(() -> new UserNotFoundException(currentUsername, "isEventOrganizer"));

    if (user.getRole().equals(Role.ADMIN)) {
      return true;
    }

    Event event = eventService.findById(eventId);
    return event.getOrganizer().getId().equals(user.getId());
  }

  public boolean isEventOrganizerForRegistration(Long registrationId) {
    String currentUsername = getCurrentUsername();

    User user =
        userRepository
            .findByUsername(currentUsername)
            .orElseThrow(
                () ->
                    new UserNotFoundException(currentUsername, "isEventOrganizerForRegistration"));

    if (user.getRole().equals(Role.ADMIN)) {
      return true;
    }

    EventRegistration eventRegistration = eventRegistrationService.findById(registrationId);
    Event event = eventService.findById(eventRegistration.getEvent().getId());
    return event.getOrganizer().getId().equals(user.getId());
  }

  public boolean isOwnerForRegistration(Long registrationId) {
    String currentUsername = getCurrentUsername();

    User user =
        userRepository
            .findByUsername(currentUsername)
            .orElseThrow(
                () -> new UserNotFoundException(currentUsername, "isOwnerForRegistration"));

    if (user.getRole().equals(Role.ADMIN)) {
      return true;
    }

    EventRegistration eventRegistration = eventRegistrationService.findById(registrationId);

    return eventRegistration.getParticipant().getUser().getId().equals(user.getId());
  }

  private String getCurrentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth.getName();
  }
}
