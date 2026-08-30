package com.eventreg.dto.response;

import com.eventreg.model.enums.EventGenderRequirement;
import com.eventreg.model.enums.EventReservationStatus;
import com.eventreg.model.enums.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response with event data")
public class EventResponse {
  @Schema(description = "Unique event id", example = "1")
  private Long id;

  @Schema(description = "Name of the event", example = "Tech Conference")
  private String eventName;

  @Schema(description = "Description of the event", example = "Annual tech meetup")
  private String eventDescription;

  @Schema(description = "Date and time the event starts", example = "2026-10-01T18:00:00+03:00")
  private OffsetDateTime eventDate;

  @Schema(description = "Location where the event takes place", example = "Moscow, Tverskaya 1")
  private String location;

  @Schema(description = "Duration of the event", example = "PT2H")
  private Duration eventDuration;

  @Schema(description = "Minimum age required to participate", example = "18")
  private int ageRequired;

  @Schema(description = "Gender requirement for the event", example = "NONE")
  private EventGenderRequirement eventGenderRequirement;

  @Schema(description = "Maximum number of participants", example = "100")
  private int maxParticipantAmount;

  @Schema(description = "Current number of accepted participants", example = "42")
  private int currentParticipantAmount;

  @Schema(description = "Current number of participants in the waiting queue", example = "7")
  private int currentWaitingQueueParticipantAmount;

  @Schema(description = "Lifecycle status of the event", example = "PLANNED")
  private EventStatus eventStatus;

  @Schema(description = "Reservation status of the event", example = "RESERVATIONS_OPEN")
  private EventReservationStatus eventReservationStatus;

  @Schema(description = "Whether registration requires manual confirmation", example = "false")
  private boolean confirmationRequired;

  @Schema(description = "ID of the user organizing the event", example = "5")
  private Long organizerId;
}
