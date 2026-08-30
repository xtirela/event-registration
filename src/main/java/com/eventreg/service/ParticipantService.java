package com.eventreg.service;

import com.eventreg.dto.request.create.ParticipantCreateRequest;
import com.eventreg.dto.request.update.ParticipantUpdateRequest;
import com.eventreg.model.Participant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ParticipantService {
  Participant createParticipant(ParticipantCreateRequest participantCreateRequest);

  Participant updateParticipant(Long id, ParticipantUpdateRequest participantUpdateRequest);

  void deleteParticipant(Long participantId);

  Participant findById(Long participantId);

  Page<Participant> findAll(Specification<Participant> spec, Pageable page);

  Page<Participant> findAll(Pageable page);
}
