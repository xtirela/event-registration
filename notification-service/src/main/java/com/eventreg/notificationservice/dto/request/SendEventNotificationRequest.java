package com.eventreg.notificationservice.dto.request;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request payload for sending an event-created notification. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEventNotificationRequest {
  private String eventName;
  private String eventDescription;
  private OffsetDateTime eventDate;
  private long eventDurationMinutes;
}
