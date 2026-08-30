package com.eventreg.specification;

import lombok.Data;

@Data
public class SearchCriteria implements Filter {
  private final String type = "SEARCH";

  private String key; // поле: "eventName", "eventDate", "eventStatus"
  private String table; // таблица для JOIN: "organizer" (опционально)
  private String operation; // EQ, LIKE, SEARCH, GR, LO, GT, LT, HAS_FREE_SEATS, IN
  private Object value; // значение
}
