package com.eventreg.eventregistrationservice.repository;

import com.eventreg.eventregistrationservice.model.EventRegistration;
import com.eventreg.eventregistrationservice.model.enums.EventRegistrationStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository for event registrations. */
public interface EventRegistrationRepository
    extends JpaRepository<EventRegistration, Long>, JpaSpecificationExecutor<EventRegistration> {

  /** Returns the oldest waiting registrations for an event. */
  @Query(
      value =
          """
          SELECT * FROM event_registrations er
             WHERE er.event_id = :eventId
               AND er.event_registration_status = 'WAITING'
          ORDER BY er.created_at ASC
          LIMIT :amount
          """,
      nativeQuery = true)
  List<EventRegistration> findWaitingQueue(
      @Param("eventId") Long eventId, @Param("amount") int amount);

  /** Counts the registrations of an event that have the given status. */
  @Query(
      value =
          """
          SELECT COUNT(*) FROM event_registrations
             WHERE event_id = :id
               AND event_registration_status = :eventRegistrationStatus
          """,
      nativeQuery = true)
  Long countRegistrationsByStatus(
      @Param("id") Long id, @Param("eventRegistrationStatus") String eventRegistrationStatus);

  /**
   * Updates the status and description of the given registrations as long as they are still
   * waiting.
   *
   * @return the number of updated registrations
   */
  @Modifying
  @Query(
      "UPDATE EventRegistration er SET er.eventRegistrationStatus = :status, "
          + "er.description = :description "
          + "WHERE er.eventId = :eventId AND er.id IN :ids "
          + "AND er.eventRegistrationStatus = :waiting")
  int promoteAll(
      @Param("eventId") Long eventId,
      @Param("ids") List<Long> ids,
      @Param("status") EventRegistrationStatus status,
      @Param("description") String description,
      @Param("waiting") EventRegistrationStatus waiting);
}
