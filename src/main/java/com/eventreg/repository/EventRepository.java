package com.eventreg.repository;

import com.eventreg.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository
    extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

  @Query(
      value =
"""
    SELECT COUNT(*) FROM event_registrations WHERE event_id = :id
                                              AND event_registration_status = :eventRegistrationStatus

""",
      nativeQuery = true)
  long countRegistrationsByStatus(
      @Param("id") Long id, @Param("eventRegistrationStatus") String eventRegistrationStatus);
  //  List<Event> findMostPopular(int limit);
}
