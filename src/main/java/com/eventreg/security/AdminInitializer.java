package com.eventreg.security;

import com.eventreg.model.User;
import com.eventreg.model.enums.RBAC.Role;
import com.eventreg.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    // Проверяем, есть ли админ
    if (userRepository.findByUsername("admin").isEmpty()) {

      // Создаем админа
      User admin =
          User.builder()
              .username("admin")
              .email("admin@example.com")
              .password(passwordEncoder.encode("admin123"))
              .role(Role.ADMIN)
              .build();

      userRepository.save(admin);
    }
  }
}
