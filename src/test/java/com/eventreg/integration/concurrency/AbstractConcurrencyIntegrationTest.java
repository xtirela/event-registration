package com.eventreg.integration.concurrency;

import com.eventreg.model.Event;
import com.eventreg.model.Participant;
import com.eventreg.model.User;
import com.eventreg.model.enums.EventGenderRequirement;
import com.eventreg.model.enums.EventReservationStatus;
import com.eventreg.model.enums.EventStatus;
import com.eventreg.model.enums.ParticipantGender;
import com.eventreg.model.enums.RBAC.Role;
import com.eventreg.security.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Base class for concurrency integration tests. Boots the full application context ({@code
 * PartyApplication}) against a real PostgreSQL Testcontainers database. The real {@link
 * com.eventreg.service.implementation.EmailService} runs against a stubbed low-level {@link
 * JavaMailSender}, so no SMTP server or external network is touched. Each concrete test class
 * declares its own {@code @Container} and {@code @DynamicPropertySource}, mirroring the project's
 * integration test convention.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractConcurrencyIntegrationTest {

  @Autowired protected ObjectMapper objectMapper;
  @Autowired protected JwtService jwtService;
  @Autowired protected PasswordEncoder passwordEncoder;
  @Autowired protected JdbcTemplate jdbcTemplate;

  @MockitoBean protected JavaMailSender javaMailSender;

  protected User organizer() {
    return User.builder()
        .username("organizer_" + System.nanoTime())
        .email("organizer_" + System.nanoTime() + "@example.com")
        .password("password123")
        .role(Role.ORGANISER)
        .build();
  }

  protected User participantUser() {
    return User.builder()
        .username("participant_" + System.nanoTime())
        .email("participant_" + System.nanoTime() + "@example.com")
        .password("password123")
        .role(Role.PARTICIPANT)
        .build();
  }

  protected Participant participant(User user) {
    return Participant.builder()
        .firstName("John")
        .lastName("Doe")
        .age(25)
        .participantGender(ParticipantGender.MALE)
        .user(user)
        .build();
  }

  protected Event event(User organizer, int maxParticipants, boolean confirmationRequired) {
    return Event.builder()
        .eventName("Concurrency event " + System.nanoTime())
        .eventDescription("concurrency")
        .eventDate(OffsetDateTime.now().plusDays(30))
        .eventDuration(Duration.ofHours(2))
        .location("Test Hall")
        .ageRequired(18)
        .eventGenderRequirement(EventGenderRequirement.NONE)
        .currentParticipantAmount(0)
        .currentWaitingQueueParticipantAmount(0)
        .maxParticipantAmount(maxParticipants)
        .eventStatus(EventStatus.PLANNED)
        .eventReservationStatus(EventReservationStatus.RESERVATIONS_OPEN)
        .confirmationRequired(confirmationRequired)
        .waitlistWhenAllReserved(false)
        .organizer(organizer)
        .build();
  }

  protected void cleanDatabase() {
    jdbcTemplate.update("DELETE FROM event_registrations");
    jdbcTemplate.update("DELETE FROM events");
    jdbcTemplate.update("DELETE FROM participants");
    jdbcTemplate.update("DELETE FROM users WHERE username <> 'admin'");
    jdbcTemplate.update("DELETE FROM idempotency_keys");
  }
}
