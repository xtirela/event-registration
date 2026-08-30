package com.eventreg.annotation.swagger;

import com.eventreg.dto.response.EventRegistrationResponse;
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
    responseCode = "201",
    description = "Registration created",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = EventRegistrationResponse.class),
            examples =
                @ExampleObject(
                    name = "Registration Created",
                    value =
                        """
                        {
                          "id": 1,
                          "eventId": 3,
                          "participantId": 2,
                          "eventRegistrationStatus": "ACCEPTED",
                          "description": "event registration accepted"
                        }
                        """)))
public @interface EventRegistrationCreatedDto {}
