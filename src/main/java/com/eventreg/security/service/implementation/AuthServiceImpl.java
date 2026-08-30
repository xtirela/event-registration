package com.eventreg.security.service.implementation;

import com.eventreg.annotation.Idempotent;
import com.eventreg.exception.UserNotFoundException;
import com.eventreg.mapper.UserMapper;
import com.eventreg.model.User;
import com.eventreg.model.enums.RBAC.Role;
import com.eventreg.repository.UserRepository;
import com.eventreg.security.dto.request.LoginRequest;
import com.eventreg.security.dto.request.RegisterRequest;
import com.eventreg.security.service.AuthService;
import com.eventreg.security.service.JwtService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Override
  @Transactional
  public String login(LoginRequest loginRequest) {

    User user =
        userRepository
            .findByUsername(loginRequest.getUsername())
            .orElseThrow(() -> new UserNotFoundException(loginRequest.getUsername(), "login"));
    if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
      return jwtService.generateToken(user);
    }
    throw new BadCredentialsException("username or password is incorrect!");
  }

  @Override
  @Idempotent
  @Transactional
  public String register(RegisterRequest registerRequest) {

    registerRequest.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
    User user = userMapper.userRegisterRequestToUser(registerRequest);

    if (registerRequest.isOrganizer()) {
      user.setRole(Role.ORGANISER);
    }

    return jwtService.generateToken(userRepository.save(user));
  }
}
