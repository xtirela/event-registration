package com.eventreg.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.eventreg.model.Event;
import com.eventreg.model.EventRegistration;
import com.eventreg.model.Participant;
import com.eventreg.model.User;
import com.eventreg.model.enums.EventGenderRequirement;
import com.eventreg.model.enums.EventRegistrationStatus;
import com.eventreg.model.enums.EventReservationStatus;
import com.eventreg.model.enums.EventStatus;
import com.eventreg.model.enums.ParticipantGender;
import com.eventreg.model.enums.RBAC.Role;
import com.eventreg.repository.EventRegistrationRepository;
import com.eventreg.repository.EventRepository;
import com.eventreg.repository.ParticipantRepository;
import com.eventreg.repository.UserRepository;
import com.eventreg.security.service.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base class for the Spring Boot controller integration tests.
 *
 * <p>The full application context ({@link TestApplication}) is started with a real PostgreSQL
 * Testcontainers database. Authentication is performed with real JWT tokens issued by the {@link
 * JwtService}; the only stubbed dependency is the low level {@link JavaMailSender} used by the real
 * {@link com.eventreg.service.implementation.EmailService}, so no SMTP server is touched.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseControllerIntegrationTest {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected ObjectMapper objectMapper;
  @Autowired protected JwtService jwtService;
  @Autowired protected PasswordEncoder passwordEncoder;
  @Autowired protected UserRepository userRepository;
  @Autowired protected ParticipantRepository participantRepository;
  @Autowired protected EventRepository eventRepository;
  @Autowired protected EventRegistrationRepository eventRegistrationRepository;
  @Autowired protected JdbcTemplate jdbcTemplate;

  @MockitoBean protected JavaMailSender javaMailSender;

  @BeforeEach
  protected void cleanDatabase() {
    jdbcTemplate.update("DELETE FROM event_registrations");
    jdbcTemplate.update("DELETE FROM events");
    jdbcTemplate.update("DELETE FROM participants");
    jdbcTemplate.update("DELETE FROM users WHERE username <> 'admin'");
    jdbcTemplate.update("DELETE FROM idempotency_keys");
  }

  // ---------- fixture helpers ----------

  protected User createUser(String username, Role role) {
    return userRepository.save(
        User.builder()
            .username(username)
            .email(username + "@example.com")
            .password(passwordEncoder.encode("password123"))
            .role(role)
            .build());
  }

  protected User createUserWithEmail(String username, String email, Role role) {
    return userRepository.save(
        User.builder()
            .username(username)
            .email(email)
            .password(passwordEncoder.encode("password123"))
            .role(role)
            .build());
  }

  protected Participant createParticipant(User user) {
    return participantRepository.save(
        Participant.builder()
            .firstName("FirstName")
            .lastName("LastName")
            .age(25)
            .participantGender(ParticipantGender.MALE)
            .user(user)
            .build());
  }

  protected Event createEvent(
      String eventName,
      User organizer,
      int maxParticipantAmount,
      boolean waitlistWhenAllReserved,
      boolean confirmationRequired) {
    return eventRepository.save(
        Event.builder()
            .eventName(eventName)
            .eventDescription("Event description")
            .location("Moscow, Tverskaya 1")
            .eventDate(OffsetDateTime.now().plusDays(10))
            .eventDuration(Duration.ofHours(2))
            .ageRequired(18)
            .eventGenderRequirement(EventGenderRequirement.NONE)
            .maxParticipantAmount(maxParticipantAmount)
            .eventStatus(EventStatus.PLANNED)
            .eventReservationStatus(EventReservationStatus.RESERVATIONS_OPEN)
            .confirmationRequired(confirmationRequired)
            .waitlistWhenAllReserved(waitlistWhenAllReserved)
            .organizer(organizer)
            .build());
  }

  protected EventRegistration createRegistration(
      Event event, Participant participant, EventRegistrationStatus status) {
    return eventRegistrationRepository.save(
        EventRegistration.builder()
            .event(event)
            .participant(participant)
            .eventRegistrationStatus(status)
            .description("none")
            .build());
  }

  // ---------- request helpers ----------

  protected String bearerToken(User user) {
    return "Bearer " + jwtService.generateToken(user);
  }

  protected MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder, User user) {
    return builder.header(HttpHeaders.AUTHORIZATION, bearerToken(user));
  }

  protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, Object body)
      throws JsonProcessingException {
    return builder
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body));
  }

  protected MockHttpServletRequestBuilder postJson(String url, Object body)
      throws JsonProcessingException {
    return json(post(url), body);
  }

  protected MockHttpServletRequestBuilder patchJson(String url, Object body)
      throws JsonProcessingException {
    return json(patch(url), body);
  }

  protected MockHttpServletRequestBuilder patchJson(String url, long id, Object body)
      throws JsonProcessingException {
    return json(patch(url, id), body);
  }
}
