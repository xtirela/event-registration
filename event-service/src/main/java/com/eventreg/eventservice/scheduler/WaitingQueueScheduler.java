package com.eventreg.eventservice.scheduler;

import com.eventreg.eventservice.model.Event;
import com.eventreg.eventservice.service.EventService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WaitingQueueScheduler {
  private final EventService eventService;

  @Value("${waiting-queue.scheduler.limit:100}")
  private int waitingQueueLimit;

  public WaitingQueueScheduler(EventService eventService) {
    this.eventService = eventService;
  }

  @Scheduled(fixedRateString = "${waiting-queue.scheduler.fixed-rate-ms:60000}")
  public void updateAllWaitingQueues() {
    List<Event> events = eventService.findAll(Pageable.unpaged()).getContent();

    for (Event event : events) {
      try {
        eventService.updateCurrentEventReservationStatus(event);
        if (event.getMaxParticipantAmount() - event.getCurrentParticipantAmount() > 0) {
          eventService.promoteWaitingQueue(event.getId(), waitingQueueLimit);
        }

        log.info("updated waiting queue for event {}", event.getId());
      } catch (Exception e) {
        log.error("Failed to update queue for event: {}", event.getId(), e);
      }
    }
  }
}
