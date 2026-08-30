package com.eventreg.dto.response;

import com.eventreg.model.enums.ParticipantGender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Response with participant data")
public class ParticipantResponse {
  @Schema(description = "Unique participant id", example = "1")
  private Long id;

  @Schema(description = "Participant first name", example = "Ivan")
  private String firstName;

  @Schema(description = "Participant last name", example = "Ivanov")
  private String lastName;

  @Schema(description = "Participant age", example = "25")
  private int age;

  @Schema(description = "Participant gender", example = "MALE")
  private ParticipantGender participantGender;

  @Schema(description = "ID of the user the participant profile is attached to", example = "42")
  private Long userId;
}
