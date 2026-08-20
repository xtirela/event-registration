package repository;

import java.util.Collection;
import java.util.List;
import model.Participant;

public interface ParticipantRepository {
  Participant save(Participant participant);

  Participant update(Participant participant);

  Participant findById(Integer id);

  Collection<Participant> findAll();

  List<Participant> searchByFragment(String fragment);

  void delete(Integer id);

  boolean existsById(Integer id);

  boolean existsByEmail(String email);
}
