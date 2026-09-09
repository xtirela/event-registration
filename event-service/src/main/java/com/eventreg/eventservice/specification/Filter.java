package com.eventreg.eventservice.specification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "Filter criteria for event search. A single SEARCH criterion or an AND/OR group of filters")
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AndFilter.class, name = "AND"),
  @JsonSubTypes.Type(value = OrFilter.class, name = "OR"),
  @JsonSubTypes.Type(value = SearchCriteria.class, name = "SEARCH")
})
public interface Filter {}
