package dto;

import lombok.Builder;
import lombok.Data;
import model.enums.EventRegRequestStatus;

@Data
@Builder
public class EventRegResponse {
  private int registrationId;

  private int participantId;

  private int eventId;

  ParticipantResponse participantResponse;
  EventResponse eventResponse;

  private EventRegRequestStatus eventRegRequestStatus;
  @Builder.Default private String description = "none";
}
