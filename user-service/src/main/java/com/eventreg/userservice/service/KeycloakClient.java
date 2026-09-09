package com.eventreg.userservice.service;

import com.eventreg.userservice.dto.request.LoginRequest;
import com.eventreg.userservice.dto.request.RegisterRequest;
import com.eventreg.userservice.dto.response.LoginResponse;
import com.eventreg.userservice.dto.response.RegisterResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class KeycloakClient {

  private static final String ROLE_PARTICIPANT = "ROLE_PARTICIPANT";
  private static final String ROLE_ORGANISER = "ROLE_ORGANISER";

  private final RestClient restClient;
  private final String realm;
  private final String clientId;
  private final String clientSecret;

  public KeycloakClient(
      @Value("${keycloak.base-url}") String baseUrl,
      @Value("${keycloak.realm}") String realm,
      @Value("${keycloak.client-id}") String clientId,
      @Value("${keycloak.client-secret}") String clientSecret) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    this.realm = realm;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  @CircuitBreaker(name = "keycloak")
  public LoginResponse login(LoginRequest request) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "password");
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    form.add("username", request.getUsername());
    form.add("password", request.getPassword());

    Map<String, Object> token;
    try {
      token = callTokenEndpoint(form);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
      }
      throw e;
    }
    return LoginResponse.builder()
        .accessToken((String) token.get("access_token"))
        .refreshToken((String) token.get("refresh_token"))
        .expiresIn(((Number) token.get("expires_in")).longValue())
        .tokenType((String) token.get("token_type"))
        .build();
  }

  @CircuitBreaker(name = "keycloak")
  public RegisterResponse register(RegisterRequest request) {
    String userId;
    try {
      URI location =
          restClient
              .post()
              .uri("/admin/realms/{realm}/users", realm)
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + adminToken())
              .body(newUser(request))
              .retrieve()
              .toBodilessEntity()
              .getHeaders()
              .getLocation();
      userId = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.CONFLICT) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
      }
      throw e;
    }
    // ponytail: self-declared ORGANISER (from request) is an accepted privilege-escalation
    // shortcut for the course; upgrade: assign the role from Keycloak admin only.
    assignRole(userId, request.isOrganizer() ? ROLE_ORGANISER : ROLE_PARTICIPANT);
    return RegisterResponse.builder().message("User registered successfully").build();
  }

  private LoginResponse loginFallback(LoginRequest request, Throwable t) {
    throw new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE, "Auth provider unavailable, try again later");
  }

  private RegisterResponse registerFallback(RegisterRequest request, Throwable t) {
    throw new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE, "Auth provider unavailable, try again later");
  }

  private Map<String, Object> newUser(RegisterRequest request) {
    Map<String, Object> user = new HashMap<>();
    user.put("username", request.getUsername());
    user.put("email", request.getEmail());
    user.put("enabled", true);
    user.put("emailVerified", true);
    Optional.ofNullable(request.getFirstName()).ifPresent(v -> user.put("firstName", v));
    Optional.ofNullable(request.getLastName()).ifPresent(v -> user.put("lastName", v));
    user.put(
        "credentials",
        List.of(Map.of("type", "password", "value", request.getPassword(), "temporary", false)));
    return user;
  }

  private void assignRole(String userId, String roleName) {
    Map<String, Object> role =
        restClient
            .get()
            .uri("/admin/realms/{realm}/roles/{role}", realm, roleName)
            .header("Authorization", "Bearer " + adminToken())
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    restClient
        .post()
        .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + adminToken())
        .body(List.of(role))
        .retrieve()
        .toBodilessEntity();
  }

  private String adminToken() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    return (String) callTokenEndpoint(form).get("access_token");
  }

  private Map<String, Object> callTokenEndpoint(MultiValueMap<String, String> form) {
    return restClient
        .post()
        .uri("/realms/{realm}/protocol/openid-connect/token", realm)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
  }
}
