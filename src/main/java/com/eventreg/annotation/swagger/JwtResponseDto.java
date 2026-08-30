package com.eventreg.annotation.swagger;

import com.eventreg.security.dto.response.JwtResponse;
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
    description = "JWT token returned",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = JwtResponse.class),
            examples =
                @ExampleObject(
                    name = "JWT Token",
                    value =
                        """
                        {
                          "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJpdmFub3YifQ.example"
                        }
                        """)))
public @interface JwtResponseDto {}
