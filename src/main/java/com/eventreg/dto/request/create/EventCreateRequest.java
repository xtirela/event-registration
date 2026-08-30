package com.eventreg.dto.request.create;

import com.eventreg.model.enums.EventGenderRequirement;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Request to create an event")
public class EventCreateRequest {

  @Schema(
      description = "Name of the event",
      example = "Tech Conference",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Event name is required")
  private String eventName;

  @Schema(description = "Description of the event", example = "Annual tech meetup")
  @Size(max = 1000, message = "Description too long")
  private String eventDescription;

  @Schema(
      description = "Date and time the event starts",
      example = "2026-10-01T18:00:00+03:00",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Event date is required")
  @FutureOrPresent(message = "Event date must be in future or present")
  private OffsetDateTime eventDate;

  @Schema(
      description = "Duration of the event",
      example = "PT2H",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Event duration is required")
  private Duration eventDuration;

  @Schema(
      description = "Location where the event takes place",
      example = "Moscow, Tverskaya 1",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Location is required")
  private String location;

  @Schema(description = "Minimum age required to participate", example = "18")
  @Min(value = 0, message = "Age cannot be negative")
  private int ageRequired;

  @Schema(
      description = "Gender requirement for the event",
      example = "NONE",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Gender requirement is required")
  private EventGenderRequirement eventGenderRequirement;

  @Schema(
      description = "Maximum number of participants",
      example = "100",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @Min(value = 1, message = "Max participants must be at least 1")
  private int maxParticipantAmount;

  @Schema(description = "Whether registration requires manual confirmation", example = "false")
  private boolean confirmationRequired;

  @Schema(
      description = "Whether a waiting queue is enabled once all places are reserved",
      example = "true")
  private boolean waitlistWhenAllReserved;

  @Schema(
      description = "ID of the user organizing the event",
      example = "5",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Organizer ID is required")
  @Positive(message = "Organizer ID must be positive")
  private Long organizerId;
}
