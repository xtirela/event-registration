package com.eventreg.model.enums.RBAC;

public enum Permission
{
    EVENT_VIEW,        // Смотреть список событий
    EVENT_REGISTER,    // Записываться на события
    EVENT_CANCEL,

    // Организатор
    EVENT_CREATE,      // Создавать свои события
    EVENT_UPDATE,      // Редактировать свои события
    REGISTRATION_VIEW, // Смотреть заявки на своем событии
    REGISTRATION_APPROVE, // Подтверждать заявки
    REGISTRATION_REJECT,  // Отклонять заявки
    PARTICIPANT_ADD_DIRECTLY, // Записывать участника напрямую

    // Админ
    USER_MANAGE,       // Управлять пользователями
    EVENT_DELETE_ANY,  // Удалять любые события
    SYSTEM_OVERVIEW    // Смотреть статистику/логи
}
