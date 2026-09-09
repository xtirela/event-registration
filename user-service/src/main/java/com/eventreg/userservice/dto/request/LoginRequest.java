package com.eventreg.userservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to log in and obtain a Keycloak JWT")
public class LoginRequest {

  @Schema(
      description = "Username of the account",
      example = "ivanov",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Username is required")
  private String username;

  @Schema(
      description = "Password of the account",
      example = "password123",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Password is required")
  private String password;
}
