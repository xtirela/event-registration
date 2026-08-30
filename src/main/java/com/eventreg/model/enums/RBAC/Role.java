package com.eventreg.model.enums.RBAC;

import java.util.Set;
import lombok.Getter;

@Getter
public enum Role {
  PARTICIPANT(
      Set.of(
          Permission.EVENT_VIEW,
          Permission.PARTICIPANT_CREATE,
          Permission.PARTICIPANT_VIEW,
          Permission.PARTICIPANT_DELETE,
          Permission.PARTICIPANT_UPDATE,
          Permission.REGISTRATION_CREATE,
          Permission.REGISTRATION_VIEW,
          Permission.REGISTRATION_CANCEL,
          Permission.USER_VIEW)),

  ORGANISER(
      Set.of(
          // Организатор тоже может быть участником (видеть и записываться)
          Permission.EVENT_VIEW,
          Permission.PARTICIPANT_CREATE,
          Permission.PARTICIPANT_VIEW,
          Permission.PARTICIPANT_DELETE,
          Permission.PARTICIPANT_UPDATE,
          Permission.REGISTRATION_CREATE,
          Permission.REGISTRATION_VIEW,
          Permission.REGISTRATION_CANCEL,
          Permission.USER_VIEW,

          // Плюс права организатора
          Permission.EVENT_CREATE,
          Permission.EVENT_DELETE,
          Permission.EVENT_UPDATE,
          Permission.REGISTRATION_ACCEPT,
          Permission.REGISTRATION_DENY,
          Permission.PARTICIPANT_ADD_DIRECTLY)),

  ADMIN(
      Set.of(
          // Админ имеет все права
          Permission.EVENT_VIEW,
          Permission.PARTICIPANT_CREATE,
          Permission.PARTICIPANT_VIEW,
          Permission.PARTICIPANT_DELETE,
          Permission.PARTICIPANT_UPDATE,
          Permission.USER_DELETE,
          Permission.USER_UPDATE,
          Permission.REGISTRATION_CREATE,
          Permission.REGISTRATION_VIEW,
          Permission.REGISTRATION_CANCEL,
          Permission.USER_VIEW,
          Permission.EVENT_CREATE,
          Permission.EVENT_DELETE,
          Permission.EVENT_UPDATE,
          Permission.REGISTRATION_ACCEPT,
          Permission.REGISTRATION_DENY,
          Permission.PARTICIPANT_ADD_DIRECTLY,
          Permission.SKIP_OWNERSHIP_CHECK,
          Permission.REGISTRATION_DELETE,
          Permission.USER_VIEW_ALL,
          Permission.REGISTRATION_CHANGE_STATUS,
          Permission.SYSTEM_OVERVIEW));

  private final Set<Permission> permissions;

  Role(Set<Permission> permissions) {
    this.permissions = permissions;
  }

  public boolean hasPermission(Permission permission) {
    return this.permissions.contains(permission);
  }

  public static Role fromString(String input) {
    if (input == null) return null;
    try {
      return Role.valueOf(input.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
