package com.eventreg.mapper;

import com.eventreg.dto.request.create.EventCreateRequest;
import com.eventreg.dto.request.update.EventUpdateRequest;
import com.eventreg.dto.response.EventResponse;
import com.eventreg.model.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventMapper {
  @Mapping(target = "organizerId", expression = "java(event.getOrganizer().getId())")
  EventResponse eventToEventResponse(Event event);

  Event eventCreateRequestToEvent(EventCreateRequest eventCreateRequest);

  void eventUpdateRequestToEvent(EventUpdateRequest eventCreateRequest, @MappingTarget Event event);
}
