package com.eventreg.userservice.annotation.swagger;

import com.eventreg.userservice.dto.response.LoginResponse;
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
    description = "Login successful",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = LoginResponse.class),
            examples =
                @ExampleObject(
                    name = "Token Pair",
                    value =
                        """
                        {
                          "accessToken": "eyJhbGciOi...",
                          "refreshToken": "eyJhbGciOi...",
                          "expiresIn": 300,
                          "tokenType": "Bearer"
                        }
                        """)))
public @interface LoginResponseDto {}
