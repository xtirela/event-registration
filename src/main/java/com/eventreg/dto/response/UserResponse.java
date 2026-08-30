package com.eventreg.dto.response;

import com.eventreg.model.enums.RBAC.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Schema(description = "Response with user data")
public class UserResponse {
  @Schema(description = "Unique user id", example = "1")
  private Long id;

  @Schema(description = "Unique username", example = "ivanov")
  private String username;

  @Schema(description = "Email address of the user", example = "ivan@example.com")
  private String email;

  @Schema(description = "Role of the user", example = "PARTICIPANT")
  private Role role;

  @Schema(description = "ID of the participant profile attached to the user", example = "2")
  private Long participantId;
}
