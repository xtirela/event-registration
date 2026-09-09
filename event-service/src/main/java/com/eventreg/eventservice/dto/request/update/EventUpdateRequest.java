package com.eventreg.eventservice.dto.request.update;

import com.eventreg.eventservice.model.enums.EventGenderRequirement;
import com.eventreg.eventservice.model.enums.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update an event, only the provided fields are changed")
public class EventUpdateRequest {
  @Schema(description = "Name of the event", example = "Tech Conference")
  private String eventName;

  @Schema(description = "Date and time the event starts", example = "2026-10-01T18:00:00+03:00")
  private OffsetDateTime eventDate;

  @Schema(description = "Location where the event takes place", example = "Moscow, Tverskaya 1")
  private String location;

  @Schema(description = "Duration of the event in minutes", example = "120")
  private Long eventDurationMinutes;

  @Schema(description = "Minimum age required to participate", example = "18")
  private Integer ageRequired;

  @Schema(description = "Gender requirement for the event", example = "NONE")
  private EventGenderRequirement eventGenderRequirement;

  @Schema(description = "Maximum number of participants", example = "100")
  private Integer maxParticipantAmount;

  @Schema(description = "Lifecycle status of the event", example = "PLANNED")
  private EventStatus eventStatus;

  @Schema(description = "Whether registration requires manual confirmation", example = "false")
  private Boolean confirmationRequired;
}
