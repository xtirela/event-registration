package com.eventreg.mapper;

import com.eventreg.dto.request.update.UserUpdateRequest;
import com.eventreg.dto.response.UserResponse;
import com.eventreg.model.User;
import com.eventreg.security.dto.request.RegisterRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
  @Mapping(target = "participantId", source = "participant.id")
  UserResponse userToUserResponse(User user);

  User userRegisterRequestToUser(RegisterRequest registerRequest);

  void userUpdateRequestToUser(UserUpdateRequest userUpdateRequest, @MappingTarget User user);
}
