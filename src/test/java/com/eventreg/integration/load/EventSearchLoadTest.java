package com.eventreg.integration.load;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.eventreg.model.Event;
import com.eventreg.model.User;
import com.eventreg.model.enums.RBAC.Role;
import com.eventreg.specification.SearchCriteria;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Load test for {@code POST /api/events/search} - filtering events by criteria. */
class EventSearchLoadTest extends BaseLoadTest {

  private static final Duration MAX_ONE_REQUEST = Duration.ofSeconds(1);
  private static final int REQUESTS = 50;
  private static final int EVENTS_SEED = 1000;

  private User organizer;

  @BeforeEach
  void setUp() throws Exception {
    initLoadTest();
    organizer = seedUser(0, Role.ORGANISER);
    for (int i = 0; i < EVENTS_SEED; i++) {
      Event event = seedEvent(i, organizer);
      event.setEventName("Conference " + i + (i % 2 == 0 ? " Tech" : " Workshop"));
      eventRepository.save(event);
    }
  }

  @Test
  void shouldSearchEachWithDifferentFiltersUnderOneSecond_WhenFiftySearches() throws Exception {
    List<Long> durations = new ArrayList<>(REQUESTS);
    for (int i = 0; i < REQUESTS; i++) {
      SearchCriteria criteria = new SearchCriteria();
      criteria.setKey("eventName");
      criteria.setOperation(i % 2 == 0 ? "LIKE" : "SEARCH");
      criteria.setValue(i % 2 == 0 ? "Tech" : "Conference");

      long start = System.nanoTime();
      mockMvc
          .perform(
              authorized(post("/api/events/search"))
                  .param("page", "0")
                  .param("size", "20")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(criteria)))
          .andReturn();
      durations.add(System.nanoTime() - start);
    }

    for (long duration : durations) {
      assertUnder(duration, MAX_ONE_REQUEST, "search events");
    }
  }
}
