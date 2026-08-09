package model;

import lombok.Builder;
import lombok.Data;
import model.enums.EventGenderRequirement;
import model.enums.EventRegistrationStatus;
import model.enums.EventStatus;

import java.time.Duration;
import java.time.OffsetDateTime;

@Data
@Builder
public class Event
{
    private int id;
    private String eventName;

    private OffsetDateTime eventDate;

    private Duration eventDuration;

    private int ageRequired;

    private EventGenderRequirement eventGenderRequirement;

    private int currentParticipantAmount;
    private int maxParticipantAmount;

    private EventStatus eventStatus;
    private EventRegistrationStatus eventRegistrationStatus;

    private OffsetDateTime createdAt;
}
//TODO: Добавить логику взаимодействия с временем, отдельная логика времени куда вводятся данные и проверяются какие события происходят и какие будут происходить
//TODO: окно регистрации на событие
//TODO: автоматизировання логики EventRegistrationStatus связанная с текущим временем, то же самое с EventStatus
//TODO: EventService и polling его на обновление статуса События