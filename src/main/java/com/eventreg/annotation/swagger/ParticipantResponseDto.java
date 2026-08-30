package com.eventreg.annotation.swagger;

import com.eventreg.dto.response.ParticipantResponse;
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
    description = "Participant response",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ParticipantResponse.class),
            examples =
                @ExampleObject(
                    name = "Participant Response",
                    value =
                        """
                        {
                          "id": 1,
                          "firstName": "Ivan",
                          "lastName": "Ivanov",
                          "age": 25,
                          "participantGender": "MALE",
                          "userId": 42
                        }
                        """)))
public @interface ParticipantResponseDto {}
