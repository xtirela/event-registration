package com.eventreg.eventservice.repository;

import com.eventreg.eventservice.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository
    extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "UPDATE Event e SET e.currentParticipantAmount = e.currentParticipantAmount + 1 "
          + "WHERE e.id = :eventId AND e.currentParticipantAmount < e.maxParticipantAmount")
  int tryAcquireSeat(@Param("eventId") Long eventId);
}
