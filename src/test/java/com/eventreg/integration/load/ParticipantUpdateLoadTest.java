package com.eventreg.integration.load;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import com.eventreg.dto.request.update.ParticipantUpdateRequest;
import com.eventreg.model.Participant;
import com.eventreg.model.enums.RBAC.Role;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Load test for {@code PATCH /api/participants/{id}} - updating a participant profile. */
class ParticipantUpdateLoadTest extends BaseLoadTest {

  private static final Duration MAX_ONE_REQUEST = Duration.ofMillis(500);
  private static final int REQUESTS = 50;

  private Participant participant;

  @BeforeEach
  void setUp() throws Exception {
    initLoadTest();
    participant = seedParticipant(0, seedUser(0, Role.PARTICIPANT));
  }

  @Test
  void shouldUpdateEachParticipantUnder500ms_WhenUpdatingFiftyTimes() throws Exception {
    List<Long> durations = new ArrayList<>(REQUESTS);
    for (int i = 0; i < REQUESTS; i++) {
      ParticipantUpdateRequest request =
          ParticipantUpdateRequest.builder().firstName("Updated" + i).build();

      long start = System.nanoTime();
      mockMvc
          .perform(
              authorized(patch("/api/participants/" + participant.getId()))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request)))
          .andReturn();
      durations.add(System.nanoTime() - start);
    }

    for (long duration : durations) {
      assertUnder(duration, MAX_ONE_REQUEST, "update participant");
    }
  }
}
