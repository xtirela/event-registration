package com.eventreg.apigateway.config;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/** Security configuration for the gateway. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private static final String[] AUTH_WHITELIST = {
    "/swagger-resources/**",
    "/v3/api-docs/**",
    "/webjars/**",
    "/swagger-ui.html",
    "/actuator/**",
    "/health/**",
    "/fallback/**",
    "/register",
    "/login"
  };

  /**
   * Builds the gateway security filter chain.
   *
   * @param http the http security builder
   * @return the configured filter chain
   * @throws Exception if the chain cannot be built
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(AUTH_WHITELIST).permitAll().anyRequest().authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

    return http.build();
  }

  /**
   * Extracts roles from the Keycloak realm_access claim into granted authorities.
   *
   * @return the jwt authentication converter
   */
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    converter.setPrincipalClaimName("preferred_username");
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

          if (realmAccess == null) {
            return List.of();
          }

          Object rolesObj = realmAccess.get("roles");
          if (!(rolesObj instanceof List<?> rawList)) {
            return List.of();
          }

          List<String> roles =
              rawList.stream().filter(String.class::isInstance).map(String.class::cast).toList();

          return roles.stream()
              .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
              .collect(Collectors.toList());
        });

    return converter;
  }
}
