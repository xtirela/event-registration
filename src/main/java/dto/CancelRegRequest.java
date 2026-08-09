package dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CancelRegRequest {
  private int eventRegistrationId;
}
