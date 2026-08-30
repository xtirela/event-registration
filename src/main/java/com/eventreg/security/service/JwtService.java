package com.eventreg.security.service;

import com.eventreg.model.User;

public interface JwtService {
  String generateToken(User user);

  String extractUserName(String token);

  boolean isTokenValid(String token);
}
