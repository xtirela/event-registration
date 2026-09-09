package com.eventreg.eventregistrationservice.mapper;

import com.eventreg.eventregistrationservice.dto.request.update.EventRegistrationStatusUpdateRequest;
import com.eventreg.eventregistrationservice.dto.response.EventRegistrationResponse;
import com.eventreg.eventregistrationservice.model.EventRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/** Mapstruct mapper between event registration entities and DTOs. */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventRegistrationMapper {
  EventRegistrationResponse eventRegistrationToEventRegistrationResponse(
      EventRegistration eventRegistration);

  void eventRegistrationStatusUpdateRequestToEventRegistration(
      EventRegistrationStatusUpdateRequest eventRegistrationStatusUpdateRequest,
      @MappingTarget EventRegistration eventRegistration);
}
