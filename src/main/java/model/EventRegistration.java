package model;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;
import model.enums.EventRegRequestStatus;

@Data
@Builder
public class EventRegistration {
  private int id;

  private int participantId;

  private int eventId;

  private EventRegRequestStatus eventRegRequestStatus;
  @Builder.Default private String description = "none";
  private OffsetDateTime createdAt;
}
