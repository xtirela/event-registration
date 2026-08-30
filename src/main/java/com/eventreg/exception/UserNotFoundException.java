package com.eventreg.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends EventRegException {
  public UserNotFoundException(Long userId, String operation) {
    super("user with id " + userId + " not found", operation, HttpStatus.NOT_FOUND);
  }

  public UserNotFoundException(String username, String operation) {
    super("user with id " + username + " not found", operation, HttpStatus.NOT_FOUND);
  }
}
