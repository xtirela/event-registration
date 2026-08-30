package com.eventreg.service;

import com.eventreg.exception.EventRegException;
import com.eventreg.model.Event;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class WaitingQueueScheduler {
  private final EventService eventService;
  private final EventRegistrationService registrationService;

  @Scheduled(fixedRate = 60000)
  public void updateAllWaitingQueues() {
    List<Event> events = eventService.findAll(Pageable.unpaged()).getContent();

    for (Event event : events) {
      try {
        eventService.updateCurrentEventReservationStatus(event);
        if (event.getMaxParticipantAmount() - event.getCurrentParticipantAmount() > 0) {
          registrationService.updateWaitingQueueForEvent(event);
        }

        log.info("updated waiting queue for event {}", event.getId());
      } catch (EventRegException e) {
        log.error("Failed to update queue for event: {}", event.getId(), e);
      }
    }
  }
}
