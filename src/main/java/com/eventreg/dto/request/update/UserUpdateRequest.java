package com.eventreg.dto.request.update;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Schema(description = "Request to update a user account, only the provided fields are changed")
public class UserUpdateRequest {
  @Schema(description = "Unique username", example = "ivanov")
  private String username;

  @Schema(description = "Email address of the user", example = "ivan@example.com")
  private String email;

  @Schema(description = "New password, at least 8 characters", example = "password123")
  private String password;
}
