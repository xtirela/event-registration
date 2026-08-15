package dto;

import java.time.Duration;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;
import model.enums.EventGenderRequirement;

@Data
@Builder
public class EventCreateRequest {
  private String eventName;

  private OffsetDateTime eventDate;

  private Duration eventDuration;

  private String location;

  private int ageRequired;
  private EventGenderRequirement genderRequirement;

  private int maxParticipantAmount;
}
