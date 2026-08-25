package com.eventreg.repository;

import java.util.Collection;
import java.util.List;
import com.eventreg.model.Participant;
import com.eventreg.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long>, JpaSpecificationExecutor<Participant>
{

  List<Participant> searchByFragment(String fragment);

  boolean existsByEmail(String email);
}
