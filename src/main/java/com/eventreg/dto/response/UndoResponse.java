package com.eventreg.dto.response;

import lombok.Builder;
import lombok.Data;
import com.eventreg.model.enums.ActionType;

@Data
@Builder
public class UndoResponse {
  ActionType type;
  String description;
}
