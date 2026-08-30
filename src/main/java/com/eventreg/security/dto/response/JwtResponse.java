package com.eventreg.security.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(
    description = "Response containing a JWT token after successful authentication or registration")
public class JwtResponse {
  @Schema(
      description = "JWT access token",
      example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJpdmFub3YifQ.example")
  private String token;
}
