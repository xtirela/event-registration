package repository.implementation;

import collection.SimpleArrayList;
import collection.SimpleHashMap;
import java.time.OffsetDateTime;
import java.util.Collection;
import model.Participant;
import model.enums.ParticipantGender;
import repository.CsvFile;
import repository.ParticipantRepository;

public class ParticipantRepositoryImpl implements ParticipantRepository {
  private final SimpleHashMap<Integer, Participant> participants = new SimpleHashMap<>();
  private final String csvPath;
  private int participantCounter = 1;

  public ParticipantRepositoryImpl() {
    this(null);
  }

  public ParticipantRepositoryImpl(String csvPath) {
    this.csvPath = csvPath;
    load();
  }

  private void load() {
    SimpleArrayList<String> lines = CsvFile.read(csvPath);
    for (int i = 0; i < lines.size(); i++) {
      String[] f = lines.get(i).split(",");
      Participant participant =
          Participant.builder()
              .id(Integer.parseInt(f[0]))
              .firstName(f[1])
              .lastName(f[2])
              .email(f[3])
              .age(Integer.parseInt(f[4]))
              .participantGender(ParticipantGender.valueOf(f[5]))
              .registeredAt(OffsetDateTime.parse(f[6]))
              .build();
      participants.put(participant.getId(), participant);
      if (participant.getId() >= participantCounter) {
        participantCounter = participant.getId() + 1;
      }
    }
  }

  private void saveCsv() {
    SimpleArrayList<String> lines = new SimpleArrayList<>();
    for (Participant participant : participants.values()) {
      lines.add(
          String.join(
              ",",
              String.valueOf(participant.getId()),
              participant.getFirstName(),
              participant.getLastName(),
              participant.getEmail(),
              String.valueOf(participant.getAge()),
              participant.getParticipantGender().name(),
              participant.getRegisteredAt().toString()));
    }
    CsvFile.write(csvPath, lines);
  }

  @Override
  public Participant save(Participant participant) {
    Participant saved = participants.put(participant.getId(), participant);
    saveCsv();
    return saved;
  }

  @Override
  public Participant findById(Integer id) {
    return participants.get(id);
  }

  @Override
  public Collection<Participant> findAll() {
    return participants.values();
  }

  @Override
  public void delete(Integer id) {
    participants.remove(id);
    saveCsv();
  }

  @Override
  public boolean existsById(Integer id) {
    return participants.containsKey(id);
  }

  @Override
  public boolean existsByEmail(String email) {
    for (Participant participant : participants.values()) {
      if (participant.getEmail().equals(email)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int nextId() {
    return participantCounter++;
  }
}
