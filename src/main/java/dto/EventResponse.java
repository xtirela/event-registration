package dto;

import java.time.Duration;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;
import model.enums.EventGenderRequirement;
import model.enums.EventRegistrationStatus;
import model.enums.EventStatus;

@Data
@Builder
public class EventResponse {
  private int eventId;

  private String eventName;

  private OffsetDateTime eventDate;

  private String location;

  private Duration eventDuration;

  private int ageRequired;

  private int currentParticipantAmount;

  private int maxParticipantAmount;
  private EventGenderRequirement eventGenderRequirement;

  private EventStatus eventStatus;
  private EventRegistrationStatus eventRegistrationStatus;
}
