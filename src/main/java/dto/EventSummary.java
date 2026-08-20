package dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventSummary {
  private String eventName;
  private int currentParticipantAmount;
  private int freeSpotAmount;
  private int waitlistSize;
  private double fillPercent;
}
