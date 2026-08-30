package com.eventreg.model.enums.RBAC;

public enum Permission {
  EVENT_VIEW, // Смотреть список событий
  PARTICIPANT_CREATE,
  PARTICIPANT_VIEW,
  PARTICIPANT_DELETE,
  PARTICIPANT_UPDATE,
  USER_DELETE,
  USER_UPDATE,
  REGISTRATION_CREATE,
  REGISTRATION_VIEW,
  REGISTRATION_CANCEL,
  USER_VIEW,

  // Организатор
  EVENT_CREATE, // Создавать свои события
  EVENT_DELETE,
  EVENT_UPDATE, // Редактировать свои события
  REGISTRATION_ACCEPT, // Подтверждать заявки
  REGISTRATION_DENY, // Отклонять заявки
  PARTICIPANT_ADD_DIRECTLY, // Записывать участника напрямую

  // Админ
  SKIP_OWNERSHIP_CHECK,
  USER_VIEW_ALL,
  REGISTRATION_DELETE,
  REGISTRATION_CHANGE_STATUS,
  SYSTEM_OVERVIEW // Смотреть статистику/логи
}
