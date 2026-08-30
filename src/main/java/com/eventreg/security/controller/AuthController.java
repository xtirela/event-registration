package com.eventreg.security.controller;

import com.eventreg.annotation.swagger.JwtResponseDto;
import com.eventreg.annotation.swagger.LoginErrors;
import com.eventreg.annotation.swagger.RegisterErrors;
import com.eventreg.security.dto.request.LoginRequest;
import com.eventreg.security.dto.request.RegisterRequest;
import com.eventreg.security.dto.response.JwtResponse;
import com.eventreg.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Operations for logging in and registering users")
@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;

  // TODO: разобраться с logout логикой, после создания одного user нельзя создать нового
  @Operation(description = "Log in with username and password, returns a JWT token")
  @JwtResponseDto
  @LoginErrors
  @PostMapping("/login")
  public ResponseEntity<JwtResponse> login(
      @Parameter(
              description = "Login credentials request",
              required = true,
              in = ParameterIn.DEFAULT)
          @Valid
          @RequestBody
          LoginRequest loginRequest,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(new JwtResponse(authService.login(loginRequest)));
  }

  @Operation(description = "Register a new user, returns a JWT token")
  @JwtResponseDto
  @RegisterErrors
  @PostMapping("/register")
  public ResponseEntity<JwtResponse> registerParticipant(
      @Parameter(description = "Register user request", required = true, in = ParameterIn.DEFAULT)
          @Valid
          @RequestBody
          RegisterRequest registerRequest,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(new JwtResponse(authService.register(registerRequest)));
  }
}
