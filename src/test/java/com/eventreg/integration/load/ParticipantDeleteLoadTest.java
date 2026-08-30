package com.eventreg.integration.load;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import com.eventreg.model.Participant;
import com.eventreg.model.enums.RBAC.Role;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Load test for {@code DELETE /api/participants/{id}} - deleting participant profiles. */
class ParticipantDeleteLoadTest extends BaseLoadTest {

  private static final Duration MAX_ONE_REQUEST = Duration.ofMillis(500);
  private static final int REQUESTS = 50;

  private final List<Long> participantIds = new ArrayList<>(REQUESTS);

  @BeforeEach
  void setUp() throws Exception {
    initLoadTest();
    for (int i = 0; i < REQUESTS; i++) {
      Participant participant = seedParticipant(i, seedUser(i, Role.PARTICIPANT));
      participantIds.add(participant.getId());
    }
  }

  @Test
  void shouldDeleteEachParticipantUnder500ms_WhenDeletingFifty() throws Exception {
    List<Long> durations = new ArrayList<>(REQUESTS);
    for (int i = 0; i < REQUESTS; i++) {
      long id = participantIds.get(i);
      long start = System.nanoTime();
      mockMvc.perform(authorized(delete("/api/participants/" + id))).andReturn();
      durations.add(System.nanoTime() - start);
    }

    for (long duration : durations) {
      assertUnder(duration, MAX_ONE_REQUEST, "delete participant");
    }
  }
}
