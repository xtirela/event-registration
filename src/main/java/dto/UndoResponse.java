package dto;

import lombok.Builder;
import lombok.Data;
import model.enums.ActionType;

@Data
@Builder
public class UndoResponse {
  ActionType type;
  String description;
}
