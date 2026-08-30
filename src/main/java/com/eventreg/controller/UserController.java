package com.eventreg.controller;

import com.eventreg.annotation.swagger.CollectionErrors;
import com.eventreg.annotation.swagger.NoContentResponseDto;
import com.eventreg.annotation.swagger.PageResponseDto;
import com.eventreg.annotation.swagger.ResourceErrors;
import com.eventreg.annotation.swagger.StandardErrors;
import com.eventreg.annotation.swagger.UserResponseDto;
import com.eventreg.dto.request.update.UserUpdateRequest;
import com.eventreg.dto.response.UserResponse;
import com.eventreg.mapper.UserMapper;
import com.eventreg.security.SecurityGuard;
import com.eventreg.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "User profile management",
    description = "Operations for viewing, updating and deleting user accounts")
@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserMapper userMapper;
  private final SecurityGuard securityGuard;
  private final UserService userService;

  @Operation(description = "Get a page of all users")
  @PageResponseDto
  @CollectionErrors
  @PreAuthorize("hasAuthority('USER_VIEW_ALL')")
  @GetMapping
  public ResponseEntity<Page<UserResponse>> findAll(@PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(userService.findAll(pageable).map(userMapper::userToUserResponse));
  }

  @Operation(description = "Get a user by its id")
  @UserResponseDto
  @ResourceErrors
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('USER_VIEW') && @securityGuard.isSelf(#id)")
  public ResponseEntity<UserResponse> findById(
      @Parameter(description = "ID of the user to fetch", required = true, in = ParameterIn.PATH)
          @PathVariable
          Long id) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(userMapper.userToUserResponse(userService.findById(id)));
  }

  @Operation(description = "Delete a user by its id")
  @NoContentResponseDto
  @ResourceErrors
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('USER_DELETE') && @securityGuard.isSelf(#id)")
  public ResponseEntity<Void> deleteUser(
      @Parameter(description = "ID of the user to delete", required = true, in = ParameterIn.PATH)
          @PathVariable
          Long id,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(description = "Update a user by its id")
  @UserResponseDto
  @StandardErrors
  @PatchMapping("/{id}")
  @PreAuthorize("hasAuthority('USER_UPDATE') && @securityGuard.isSelf(#id)")
  public ResponseEntity<UserResponse> updateUser(
      @Parameter(description = "ID of the user to update", required = true, in = ParameterIn.PATH)
          @PathVariable
          Long id,
      @Parameter(description = "Update user request", required = true, in = ParameterIn.DEFAULT)
          @Valid
          @RequestBody
          UserUpdateRequest userUpdateRequest,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    var result =
        ResponseEntity.status(HttpStatus.OK)
            .body(userMapper.userToUserResponse(userService.updateUser(id, userUpdateRequest)));

    return result;
  }
}
