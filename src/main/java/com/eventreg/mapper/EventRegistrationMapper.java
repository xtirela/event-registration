package com.eventreg.mapper;

import com.eventreg.dto.request.update.EventRegistrationStatusUpdateRequest;
import com.eventreg.dto.response.EventRegistrationResponse;
import com.eventreg.model.EventRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventRegistrationMapper {
  @Mapping(target = "participantId", source = "participant.id")
  @Mapping(target = "eventId", source = "event.id")
  EventRegistrationResponse eventRegistrationToEventRegistrationResponse(
      EventRegistration eventRegistration);

  void eventRegistrationStatusUpdateRequestToEventRegistration(
      EventRegistrationStatusUpdateRequest eventRegistrationStatusUpdateRequest,
      @MappingTarget EventRegistration eventRegistration);
}
