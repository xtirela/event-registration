package com.eventreg.dto.response;

import com.eventreg.model.enums.ActionType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UndoResponse {
  ActionType type;
  String description;
}
