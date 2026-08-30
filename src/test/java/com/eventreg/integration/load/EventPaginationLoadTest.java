package com.eventreg.integration.load;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.eventreg.model.User;
import com.eventreg.model.enums.RBAC.Role;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Load test for {@code GET /api/events} - fetching a paginated event list. */
class EventPaginationLoadTest extends BaseLoadTest {

  private static final Duration MAX_ONE_REQUEST = Duration.ofSeconds(1);
  private static final int REQUESTS = 50;
  private static final int EVENTS_SEED = 1000;

  private User organizer;

  @BeforeEach
  void setUp() throws Exception {
    initLoadTest();
    organizer = seedUser(0, Role.ORGANISER);
    for (int i = 0; i < EVENTS_SEED; i++) {
      seedEvent(i, organizer);
    }
  }

  @Test
  void shouldHandleEachPagedEventsRequestUnderOneSecond_WhenFetchingFiftyTimes() throws Exception {
    List<Long> durations = new ArrayList<>(REQUESTS);
    for (int i = 0; i < REQUESTS; i++) {
      long start = System.nanoTime();
      mockMvc
          .perform(authorized(get("/api/events").param("page", "0").param("size", "20")))
          .andReturn();
      durations.add(System.nanoTime() - start);
    }

    for (long duration : durations) {
      assertUnder(duration, MAX_ONE_REQUEST, "paged events request");
    }
  }
}
