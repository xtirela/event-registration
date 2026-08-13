package repository.implementation;

import java.util.Collection;
import java.util.HashMap;
import model.Participant;
import repository.ParticipantRepository;

public class ParticipantRepositoryImpl implements ParticipantRepository {
  private final HashMap<Integer, Participant> participants = new HashMap<Integer, Participant>();
  private int participantCounter = 1;

  @Override
  public Participant save(Participant participant) {
    return participants.put(participant.getId(), participant);
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
  }

  @Override
  public boolean existsById(Integer id) {
    return participants.containsKey(id);
  }

  @Override
  public int nextId() {
    return participantCounter++;
  }
}
