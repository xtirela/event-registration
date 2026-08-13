package repository;

import java.util.Collection;
import model.EventRegistration;

public interface EventRegistrationRepository {
  EventRegistration save(EventRegistration eventRegistration);

  EventRegistration findById(Integer id);

  Collection<EventRegistration> findAll();

  void delete(Integer id);

  boolean existsById(Integer id);

  int nextId();

  void addToWaitingQueue(Integer eventId, EventRegistration eventRegistration);

  EventRegistration pollWaitingQueue(Integer eventId);

  void removeFromWaitingQueue(Integer registrationId);

  Collection<EventRegistration> findAllInWaitingQueue();
}
