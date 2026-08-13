package repository.implementation;

import java.util.Collection;
import java.util.HashMap;
import model.Event;
import repository.EventRepository;

public class EventRepositoryImpl implements EventRepository {

  private final HashMap<Integer, Event> events = new HashMap<Integer, Event>();

  private int eventsCounter = 1;

  @Override
  public Event save(Event event) {
    return events.put(event.getId(), event);
  }

  @Override
  public Event findById(Integer id) {
    return events.get(id);
  }

  @Override
  public Collection<Event> findAll() {
    return events.values();
  }

  @Override
  public void delete(Integer id) {
    events.remove(id);
  }

  @Override
  public boolean existsById(Integer id) {
    return events.containsKey(id);
  }

  @Override
  public int nextId() {
    return eventsCounter++;
  }
}
