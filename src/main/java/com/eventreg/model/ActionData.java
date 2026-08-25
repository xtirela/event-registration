package com.eventreg.model;

import lombok.Builder;
import lombok.Data;
import com.eventreg.model.enums.ActionType;
import com.eventreg.model.enums.EventRegRequestStatus;

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
