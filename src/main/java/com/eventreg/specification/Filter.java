package com.eventreg.specification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
