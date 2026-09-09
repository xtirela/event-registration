package com.eventreg.participantservice.annotation.swagger;

import com.eventreg.participantservice.dto.response.ParticipantResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Swagger annotation that documents a 201 participant-created response body. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
    responseCode = "201",
    description = "Participant created",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ParticipantResponse.class),
            examples =
                @ExampleObject(
                    name = "Participant Created",
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
public @interface ParticipantCreatedDto {}
