package model;

import lombok.Builder;
import lombok.Data;
import model.enums.ActionType;
import model.enums.EventRegRequestStatus;

@Data
@Builder
public class ActionData {
  Integer participantId;
  Integer eventId;
  Integer registeredRegistrationId;
  EventRegRequestStatus registeredEventRegRequestStatus;
  Integer cancelledRegistrationId;
  EventRegRequestStatus cancelledEventRegRequestStatus;
  ActionType actionType;
}
