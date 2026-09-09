package com.eventreg.participantservice.service.implementation;

import com.eventreg.participantservice.annotation.Idempotent;
import com.eventreg.participantservice.dto.request.create.ParticipantCreateRequest;
import com.eventreg.participantservice.dto.request.update.ParticipantUpdateRequest;
import com.eventreg.participantservice.exception.ParticipantNotFoundException;
import com.eventreg.participantservice.mapper.ParticipantMapper;
import com.eventreg.participantservice.model.Participant;
import com.eventreg.participantservice.repository.ParticipantRepository;
import com.eventreg.participantservice.service.ParticipantService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of participant profile management operations. */
@AllArgsConstructor
@Service
public class ParticipantServiceImpl implements ParticipantService {

  private final ParticipantRepository participantRepository;
  private final ParticipantMapper participantMapper;

  @Idempotent
  @Override
  @Transactional
  public Participant createParticipant(
      ParticipantCreateRequest participantCreateRequest, String keycloakId) {
    Participant participant =
        participantMapper.participantCreateRequestToParticipant(
            participantCreateRequest, keycloakId);
    return participantRepository.save(participant);
  }

  @Idempotent
  @Override
  @Transactional
  public Participant updateParticipant(
      Long participantId, ParticipantUpdateRequest participantUpdateRequest) {
    Participant participant = findById(participantId);
    participantMapper.participantUpdateRequestToParticipant(participantUpdateRequest, participant);
    return participantRepository.save(participant);
  }

  @Idempotent
  @Override
  @Transactional
  public void deleteParticipant(Long participantId) {
    Participant participant = findById(participantId);
    participantRepository.delete(participant);
  }

  @Override
  @Transactional(readOnly = true)
  public Participant findById(Long participantId) {
    return participantRepository
        .findById(participantId)
        .orElseThrow(() -> new ParticipantNotFoundException(participantId, "findById"));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Participant> findAll(Specification<Participant> spec, Pageable page) {
    return participantRepository.findAll(spec, page);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Participant> findAll(Pageable page) {
    return participantRepository.findAll(page);
  }
}
