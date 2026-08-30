package com.eventreg.controller.unit;

import com.eventreg.security.SecurityGuard;
import com.eventreg.security.service.JwtService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Base class for {@code @WebMvcTest} controller unit tests. Holds the shared security mocks that
 * the web slice context requires ({@link JwtService}, {@link UserDetailsService} and the security
 * guard) so each subclass only declares the mocks for its own controller dependencies.
 */
public abstract class BaseControllerUnitTest {

  @MockitoBean protected JwtService jwtService;
  @MockitoBean protected UserDetailsService userDetailsService;
  @MockitoBean protected SecurityGuard securityGuard;
}
