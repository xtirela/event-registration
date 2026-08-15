package repository;

import java.util.Collection;
import model.Event;

public interface EventRepository {
  Event save(Event event);

  Event findById(Integer id);

  Collection<Event> findAll();

  void delete(Integer id);

  boolean existsById(Integer id);

  boolean existsByName(String name);

  int nextId();
}
