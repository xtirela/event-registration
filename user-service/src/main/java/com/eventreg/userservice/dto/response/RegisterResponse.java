package com.eventreg.userservice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response confirming a new user account was created")
public class RegisterResponse {

  @Schema(
      description = "Human-readable account created message",
      example = "User registered successfully")
  private String message;
}
