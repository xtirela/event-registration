package com.eventreg.participantservice.repository;

import com.eventreg.participantservice.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** JPA repository for participant entities. */
@Repository
public interface ParticipantRepository
    extends JpaRepository<Participant, Long>, JpaSpecificationExecutor<Participant> {}
