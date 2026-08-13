package repository.implementation;

import java.util.*;
import model.EventRegistration;
import repository.EventRegistrationRepository;

public class EventRegistrationRepositoryImpl implements EventRegistrationRepository {
  private final HashMap<Integer, EventRegistration> registrationRequests = new HashMap<>();

  private final HashMap<Integer, Deque<EventRegistration>> waitingQueue = new HashMap<>();

  private int eventRegistrationCounter = 1;

  @Override
  public EventRegistration save(EventRegistration eventRegistration) {
    return registrationRequests.put(eventRegistration.getId(), eventRegistration);
  }

  @Override
  public EventRegistration findById(Integer id) {
    return registrationRequests.get(id);
  }

  @Override
  public Collection<EventRegistration> findAll() {
    return registrationRequests.values();
  }

  @Override
  public void delete(Integer id) {
    registrationRequests.remove(id);
  }

  @Override
  public boolean existsById(Integer id) {
    return registrationRequests.containsKey(id);
  }

  @Override
  public int nextId() {
    return eventRegistrationCounter++;
  }

  @Override
  public void addToWaitingQueue(Integer eventId, EventRegistration eventRegistration) {
    waitingQueue.computeIfAbsent(eventId, k -> new ArrayDeque<>()).addLast(eventRegistration);
  }

  @Override
  public EventRegistration pollWaitingQueue(Integer eventId) {
    Deque<EventRegistration> queue = waitingQueue.get(eventId);
    if (queue != null && !queue.isEmpty()) {
      return queue.removeFirst();
    }
    return null;
  }

  @Override
  public void removeFromWaitingQueue(Integer registrationId) {
    waitingQueue
        .values()
        .forEach(queue -> queue.removeIf(er -> Integer.valueOf(er.getId()).equals(registrationId)));
    waitingQueue.values().removeIf(Collection::isEmpty);
  }

  @Override
  public List<EventRegistration> findAllInWaitingQueue() {
    return waitingQueue.values().stream().flatMap(Collection::stream).toList();
  }
}
