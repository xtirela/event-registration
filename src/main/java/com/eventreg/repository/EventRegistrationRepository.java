package com.eventreg.repository;

import com.eventreg.model.EventRegistration;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRegistrationRepository
    extends JpaRepository<EventRegistration, Long>, JpaSpecificationExecutor<EventRegistration> {

  // query waiting status and lowest created at
  @Query(
      value =
"""
    SELECT * FROM event_registrations er WHERE er.event_id = :eventId AND er.event_registration_status = 'WAITING'
    ORDER BY er.created_at ASC
    LIMIT :amount
""",
      nativeQuery = true)
  List<EventRegistration> findWaitingQueue(
      @Param("eventId") Long eventId, @Param("amount") int amount);
}
