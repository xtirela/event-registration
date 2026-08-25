package com.eventreg.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventSummaryResponse {
  private String eventName;
  private int currentParticipantAmount;
  private int freeSpotAmount;
  private int waitlistSize;
  private double fillPercent;
}
