package com.eventreg.participantservice.dto.response;

import com.eventreg.participantservice.model.enums.ParticipantGender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/** DTO returned when reading participant profile data. */
@Data
@Builder
@Schema(description = "Response with participant data")
public class ParticipantResponse {
  @Schema(description = "Unique participant id", example = "1")
  private Long id;

  @Schema(description = "Keycloak user ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
  private String keycloakId;

  @Schema(description = "Participant first name", example = "Ivan")
  private String firstName;

  @Schema(description = "Participant last name", example = "Ivanov")
  private String lastName;

  @Schema(description = "Participant age", example = "25")
  private Integer age;

  @Schema(description = "Participant gender", example = "MALE")
  private ParticipantGender participantGender;
}
