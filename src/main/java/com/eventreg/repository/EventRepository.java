package com.eventreg.repository;

import com.eventreg.dto.response.EventSummaryResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.eventreg.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event>
{
  Event save(Event event);

  Event update(Event event);

  Event findById(Integer id);

  Collection<Event> findAll();

  void delete(Integer id);

  boolean existsById(Integer id);

  boolean existsByName(String name);

  EventSummaryResponse getEventSummary(int eventId);

  Map<String, Long> groupByFillStatus();

  List<Event> findMostPopular(int limit);
}
