package com.eventreg.integration.load;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.eventreg.dto.request.create.EventCreateRequest;
import com.eventreg.model.User;
import com.eventreg.model.enums.EventGenderRequirement;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Load test for {@code POST /api/events} - creating events. */
class EventCreateLoadTest extends BaseLoadTest {

  private static final Duration MAX_ONE_REQUEST = Duration.ofMillis(500);
  private static final int REQUESTS = 50;

  private User admin;

  @BeforeEach
  void setUp() throws Exception {
    initLoadTest();
    admin = userRepository.findByUsername("admin").orElseThrow();
  }

  @Test
  void shouldCreateEachEventUnder500ms_WhenCreatingFiftyEvents() throws Exception {
    List<Long> durations = new ArrayList<>(REQUESTS);
    for (int i = 0; i < REQUESTS; i++) {
      EventCreateRequest request =
          EventCreateRequest.builder()
              .eventName("Load Event " + i)
              .eventDescription("load")
              .eventDate(OffsetDateTime.now().plusDays(60))
              .eventDuration(Duration.ofHours(2))
              .location("Load Hall")
              .ageRequired(18)
              .eventGenderRequirement(EventGenderRequirement.NONE)
              .maxParticipantAmount(100)
              .confirmationRequired(false)
              .waitlistWhenAllReserved(false)
              .organizerId(admin.getId())
              .build();

      long start = System.nanoTime();
      mockMvc
          .perform(
              authorized(post("/api/events"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andReturn();
      durations.add(System.nanoTime() - start);
    }

    for (long duration : durations) {
      assertUnder(duration, MAX_ONE_REQUEST, "create event");
    }
  }
}
