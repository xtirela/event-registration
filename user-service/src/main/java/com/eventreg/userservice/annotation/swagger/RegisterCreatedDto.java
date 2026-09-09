package com.eventreg.userservice.annotation.swagger;

import com.eventreg.userservice.dto.response.RegisterResponse;
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
    description = "User registered",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = RegisterResponse.class),
            examples =
                @ExampleObject(
                    name = "User Registered",
                    value =
                        """
                        {
                          "message": "User registered successfully"
                        }
                        """)))
public @interface RegisterCreatedDto {}
