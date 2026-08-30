package com.eventreg.service.implementation;

import com.eventreg.annotation.Idempotent;
import com.eventreg.dto.request.create.ParticipantCreateRequest;
import com.eventreg.dto.request.update.ParticipantUpdateRequest;
import com.eventreg.exception.ParticipantNotFoundException;
import com.eventreg.mapper.ParticipantMapper;
import com.eventreg.model.Participant;
import com.eventreg.model.User;
import com.eventreg.repository.ParticipantRepository;
import com.eventreg.service.ParticipantService;
import com.eventreg.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class ParticipantServiceImpl implements ParticipantService {

  private final ParticipantRepository participantRepository;
  private final UserService userService;
  private final ParticipantMapper participantMapper;

  @Idempotent
  @Override
  @Transactional
  public Participant createParticipant(ParticipantCreateRequest participantCreateRequest) {
    Participant participant =
        participantMapper.participantCreateRequestToParticipant(participantCreateRequest);

    User user = userService.findById(participantCreateRequest.getUserId());
    participant.setUser(user);

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
