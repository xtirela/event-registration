package repository.implementation;

import collection.SimpleArrayList;
import collection.SimpleHashMap;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import model.Event;
import model.enums.EventGenderRequirement;
import model.enums.EventRegistrationStatus;
import model.enums.EventStatus;
import repository.CsvFile;
import repository.EventRepository;

public class EventRepositoryImpl implements EventRepository {

  private final SimpleHashMap<Integer, Event> events = new SimpleHashMap<>();

  private final String csvPath;

  private int eventsCounter = 1;

  public EventRepositoryImpl() {
    this(null);
  }

  public EventRepositoryImpl(String csvPath) {
    this.csvPath = csvPath;
    load();
  }

  private void load() {
    SimpleArrayList<String> lines = CsvFile.read(csvPath);
    for (int i = 0; i < lines.size(); i++) {
      String[] f = lines.get(i).split(",");
      Event event =
          Event.builder()
              .id(Integer.parseInt(f[0]))
              .eventName(f[1])
              .location(f[2])
              .eventDate(OffsetDateTime.parse(f[3]))
              .eventDuration(Duration.parse(f[4]))
              .ageRequired(Integer.parseInt(f[5]))
              .currentParticipantAmount(Integer.parseInt(f[6]))
              .maxParticipantAmount(Integer.parseInt(f[7]))
              .eventGenderRequirement(EventGenderRequirement.valueOf(f[8]))
              .eventStatus(EventStatus.valueOf(f[9]))
              .eventRegistrationStatus(EventRegistrationStatus.valueOf(f[10]))
              .createdAt(OffsetDateTime.parse(f[11]))
              .build();
      events.put(event.getId(), event);
      if (event.getId() >= eventsCounter) {
        eventsCounter = event.getId() + 1;
      }
    }
  }

  private void saveCsv() {
    SimpleArrayList<String> lines = new SimpleArrayList<>();
    for (Event event : events.values()) {
      lines.add(
          String.join(
              ",",
              String.valueOf(event.getId()),
              event.getEventName(),
              event.getLocation(),
              event.getEventDate().toString(),
              event.getEventDuration().toString(),
              String.valueOf(event.getAgeRequired()),
              String.valueOf(event.getCurrentParticipantAmount()),
              String.valueOf(event.getMaxParticipantAmount()),
              event.getEventGenderRequirement().name(),
              event.getEventStatus().name(),
              event.getEventRegistrationStatus().name(),
              event.getCreatedAt().toString()));
    }
    CsvFile.write(csvPath, lines);
  }

  @Override
  public Event save(Event event) {
    Event saved = events.put(event.getId(), event);
    saveCsv();
    return saved;
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
    saveCsv();
  }

  @Override
  public boolean existsById(Integer id) {
    return events.containsKey(id);
  }

  @Override
  public boolean existsByName(String name) {
    for (Event event : events.values()) {
      if (event.getEventName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int nextId() {
    return eventsCounter++;
  }
}
