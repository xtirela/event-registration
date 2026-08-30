package com.eventreg.mapper;

import com.eventreg.dto.request.create.ParticipantCreateRequest;
import com.eventreg.dto.request.update.ParticipantUpdateRequest;
import com.eventreg.dto.response.ParticipantResponse;
import com.eventreg.model.Participant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ParticipantMapper {
  @Mapping(target = "userId", source = "user.id")
  ParticipantResponse participantToParticipantResponse(Participant participant);

  Participant participantCreateRequestToParticipant(
      ParticipantCreateRequest participantCreateRequest);

  void participantUpdateRequestToParticipant(
      ParticipantUpdateRequest participantUpdateRequest, @MappingTarget Participant participant);
}
