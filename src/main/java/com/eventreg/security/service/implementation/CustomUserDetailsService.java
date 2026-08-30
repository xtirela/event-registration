package com.eventreg.security.service.implementation;

import com.eventreg.exception.UserNotFoundException;
import com.eventreg.model.User;
import com.eventreg.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {
  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UserNotFoundException(username, "loadUserByUsername"));

    return new org.springframework.security.core.userdetails.User(
        user.getUsername(),
        user.getPassword(),
        user.getRole().getPermissions().stream()
            .map(permission -> new SimpleGrantedAuthority(permission.name()))
            .toList());
  }
}
