package com.eventreg.eventregistrationservice.annotation.swagger;

import com.eventreg.eventregistrationservice.dto.response.EventRegistrationResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Documents a 200 response containing an event registration. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
    responseCode = "200",
    description = "Registration response",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = EventRegistrationResponse.class),
            examples =
                @ExampleObject(
                    name = "Registration Response",
                    value =
                        """
                        {
                          "id": 1,
                          "eventId": 3,
                          "participantId": 2,
                          "eventRegistrationStatus": "ACCEPTED",
                          "description": "Registration accepted by organizer"
                        }
                        """)))
public @interface EventRegistrationResponseDto {}
