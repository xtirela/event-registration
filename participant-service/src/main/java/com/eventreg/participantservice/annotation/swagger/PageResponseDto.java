package com.eventreg.participantservice.annotation.swagger;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Swagger annotation that documents a paged response body. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
    responseCode = "200",
    description = "Page of resources",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(hidden = true),
            examples =
                @ExampleObject(
                    name = "Page",
                    value =
                        """
                        {
                          "content": [
                            {
                              "id": 1
                            }
                          ],
                          "totalPages": 1,
                          "totalElements": 1
                        }
                        """)))
public @interface PageResponseDto {}
