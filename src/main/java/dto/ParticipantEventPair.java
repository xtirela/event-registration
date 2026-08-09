package dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParticipantEventPair {
  EventRegResponse eventRegResponse;
  EventResponse eventResponse;
}
