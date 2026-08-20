package repository.implementation.Jdbc;

import dto.EventSummary;
import exception.SQLEventRegException;
import java.sql.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import model.Event;
import model.enums.EventGenderRequirement;
import model.enums.EventRegistrationStatus;
import model.enums.EventStatus;
import org.postgresql.util.PGInterval;
import repository.EventRepository;
import util.ConnectionManager;

@Slf4j
public class EventRepositoryJdbc implements EventRepository {

  @Override
  public Event save(Event event) {
    String sql =
        """
        INSERT INTO event(event_name, location, event_date, event_duration, age_required,
                          event_gender_requirement, current_participant_amount, max_participant_amount,
                          event_status, event_registration_status, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    Connection connection = ConnectionManager.get();
    try {
      connection.setAutoCommit(false);

      try (var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        PGInterval interval = new PGInterval();
        interval.setHours((int) event.getEventDuration().toHours());
        interval.setMinutes((int) event.getEventDuration().toMinutesPart());

        statement.setString(1, event.getEventName());
        statement.setString(2, event.getLocation());
        statement.setObject(3, event.getEventDate());
        statement.setObject(4, interval);
        statement.setInt(5, event.getAgeRequired());
        statement.setString(6, event.getEventGenderRequirement().name());
        statement.setInt(7, event.getCurrentParticipantAmount());
        statement.setInt(8, event.getMaxParticipantAmount());
        statement.setString(9, event.getEventStatus().name());
        statement.setString(10, event.getEventRegistrationStatus().name());
        statement.setObject(11, event.getCreatedAt());

        log.debug("save event: {}", sql);
        statement.executeUpdate();

        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
          if (generatedKeys.next()) {
            event.setId(generatedKeys.getInt(1));
          }
        }

        connection.commit();
        log.debug("event saved, id={}", event.getId());
        return event;
      }
    } catch (SQLException exception) {
      try {
        connection.rollback();
      } catch (SQLException rollbackException) {
        log.warn("Failed to rollback after event save error", rollbackException);
      }
      throw new SQLEventRegException(
          "Failed to save event with name " + event.getEventName(),
          "EventRepositoryJdbc.save",
          exception);
    } finally {
      try {
        connection.close();
      } catch (SQLException closeException) {
        log.warn("Failed to close connection after event save", closeException);
      }
    }
  }

  @Override
  public Event update(Event event) {
    String sql =
        """
        UPDATE EVENT
        SET event_name = ?,
            location = ?,
            event_date = ?,
            event_duration = ?,
            age_required = ?,
            event_gender_requirement = ?,
            current_participant_amount = ?,
            max_participant_amount = ?,
            event_status = ?,
            event_registration_status = ?
        WHERE id = ?
        """;
    Connection connection = ConnectionManager.get();
    try {
      connection.setAutoCommit(false);

      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        PGInterval interval = new PGInterval();
        interval.setHours((int) event.getEventDuration().toHours());
        interval.setMinutes((int) event.getEventDuration().toMinutesPart());

        statement.setString(1, event.getEventName());
        statement.setString(2, event.getLocation());
        statement.setObject(3, event.getEventDate());
        statement.setObject(4, interval);
        statement.setInt(5, event.getAgeRequired());
        statement.setString(6, event.getEventGenderRequirement().name());
        statement.setInt(7, event.getCurrentParticipantAmount());
        statement.setInt(8, event.getMaxParticipantAmount());
        statement.setString(9, event.getEventStatus().name());
        statement.setString(10, event.getEventRegistrationStatus().name());
        statement.setInt(11, event.getId());

        log.debug("update event: {}", sql);
        statement.executeUpdate();
      }

      connection.commit();
      log.debug("event updated, id={}", event.getId());
      return event;
    } catch (SQLException exception) {
      try {
        connection.rollback();
      } catch (SQLException rollbackException) {
        log.warn("Failed to rollback after event update error", rollbackException);
      }
      throw new SQLEventRegException(
          "Failed to update event " + event.getId(), "EventRepositoryJdbc.update", exception);
    } finally {
      try {
        connection.close();
      } catch (SQLException closeException) {
        log.warn("Failed to close connection after event update", closeException);
      }
    }
  }

  @Override
  public Event findById(Integer id) {
    String sql =
        """
        SELECT * FROM EVENT WHERE id = ?
        """;
    try (Connection connection = ConnectionManager.get();
        var statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);

      log.debug("find event by id={}: {}", id, sql);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return mapEvent(rs);
        }
      }
      return null;
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to find event by id " + id, "EventRepositoryJdbc.findById", exception);
    }
  }

  @Override
  public Collection<Event> findAll() {
    Collection<Event> result = new ArrayList<>();
    String sql =
        """
        SELECT * FROM EVENT
        """;
    try (Connection connection = ConnectionManager.get();
        var statement = connection.prepareStatement(sql)) {
      log.debug("find all events: {}", sql);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          result.add(mapEvent(rs));
        }
      }
      return result;
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to load all events", "EventRepositoryJdbc.findAll", exception);
    }
  }

  @Override
  public void delete(Integer id) {
    String sql =
        """
        DELETE FROM EVENT WHERE id = ?
        """;
    try (Connection connection = ConnectionManager.get();
        var statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      log.debug("delete event id={}: {}", id, sql);
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to delete event with id " + id, "EventRepositoryJdbc.delete", exception);
    }
  }

  @Override
  public boolean existsById(Integer id) {
    String sql = "SELECT * FROM EVENT WHERE id = ?";
    try (Connection connection = ConnectionManager.get();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      log.debug("exists event by id={}: {}", id, sql);
      try (ResultSet rs = statement.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to check event existence by id " + id,
          "EventRepositoryJdbc.existsById",
          exception);
    }
  }

  @Override
  public boolean existsByName(String name) {
    String sql = "SELECT * FROM EVENT WHERE event_name = ?";
    try (Connection connection = ConnectionManager.get();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, name);
      log.debug("exists event by name={}: {}", name, sql);
      try (ResultSet rs = statement.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to check event existence by name " + name,
          "EventRepositoryJdbc.existsByName",
          exception);
    }
  }

  @Override
  public EventSummary getEventSummary(int eventId) {
    String sql =
        """
              SELECT event_name, current_participant_amount,
                     max_participant_amount - current_participant_amount AS free_spots,
              (SELECT COUNT(*) FROM public.event_registration
              WHERE event_id = ? AND event_reg_request_status = 'WAITING')
                  AS waitlist_amount, (current_participant_amount::DOUBLE PRECISION) /max_participant_amount AS percent_fill
              FROM EVENT
              WHERE id = ?;
              """;
    try (Connection connection = ConnectionManager.get();
        var statement = connection.prepareStatement(sql)) {
      statement.setInt(1, eventId);
      statement.setInt(2, eventId);

      log.debug("get event summary={}: {}", eventId, sql);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return EventSummary.builder()
              .eventName(rs.getString(1))
              .currentParticipantAmount(rs.getInt(2))
              .freeSpotAmount(rs.getInt(3))
              .waitlistSize(rs.getInt(4))
              .fillPercent(rs.getDouble(5))
              .build();
        }
      }
      return null;
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed getEventSummary " + eventId, "EventRepositoryJdbc.getEventSummary", exception);
    }
  }

  @Override
  public Map<String, Long> groupByFillStatus() {
    Map<String, Long> result = new HashMap<>();

    String sql =
        """
                        SELECT event_registration_status, COUNT(current_participant_amount) FROM public.event
                        GROUP BY event_registration_status
                        ORDER BY event_registration_status
                        """;
    try (Connection connection = ConnectionManager.get();
        var statement = connection.prepareStatement(sql)) {

      log.debug("groupByFillStatus {}", sql);

      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          result.put(rs.getString(1), rs.getLong(2));
        }
      }
      return result;
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed groupByFillStatus ", "EventRepositoryJdbc.groupByFillStatus", exception);
    }
  }

  @Override
  public List<Event> findMostPopular(int limit) {

    List<Event> result = new ArrayList<>();

    String sql =
        """
                SELECT * FROM EVENT
                ORDER BY current_participant_amount DESC LIMIT ?
                """;
    try (Connection connection = ConnectionManager.get();
        var statement = connection.prepareStatement(sql)) {
      statement.setInt(1, limit);

      log.debug("find most popular={}: {}", limit, sql);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          result.add(mapEvent(rs));
        }
      }
      return result;
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to find events ", "EventRepositoryJdbc.findMostPopular", exception);
    }
  }

  private Event mapEvent(ResultSet rs) throws SQLException {
    PGInterval interval = (PGInterval) rs.getObject("event_duration");
    Duration duration = Duration.ofHours(interval.getHours()).plusMinutes(interval.getMinutes());

    return Event.builder()
        .id(rs.getInt("id"))
        .eventName(rs.getString("event_name"))
        .location(rs.getString("location"))
        .eventDate(rs.getObject("event_date", OffsetDateTime.class))
        .eventDuration(duration)
        .ageRequired(rs.getInt("age_required"))
        .eventGenderRequirement(
            EventGenderRequirement.fromString(rs.getString("event_gender_requirement")))
        .currentParticipantAmount(rs.getInt("current_participant_amount"))
        .maxParticipantAmount(rs.getInt("max_participant_amount"))
        .eventStatus(EventStatus.fromString(rs.getString("event_status")))
        .eventRegistrationStatus(
            EventRegistrationStatus.fromString(rs.getString("event_registration_status")))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
        .build();
  }
}
