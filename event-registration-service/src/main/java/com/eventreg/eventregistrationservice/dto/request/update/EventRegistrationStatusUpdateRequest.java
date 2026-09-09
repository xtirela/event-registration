package com.eventreg.eventregistrationservice.dto.request.update;

import com.eventreg.eventregistrationservice.model.enums.EventRegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for changing an event registration status. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
