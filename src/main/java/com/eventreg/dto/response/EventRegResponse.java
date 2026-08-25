package com.eventreg.dto.response;

import lombok.Builder;
import lombok.Data;
import com.eventreg.model.enums.EventRegRequestStatus;

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
