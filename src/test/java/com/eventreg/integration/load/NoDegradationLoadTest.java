package com.eventreg.integration.load;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.eventreg.model.enums.RBAC.Role;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Load test asserting that response time does not degrade over many repeated requests. */
class NoDegradationLoadTest extends BaseLoadTest {

  private static final int REQUESTS = 2000;
  private static final int WARMUP = 100;
  private static final int COMPARED_WINDOW = 100;
  private static final Duration MAX_ONE_REQUEST = Duration.ofSeconds(1);

  @BeforeEach
  void setUp() throws Exception {
    initLoadTest();
    for (int i = 0; i < SEED_COUNT; i++) {
      seedParticipant(i, seedUser(i, Role.PARTICIPANT));
    }
  }

  @Test
  void shouldNotDegradeOverTime_WhenExecutingTwoThousandReads() throws Exception {
    List<Long> durations = new ArrayList<>(REQUESTS);
    for (int i = 0; i < REQUESTS; i++) {
      long start = System.nanoTime();
      mockMvc.perform(authorized(get("/api/participants"))).andReturn();
      durations.add(System.nanoTime() - start);
    }

    List<Long> early = durations.subList(WARMUP, WARMUP + COMPARED_WINDOW);
    List<Long> late = durations.subList(REQUESTS - COMPARED_WINDOW, REQUESTS);

    long earlyAvg = averageMillis(early);
    long lateAvg = averageMillis(late);
    if (lateAvg > earlyAvg * 3) {
      throw new AssertionError(
          "performance degraded over time: early avg "
              + earlyAvg
              + "ms, late avg "
              + lateAvg
              + "ms");
    }
  }

  private long averageMillis(List<Long> durations) {
    return (long)
        (durations.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0);
  }
}
