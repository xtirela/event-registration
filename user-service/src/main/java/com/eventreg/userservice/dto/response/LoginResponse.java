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
@Schema(description = "Response with a Keycloak JWT token pair")
public class LoginResponse {

  @Schema(
      description = "JWT access token, valid against the oauth realm",
      example = "eyJhbGciOi...")
  private String accessToken;

  @Schema(description = "Refresh token", example = "eyJhbGciOi...")
  private String refreshToken;

  @Schema(description = "Access token lifetime in seconds", example = "300")
  private long expiresIn;

  @Schema(description = "Token type", example = "Bearer")
  private String tokenType;
}
