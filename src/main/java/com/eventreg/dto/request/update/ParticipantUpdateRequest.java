package com.eventreg.dto.request.update;

import com.eventreg.model.enums.ParticipantGender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(
    description = "Request to update a participant profile, only the provided fields are changed")
public class ParticipantUpdateRequest {
  @Schema(description = "First name of the participant", example = "Ivan")
  private String firstName;

  @Schema(description = "Last name of the participant", example = "Ivanov")
  private String lastName;

  @Schema(description = "Age of the participant", example = "25")
  private Integer age;

  @Schema(description = "Gender of the participant", example = "MALE")
  private ParticipantGender participantGender;
}
