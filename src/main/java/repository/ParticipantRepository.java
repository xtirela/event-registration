package repository;

import java.util.Collection;
import model.Participant;

public interface ParticipantRepository {
  Participant save(Participant participant);

  Participant findById(Integer id);

  Collection<Participant> findAll();

  void delete(Integer id);

  boolean existsById(Integer id);

  int nextId();
}
