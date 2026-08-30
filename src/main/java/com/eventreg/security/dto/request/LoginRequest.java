package com.eventreg.security.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login credentials request")
public class LoginRequest {
  @NotBlank
  @Schema(description = "Username of the account", example = "ivanov")
  String username;

  @NotBlank
  @Schema(description = "Password of the account", example = "password123")
  String password;
}
