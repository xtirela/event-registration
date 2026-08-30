package com.eventreg.security.service;

import com.eventreg.security.dto.request.LoginRequest;
import com.eventreg.security.dto.request.RegisterRequest;

public interface AuthService {
  String login(LoginRequest loginRequest);

  String register(RegisterRequest registerRequest);
}
