package com.eventreg.integration.load;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.eventreg.dto.request.create.EventRegistrationCreateRequest;
import com.eventreg.model.Event;
import com.eventreg.model.Participant;
import com.eventreg.model.User;
import com.eventreg.model.enums.RBAC.Role;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Load test for concurrent {@code POST /api/registrations} - registering for an event. */
class ConcurrentRegistrationLoadTest extends BaseLoadTest {

  private static final Duration MAX_TOTAL = Duration.ofSeconds(2);
  private static final int CONCURRENT_REQUESTS = 50;

  private Event event;
  private final List<Participant> participants = new ArrayList<>(CONCURRENT_REQUESTS);

  @BeforeEach
  void setUp() throws Exception {
    initLoadTest();
    User organizer = seedUser(10000, Role.ORGANISER);
    event = seedEvent(0, organizer);
    eventRepository.save(event);
    for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
      participants.add(seedParticipant(10000 + i, seedUser(20000 + i, Role.PARTICIPANT)));
    }
  }

  @Test
  void shouldCompleteAllConcurrentRegistrationsUnderTwoSeconds_WhenFiftyRegisterAtOnce()
      throws Exception {
    runConcurrently(
        CONCURRENT_REQUESTS,
        index -> {
          EventRegistrationCreateRequest request =
              EventRegistrationCreateRequest.builder()
                  .participantId(participants.get(index).getId())
                  .eventId(event.getId())
                  .build();
          MockHttpServletRequestBuilder call =
              authorized(post("/api/registrations"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request));
          return mockMvc.perform(call).andReturn();
        },
        MAX_TOTAL,
        201);
  }
}
