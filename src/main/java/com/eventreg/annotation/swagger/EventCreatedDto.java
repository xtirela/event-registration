package com.eventreg.annotation.swagger;

import com.eventreg.dto.response.EventResponse;
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
    description = "Event created",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = EventResponse.class),
            examples =
                @ExampleObject(
                    name = "Event Created",
                    value =
                        """
                        {
                          "id": 1,
                          "eventName": "Tech Conference",
                          "eventDescription": "Annual tech meetup",
                          "eventDate": "2026-10-01T18:00:00+03:00",
                          "location": "Moscow, Tverskaya 1",
                          "eventDuration": "PT2H",
                          "ageRequired": 18,
                          "eventGenderRequirement": "NONE",
                          "maxParticipantAmount": 100,
                          "currentParticipantAmount": 0,
                          "currentWaitingQueueParticipantAmount": 0,
                          "eventStatus": "PLANNED",
                          "eventReservationStatus": "RESERVATIONS_OPEN",
                          "confirmationRequired": false,
                          "organizerId": 5
                        }
                        """)))
public @interface EventCreatedDto {}
