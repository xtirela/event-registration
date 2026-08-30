package com.eventreg.annotation.swagger;

import com.eventreg.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
    responseCode = "200",
    description = "User response",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = UserResponse.class),
            examples =
                @ExampleObject(
                    name = "User Response",
                    value =
                        """
                        {
                          "id": 1,
                          "username": "ivanov",
                          "email": "ivan@example.com",
                          "role": "PARTICIPANT",
                          "participantId": 2
                        }
                        """)))
public @interface UserResponseDto {}
