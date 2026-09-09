package com.eventreg.eventservice.specification;

import com.eventreg.eventservice.model.Event;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class EventSpecificationCreator {

  public Specification<Event> create(Filter filter) {
    return switch (filter) {
      case null -> emptySpecification();
      case AndFilter andFilter ->
          createAndSpecification(createInnerSpecifications(andFilter.getValue()));
      case OrFilter orFilter ->
          createOrSpecification(createInnerSpecifications(orFilter.getValue()));
      case SearchCriteria criteria -> createSearchSpecification(criteria);
      default -> emptySpecification();
    };
  }

  // Пустая спецификация (без deprecated where(null))
  private Specification<Event> emptySpecification() {
    return (root, query, builder) -> builder.conjunction();
  }

  private List<Specification<Event>> createInnerSpecifications(List<Filter> filters) {
    List<Specification<Event>> specs = new ArrayList<>();

    if (filters != null) {
      for (Filter filter : filters) {
        specs.add(create(filter));
      }
    }

    return specs;
  }

  private Specification<Event> createAndSpecification(List<Specification<Event>> specs) {
    if (specs.isEmpty()) {
      return emptySpecification();
    }

    Specification<Event> result = specs.getFirst();
    for (int i = 1; i < specs.size(); i++) {
      result = result.and(specs.get(i));
    }
    return result;
  }

  private Specification<Event> createOrSpecification(List<Specification<Event>> specs) {
    if (specs.isEmpty()) {
      return emptySpecification();
    }

    Specification<Event> result = specs.getFirst();
    for (int i = 1; i < specs.size(); i++) {
      result = result.or(specs.get(i));
    }
    return result;
  }

  private Specification<Event> createSearchSpecification(SearchCriteria criteria) {
    return (root, query, builder) -> {
      Join<Object, Object> join = null;

      if (criteria.getTable() != null && !criteria.getTable().isEmpty()) {
        join = root.join(criteria.getTable());
      }

      Path<Object> path =
          (join != null) ? join.get(criteria.getKey()) : root.get(criteria.getKey());

      switch (criteria.getOperation().toUpperCase()) {
        case "GR" -> {
          return builder.greaterThanOrEqualTo(
              path.as(String.class), criteria.getValue().toString());
        }

        case "LO" -> {
          return builder.lessThanOrEqualTo(path.as(String.class), criteria.getValue().toString());
        }

        case "EQ" -> {
          return builder.equal(path, criteria.getValue());
        }

        case "LIKE" -> {
          String pattern = "%" + criteria.getValue().toString().toLowerCase() + "%";
          return builder.like(builder.lower(path.as(String.class)), pattern);
        }

        case "SEARCH" -> {
          return createFullTextSearch(root, criteria.getValue().toString(), builder);
        }

        case "HAS_FREE_SEATS" -> {
          return createHasFreeSeats(root, builder);
        }

        case "IN" -> {
          if (criteria.getValue() instanceof List<?> values) {
            return path.in(values);
          }
          return null;
        }

        default -> {
          return null;
        }
      }
    };
  }

  private Predicate createFullTextSearch(
      jakarta.persistence.criteria.Root<Event> root,
      String searchTerm,
      jakarta.persistence.criteria.CriteriaBuilder builder) {

    String pattern = "%" + searchTerm.toLowerCase() + "%";

    return builder.or(
        builder.like(builder.lower(root.get("eventName")), pattern),
        builder.like(builder.lower(root.get("eventDescription")), pattern));
  }

  private Predicate createHasFreeSeats(
      jakarta.persistence.criteria.Root<Event> root,
      jakarta.persistence.criteria.CriteriaBuilder builder) {

    return builder.greaterThan(
        builder.diff(root.get("maxParticipantAmount"), root.get("currentParticipantAmount")), 0);
  }
}
