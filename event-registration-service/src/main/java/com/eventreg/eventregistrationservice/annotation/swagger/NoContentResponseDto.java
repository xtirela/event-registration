package com.eventreg.eventregistrationservice.annotation.swagger;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Documents a 204 no content response for delete endpoints. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
    responseCode = "204",
    description = "Resource deleted",
    content = @Content(schema = @Schema(hidden = true)))
public @interface NoContentResponseDto {}
