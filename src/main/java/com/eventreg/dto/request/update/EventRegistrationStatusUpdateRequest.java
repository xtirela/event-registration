package com.eventreg.dto.request.update;

import com.eventreg.model.enums.EventRegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Request to change an event registration status")
public class EventRegistrationStatusUpdateRequest {
  @Schema(
      description = "New registration status",
      example = "ACCEPTED",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "event registration status required")
  EventRegistrationStatus eventRegistrationStatus;

  @Schema(description = "Description of the status change", example = "Approved by organizer")
  String description;
}
