package com.eventreg.model.controller;

import com.eventreg.dto.request.ParticipantCreateRequest;
import com.eventreg.dto.response.ParticipantResponse;
import com.eventreg.model.Event;
import com.eventreg.model.Participant;
import com.eventreg.repository.ParticipantRepository;
import com.eventreg.specification.Filter;
import com.eventreg.specification.SpecificationCreator;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("/api/participant")
@AllArgsConstructor
public class ParticipantController
{
  private final SpecificationCreator specificationCreator;

  private final ParticipantRepository participantRepository;


  @PostMapping
  public ResponseEntity<ParticipantResponse> createParticipant(@RequestBody ParticipantCreateRequest participantCreateRequest)
  {
    return ResponseEntity.status(HttpStatus.OK).body(participantRepository.findById(id));
  }


  @GetMapping
  public ResponseEntity<Page<ParticipantResponse>> findAll(@RequestBody Filter filter, Pageable pageable) {
    Specification<Participant> spec = specificationCreator.create(filter);

    return ResponseEntity.status(HttpStatus.OK).body(participantRepository.findAll(spec, pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Page<ParticipantResponse>> findById(@PathVariable Long id)
  {
    return ResponseEntity.status(HttpStatus.OK).body(participantRepository.findById(id));
  }


}























