package com.eventreg.model.enums.RBAC;

import java.util.Set;

public enum Role
{
    PARTICIPANT(
            Set.of(
                    Permission.EVENT_VIEW,
                    Permission.EVENT_REGISTER
                 )
            ),
    ORGANISER(
            Set.of(
                    // Организатор тоже может быть участником (видеть и записываться)
                    Permission.EVENT_VIEW,
                    Permission.EVENT_REGISTER,

                    // Плюс права организатора
                    Permission.EVENT_CREATE,
                    Permission.EVENT_UPDATE,
                    Permission.REGISTRATION_VIEW,
                    Permission.REGISTRATION_APPROVE,
                    Permission.REGISTRATION_REJECT,
                    Permission.PARTICIPANT_ADD_DIRECTLY
            )
    ),
    ADMIN(
            Set.of(
                    // Админ может всё (или можно перечислить для наглядности)
                    Permission.EVENT_VIEW,
                    Permission.EVENT_REGISTER,
                    Permission.EVENT_CREATE,
                    Permission.EVENT_UPDATE,
                    Permission.REGISTRATION_VIEW,
                    Permission.REGISTRATION_APPROVE,
                    Permission.REGISTRATION_REJECT,
                    Permission.PARTICIPANT_ADD_DIRECTLY,
                    Permission.USER_MANAGE,
                    Permission.EVENT_DELETE_ANY,
                    Permission.SYSTEM_OVERVIEW
            )
    );

    private final Set<Permission> permissions;

    // Вот ОБЯЗАТЕЛЬНЫЙ конструктор
    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return permissions;
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
