package com.eventreg.participantservice.dto.request.create;

import com.eventreg.participantservice.model.enums.ParticipantGender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for creating a new participant profile. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema
public class ParticipantCreateRequest {
  @Schema(
      description = "first name of the participant",
      example = "Ivan",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "First name is required")
  private String firstName;

  @Schema(
      description = "last name of the participant",
      example = "Ivanov",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @Size(max = 100, message = "Last name too long")
  private String lastName;

  @Schema(
      description = "age of the participant",
      example = "18",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @Min(value = 0, message = "Age cannot be negative")
  @Max(value = 150, message = "Age too high")
  private Integer age;

  @Schema(
      description = "gender of the participant",
      example = "MALE",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Gender is required")
  private ParticipantGender participantGender;
}
