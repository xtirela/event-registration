package com.eventreg.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import com.eventreg.model.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long>, JpaSpecificationExecutor<EventRegistration>
{
  EventRegistration save(EventRegistration eventRegistration);

  EventRegistration update(EventRegistration eventRegistration);

  EventRegistration findById(Integer id);

  Collection<EventRegistration> findAll();

  void delete(Integer id);

  boolean existsById(Integer id);

  void addToWaitingQueue(EventRegistration eventRegistration);

  EventRegistration pollWaitingQueue(Integer eventId);

  void removeFromWaitingQueue(Integer registrationId);

  Collection<EventRegistration> findAllInWaitingQueue();

  List<EventRegistration> findByCreatedBetween(OffsetDateTime from, OffsetDateTime to);
}
