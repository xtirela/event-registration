package com.eventreg.participantservice.service;

import com.eventreg.participantservice.dto.request.create.ParticipantCreateRequest;
import com.eventreg.participantservice.dto.request.update.ParticipantUpdateRequest;
import com.eventreg.participantservice.model.Participant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/** Service interface for participant profile CRUD operations. */
public interface ParticipantService {
  Participant createParticipant(
      ParticipantCreateRequest participantCreateRequest, String keycloakId);

  Participant updateParticipant(Long id, ParticipantUpdateRequest participantUpdateRequest);

  void deleteParticipant(Long participantId);

  Participant findById(Long participantId);

  Page<Participant> findAll(Specification<Participant> spec, Pageable page);

  Page<Participant> findAll(Pageable page);
}
