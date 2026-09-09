package com.eventreg.eventservice.specification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "A single search criterion applied to one event field")
public class SearchCriteria implements Filter {
  private final String type = "SEARCH";

  @Schema(
      description = "Event field to filter on: eventName, eventDate, eventStatus",
      example = "eventName")
  private String key; // поле: "eventName", "eventDate", "eventStatus"

  @Schema(description = "Table for a JOIN: \"organizer\" (optional)", example = "organizer")
  private String table; // таблица для JOIN: "organizer" (опционально)

  @Schema(
      description = "Comparison operation: EQ, LIKE, SEARCH, GR, LO, GT, LT, HAS_FREE_SEATS, IN",
      example = "LIKE")
  private String operation; // EQ, LIKE, SEARCH, GR, LO, GT, LT, HAS_FREE_SEATS, IN

  @Schema(description = "Value to compare against", example = "Tech")
  private Object value; // значение
}
