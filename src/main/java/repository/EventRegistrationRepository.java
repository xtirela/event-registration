package repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import model.EventRegistration;

public interface EventRegistrationRepository {
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
