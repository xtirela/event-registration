package repository.implementation;

import collection.SimpleArrayList;
import collection.SimpleHashMap;
import collection.SimpleLinkedList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import model.EventRegistration;
import model.enums.EventRegRequestStatus;
import repository.CsvFile;
import repository.EventRegistrationRepository;

public class EventRegistrationRepositoryImpl implements EventRegistrationRepository {
  private final SimpleHashMap<Integer, EventRegistration> registrationRequests =
      new SimpleHashMap<>();

  private final SimpleHashMap<Integer, SimpleLinkedList<EventRegistration>> waitingQueue =
      new SimpleHashMap<>();

  private final String csvPath;

  private int eventRegistrationCounter = 1;

  public EventRegistrationRepositoryImpl() {
    this(null);
  }

  public EventRegistrationRepositoryImpl(String csvPath) {
    this.csvPath = csvPath;
    load();
  }

  private void load() {
    SimpleArrayList<String> lines = CsvFile.read(csvPath);
    for (int i = 0; i < lines.size(); i++) {
      String[] f = lines.get(i).split(",");
      EventRegistration eventRegistration =
          EventRegistration.builder()
              .id(Integer.parseInt(f[0]))
              .participantId(Integer.parseInt(f[1]))
              .eventId(Integer.parseInt(f[2]))
              .eventRegRequestStatus(EventRegRequestStatus.valueOf(f[3]))
              .description(f[4])
              .build();
      registrationRequests.put(eventRegistration.getId(), eventRegistration);
      if (eventRegistration.getId() >= eventRegistrationCounter) {
        eventRegistrationCounter = eventRegistration.getId() + 1;
      }
      if (eventRegistration.getEventRegRequestStatus() == EventRegRequestStatus.WAITING) {
        addToWaitingQueue(eventRegistration.getEventId(), eventRegistration);
      }
    }
  }

  private void saveCsv() {
    SimpleArrayList<String> lines = new SimpleArrayList<>();
    List<EventRegistration> ordered = new ArrayList<>(registrationRequests.values());
    ordered.sort(Comparator.comparingInt(EventRegistration::getId));
    for (EventRegistration eventRegistration : ordered) {
      lines.add(
          String.join(
              ",",
              String.valueOf(eventRegistration.getId()),
              String.valueOf(eventRegistration.getParticipantId()),
              String.valueOf(eventRegistration.getEventId()),
              eventRegistration.getEventRegRequestStatus().name(),
              eventRegistration.getDescription()));
    }
    CsvFile.write(csvPath, lines);
  }

  @Override
  public EventRegistration save(EventRegistration eventRegistration) {
    EventRegistration saved =
        registrationRequests.put(eventRegistration.getId(), eventRegistration);
    saveCsv();
    return saved;
  }

  @Override
  public EventRegistration findById(Integer id) {
    return registrationRequests.get(id);
  }

  @Override
  public List<EventRegistration> findAll() {
    return new ArrayList<>(registrationRequests.values());
  }

  @Override
  public void delete(Integer id) {
    registrationRequests.remove(id);
    saveCsv();
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
    waitingQueue.computeIfAbsent(eventId, k -> new SimpleLinkedList<>()).addLast(eventRegistration);
  }

  @Override
  public EventRegistration pollWaitingQueue(Integer eventId) {
    SimpleLinkedList<EventRegistration> queue = waitingQueue.get(eventId);
    if (queue != null && !queue.isEmpty()) {
      return queue.removeFirst();
    }
    return null;
  }

  @Override
  public void removeFromWaitingQueue(Integer registrationId) {
    for (SimpleLinkedList<EventRegistration> queue : waitingQueue.values()) {
      queue.removeIf(er -> er.getId() == registrationId);
    }
  }

  @Override
  public List<EventRegistration> findAllInWaitingQueue() {
    List<EventRegistration> result = new ArrayList<>();

    for (SimpleLinkedList<EventRegistration> queue : waitingQueue.values()) {
      for (EventRegistration eventRegistration : queue) {
        result.add(eventRegistration);
      }
    }

    return result;
  }
}
