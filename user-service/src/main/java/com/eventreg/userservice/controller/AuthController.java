package com.eventreg.userservice.controller;

import com.eventreg.userservice.annotation.swagger.LoginResponseDto;
import com.eventreg.userservice.annotation.swagger.RegisterCreatedDto;
import com.eventreg.userservice.dto.request.LoginRequest;
import com.eventreg.userservice.dto.request.RegisterRequest;
import com.eventreg.userservice.dto.response.LoginResponse;
import com.eventreg.userservice.dto.response.RegisterResponse;
import com.eventreg.userservice.service.KeycloakClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "Authentication",
    description = "Registration and login. Both endpoints are public (whitelisted in the gateway)")
@RestController
@RequiredArgsConstructor
public class AuthController {

  private final KeycloakClient keycloakClient;

  @Operation(
      description =
          "Create a user in Keycloak and assign ROLE_PARTICIPANT or ROLE_ORGANISER (isOrganizer)")
  @RegisterCreatedDto
  @PostMapping("/register")
  public ResponseEntity<RegisterResponse> register(
      @Parameter(description = "Register request", required = true, in = ParameterIn.DEFAULT)
          @Valid
          @RequestBody
          RegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(keycloakClient.register(request));
  }

  @Operation(description = "Exchange username/password for a Keycloak JWT access token")
  @LoginResponseDto
  @PostMapping("/login")
  public LoginResponse login(
      @Parameter(description = "Login credentials", required = true, in = ParameterIn.DEFAULT)
          @Valid
          @RequestBody
          LoginRequest request) {
    return keycloakClient.login(request);
  }
}
