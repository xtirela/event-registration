package com.eventreg.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to register a new user")
public class RegisterRequest {
  @Schema(
      description = "Unique username, between 3 and 50 characters",
      example = "ivanov",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  private String username;

  @Schema(
      description = "Email address of the user",
      example = "ivan@example.com",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  private String email;

  @Schema(
      description = "Password, at least 8 characters",
      example = "password123",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Password is required")
  @Size(min = 8, message = "Password must be at least 8 characters")
  private String password;

  @Schema(description = "Whether the user should get the ORGANISER role", example = "false")
  private boolean isOrganizer;
}
