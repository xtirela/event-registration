package com.eventreg.integration.load;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.eventreg.model.Participant;
import com.eventreg.model.enums.RBAC.Role;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Load test for {@code GET /api/participants/{id}} - fetching a single participant by id. */
class ParticipantByIdLoadTest extends BaseLoadTest {

  private static final Duration MAX_AVERAGE = Duration.ofMillis(200);
  private static final int REQUESTS = 100;

  private final List<Long> seededIds = new ArrayList<>(SEED_COUNT);

  @BeforeEach
  void setUp() throws Exception {
    initLoadTest();
    for (int i = 0; i < SEED_COUNT; i++) {
      Participant participant = seedParticipant(i, seedUser(i, Role.PARTICIPANT));
      seededIds.add(participant.getId());
    }
  }

  @Test
  void shouldAverageFetchByIdUnder200ms_WhenFetchingHundredTimesWithThousandRecords()
      throws Exception {
    List<Long> durations = new ArrayList<>(REQUESTS);
    for (int i = 0; i < REQUESTS; i++) {
      long id = seededIds.get(i);
      long start = System.nanoTime();
      mockMvc.perform(authorized(get("/api/participants/" + id))).andReturn();
      durations.add(System.nanoTime() - start);
    }

    assertAverageUnder(durations, MAX_AVERAGE, "fetch participant by id");
  }
}
