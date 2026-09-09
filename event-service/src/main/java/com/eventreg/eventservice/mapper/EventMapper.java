package com.eventreg.eventservice.mapper;

import com.eventreg.eventservice.dto.request.create.EventCreateRequest;
import com.eventreg.eventservice.dto.request.update.EventUpdateRequest;
import com.eventreg.eventservice.dto.response.EventResponse;
import com.eventreg.eventservice.model.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventMapper {

  EventResponse eventToEventResponse(Event event);

  @Mapping(target = "organizerKeycloakId", source = "organizerKeycloakId")
  Event eventCreateRequestToEvent(
      EventCreateRequest eventCreateRequest, String organizerKeycloakId);

  void eventUpdateRequestToEvent(EventUpdateRequest eventUpdateRequest, @MappingTarget Event event);
}
