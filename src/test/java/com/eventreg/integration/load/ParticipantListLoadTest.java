package com.eventreg.integration.load;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.eventreg.model.enums.RBAC.Role;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Load test for {@code GET /api/participants} - fetching a large participant list. */
class ParticipantListLoadTest extends BaseLoadTest {

  private static final Duration MAX_ONE_REQUEST = Duration.ofSeconds(1);
  private static final int REQUESTS = 100;

  @BeforeEach
  void setUp() throws Exception {
    initLoadTest();
    for (int i = 0; i < SEED_COUNT; i++) {
      seedParticipant(i, seedUser(i, Role.PARTICIPANT));
    }
  }

  @Test
  void shouldHandleEachListRequestUnderOneSecond_WhenFetchingHundredTimesWithThousandRecords()
      throws Exception {
    runRequests(
        REQUESTS,
        index -> mockMvc.perform(authorized(get("/api/participants"))).andReturn(),
        MAX_ONE_REQUEST,
        200);
  }

  @Test
  void shouldKeepListResponseStableOverTime_WhenRepeatedlyFetchingLargeDataset() throws Exception {
    List<Long> durations =
        runRequests(
            REQUESTS,
            index -> mockMvc.perform(authorized(get("/api/participants"))).andReturn(),
            MAX_ONE_REQUEST,
            200);

    long firstHalfAvg = averageMillis(durations.subList(0, REQUESTS / 2));
    long secondHalfAvg = averageMillis(durations.subList(REQUESTS / 2, REQUESTS));
    if (secondHalfAvg > firstHalfAvg * 2) {
      throw new AssertionError(
          "list degraded over time: first-half avg "
              + firstHalfAvg
              + "ms, second-half avg "
              + secondHalfAvg
              + "ms");
    }
  }

  private long averageMillis(List<Long> durations) {
    return (long)
        (durations.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0);
  }
}
