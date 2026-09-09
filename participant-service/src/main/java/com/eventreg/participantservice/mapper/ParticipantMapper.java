package com.eventreg.participantservice.mapper;

import com.eventreg.participantservice.dto.request.create.ParticipantCreateRequest;
import com.eventreg.participantservice.dto.request.update.ParticipantUpdateRequest;
import com.eventreg.participantservice.dto.response.ParticipantResponse;
import com.eventreg.participantservice.model.Participant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/** Mapstruct mapper for converting between Participant entities and DTOs. */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ParticipantMapper {

  ParticipantResponse participantToParticipantResponse(Participant participant);

  Participant participantCreateRequestToParticipant(
      ParticipantCreateRequest participantCreateRequest, String keycloakId);

  void participantUpdateRequestToParticipant(
      ParticipantUpdateRequest participantUpdateRequest, @MappingTarget Participant participant);
}
