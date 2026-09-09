package com.eventreg.eventregistrationservice.dto.response;

import com.eventreg.eventregistrationservice.model.enums.EventRegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** DTO carrying event registration data in API responses. */
@Data
@Builder
@Schema(description = "Response with event registration data")
public class EventRegistrationResponse {
  @Schema(description = "Unique registration id", example = "1")
  private Long id;

  @Schema(description = "ID of the event the registration belongs to", example = "3")
  private Long eventId;

  @Schema(description = "ID of the participant who registered", example = "2")
  private Long participantId;

  @Schema(description = "Registration status", example = "ACCEPTED")
  private EventRegistrationStatus eventRegistrationStatus;

  @Builder.Default
  @Schema(
      description = "Description of the registration status",
      example = "event registration accepted")
  private String description = "none";
}
