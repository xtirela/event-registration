package dto;

import lombok.Builder;
import lombok.Data;
import model.enums.EventGenderRequirement;
import model.enums.EventRegistrationStatus;
import model.enums.EventStatus;

import java.time.Duration;
import java.time.OffsetDateTime;

@Data
@Builder
public class EventResponse
{
    private String eventName;

    private OffsetDateTime eventDate;

    private Duration eventDuration;

    private int ageRequired;

    private int currentParticipantAmount;

    private int maxParticipantAmount;
    private EventGenderRequirement eventGenderRequirement;

    private EventStatus eventStatus;
    private EventRegistrationStatus eventRegistrationStatus;
}
