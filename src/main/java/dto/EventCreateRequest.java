package dto;

import lombok.Builder;
import lombok.Data;
import model.enums.EventGenderRequirement;

import java.time.Duration;
import java.time.OffsetDateTime;

@Data
@Builder
public class EventCreateRequest
{
    private String eventName;

    private OffsetDateTime eventDate;

    private Duration eventDuration;

    private int ageRequired;
    private EventGenderRequirement genderRequirement;

    private int maxParticipantAmount;
}
