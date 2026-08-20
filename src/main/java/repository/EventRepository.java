package repository;

import dto.EventSummary;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import model.Event;

public interface EventRepository {
  Event save(Event event);

  Event update(Event event);

  Event findById(Integer id);

  Collection<Event> findAll();

  void delete(Integer id);

  boolean existsById(Integer id);

  boolean existsByName(String name);

  EventSummary getEventSummary(int eventId);

  Map<String, Long> groupByFillStatus();

  List<Event> findMostPopular(int limit);
}
