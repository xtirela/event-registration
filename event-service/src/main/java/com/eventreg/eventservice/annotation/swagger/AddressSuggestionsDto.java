package com.eventreg.eventservice.annotation.swagger;

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
    description = "Address suggestions",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(hidden = true),
            examples =
                @ExampleObject(
                    name = "Address Suggestions",
                    value =
                        """
                                [
                                  "г. Москва, ул. Тверская, д. 1",
                                  "г. Москва, Тверской бульвар, д. 2"
                                ]
                                """)))
public @interface AddressSuggestionsDto {}
