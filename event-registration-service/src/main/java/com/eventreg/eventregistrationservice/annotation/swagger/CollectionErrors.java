package com.eventreg.eventregistrationservice.annotation.swagger;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Documents standard error responses for collection endpoints. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(
    value = {
      @ApiResponse(
          responseCode = "400",
          description = "Invalid sort or page parameters",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
          responseCode = "403",
          description = "Access denied",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
          responseCode = "500",
          description = "Unexpected error occured",
          content = @Content(schema = @Schema(hidden = true)))
    })
public @interface CollectionErrors {}
