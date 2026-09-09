package com.eventreg.eventregistrationservice.dto.request.create;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for creating an event registration. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request to create an event registration")
public class EventRegistrationCreateRequest {

  @Schema(
      description = "ID of the participant registering for the event",
      example = "2",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Participant id is required")
  private Long participantId;

  @Schema(
      description = "ID of the event to register for",
      example = "3",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Event id is required")
  private Long eventId;

  @Schema(
      description = "Skip the pending state and accept the registration immediately",
      example = "false")
  @Builder.Default
  private boolean skipPending = false;
}
