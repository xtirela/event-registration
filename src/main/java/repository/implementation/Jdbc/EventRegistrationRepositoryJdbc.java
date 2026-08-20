package repository.implementation.Jdbc;

import exception.SQLEventRegException;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import model.EventRegistration;
import model.enums.EventRegRequestStatus;
import repository.EventRegistrationRepository;
import util.ConnectionManager;

@Slf4j
public class EventRegistrationRepositoryJdbc implements EventRegistrationRepository {

  @Override
  public EventRegistration save(EventRegistration eventRegistration) {
    String sql =
        """
           INSERT INTO EVENT_REGISTRATION(participant_id, event_id, event_reg_request_status, description, created_at)
           VALUES(?, ?, ?, ?, ?)
            """;
    Connection connection = ConnectionManager.get();
    try {
      connection.setAutoCommit(false);

      try (PreparedStatement statement =
          connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setInt(1, eventRegistration.getParticipantId());
        statement.setInt(2, eventRegistration.getEventId());
        statement.setString(3, eventRegistration.getEventRegRequestStatus().name());
        statement.setString(4, eventRegistration.getDescription());
        statement.setObject(5, eventRegistration.getCreatedAt());

        log.debug("save registration: {}", sql);
        statement.executeUpdate();

        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
          if (generatedKeys.next()) {
            eventRegistration.setId(generatedKeys.getInt(1));
          }
        }

        connection.commit();
        log.debug("registration saved, id={}", eventRegistration.getId());
        return eventRegistration;
      }
    } catch (SQLException exception) {
      try {
        connection.rollback();
      } catch (SQLException rollbackException) {
        log.warn("Failed to rollback after registration save error", rollbackException);
      }
      throw new SQLEventRegException(
          "Failed to save registration for participant "
              + eventRegistration.getParticipantId()
              + " on event "
              + eventRegistration.getEventId(),
          "EventRegistrationRepositoryJdbc.save",
          exception);
    } finally {
      try {
        connection.close();
      } catch (SQLException closeException) {
        log.warn("Failed to close connection after registration save", closeException);
      }
    }
  }

  @Override
  public EventRegistration update(EventRegistration eventRegistration) {
    String sql =
        """
           UPDATE EVENT_REGISTRATION
           SET participant_id = ?,
               event_id = ?,
               event_reg_request_status = ?,
               description = ?
           WHERE id = ?
            """;
    Connection connection = ConnectionManager.get();
    try {
      connection.setAutoCommit(false);

      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setInt(1, eventRegistration.getParticipantId());
        statement.setInt(2, eventRegistration.getEventId());
        statement.setString(3, eventRegistration.getEventRegRequestStatus().name());
        statement.setString(4, eventRegistration.getDescription());
        statement.setInt(5, eventRegistration.getId());

        log.debug("update registration: {}", sql);
        statement.executeUpdate();
      }

      connection.commit();
      log.debug("registration updated, id={}", eventRegistration.getId());
      return eventRegistration;
    } catch (SQLException exception) {
      try {
        connection.rollback();
      } catch (SQLException rollbackException) {
        log.warn("Failed to rollback after registration update error", rollbackException);
      }
      throw new SQLEventRegException(
          "Failed to update registration for participant "
              + eventRegistration.getParticipantId()
              + " on event "
              + eventRegistration.getEventId(),
          "EventRegistrationRepositoryJdbc.update",
          exception);
    } finally {
      try {
        connection.close();
      } catch (SQLException closeException) {
        log.warn("Failed to close connection after registration update", closeException);
      }
    }
  }

  @Override
  public EventRegistration findById(Integer id) {
    String sql =
        """
        SELECT * FROM EVENT_REGISTRATION WHERE id = ?
        """;
    try (Connection connection = ConnectionManager.get();
        var statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);

      log.debug("find registration by id={}: {}", id, sql);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return mapRegistration(rs);
        }
      }
      return null;
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to find registration by id " + id,
          "EventRegistrationRepositoryJdbc.findById",
          exception);
    }
  }

  @Override
  public Collection<EventRegistration> findAll() {
    Collection<EventRegistration> result = new ArrayList<>();
    String sql =
        """
        SELECT * FROM EVENT_REGISTRATION
        """;
    try (Connection connection = ConnectionManager.get();
        var statement = connection.prepareStatement(sql)) {
      log.debug("find all registrations: {}", sql);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          result.add(mapRegistration(rs));
        }
      }
      return result;
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to load all registrations", "EventRegistrationRepositoryJdbc.findAll", exception);
    }
  }

  @Override
  public List<EventRegistration> findByCreatedBetween(OffsetDateTime from, OffsetDateTime to) {
    List<EventRegistration> result = new ArrayList<>();
    StringBuilder sql = new StringBuilder("SELECT * FROM EVENT_REGISTRATION");
    if (from != null && to != null) {
      sql.append(" WHERE created_at BETWEEN ? AND ?");
    } else if (from != null) {
      sql.append(" WHERE created_at >= ?");
    } else if (to != null) {
      sql.append(" WHERE created_at <= ?");
    }
    try (Connection connection = ConnectionManager.get();
        var statement = connection.prepareStatement(sql.toString())) {
      if (from != null && to != null) {
        statement.setObject(1, from);
        statement.setObject(2, to);
      } else if (from != null) {
        statement.setObject(1, from);
      } else if (to != null) {
        statement.setObject(1, to);
      }
      log.debug("find registrations created between {} and {}: {}", from, to, sql);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          result.add(mapRegistration(rs));
        }
      }
      return result;
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to find registrations created between " + from + " and " + to,
          "EventRegistrationRepositoryJdbc.findByCreatedBetween",
          exception);
    }
  }

  @Override
  public void delete(Integer id) {
    String sql =
        """
        DELETE FROM EVENT_REGISTRATION WHERE id = ?
        """;
    try (Connection connection = ConnectionManager.get();
        var statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      log.debug("delete registration id={}: {}", id, sql);
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to delete registration with id " + id,
          "EventRegistrationRepositoryJdbc.delete",
          exception);
    }
  }

  @Override
  public boolean existsById(Integer id) {
    String sql = "SELECT * FROM EVENT_REGISTRATION WHERE id = ?";
    try (Connection connection = ConnectionManager.get();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      log.debug("exists registration by id={}: {}", id, sql);
      try (ResultSet rs = statement.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to check registration existence by id " + id,
          "EventRegistrationRepositoryJdbc.existsById",
          exception);
    }
  }

  @Override
  public void addToWaitingQueue(EventRegistration eventRegistration) {
    String sql = "UPDATE EVENT_REGISTRATION SET event_reg_request_status = 'WAITING' WHERE id = ?";
    try (Connection connection = ConnectionManager.get();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, eventRegistration.getId());
      log.debug("add registration id={} to waiting queue: {}", eventRegistration.getId(), sql);
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to add registration " + eventRegistration.getId() + " to waiting queue",
          "EventRegistrationRepositoryJdbc.addToWaitingQueue",
          exception);
    }
  }

  @Override
  public EventRegistration pollWaitingQueue(Integer eventId) {
    String sql =
        """
            SELECT * FROM event_registration
            WHERE event_id = ? AND event_reg_request_status = 'WAITING'
            ORDER BY id
            LIMIT 1
            """;
    try (Connection connection = ConnectionManager.get()) {
      connection.setAutoCommit(false);

      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setInt(1, eventId);
        log.debug("poll waiting queue for event id={}: {}", eventId, sql);
        try (ResultSet rs = statement.executeQuery()) {
          if (rs.next()) {
            EventRegistration eventRegistration = mapRegistration(rs);

            removeFromWaitingQueue(eventRegistration.getId(), connection);

            connection.commit();
            log.debug("polled registration id={} from waiting queue", eventRegistration.getId());
            return eventRegistration;
          } else {
            connection.rollback();
            return null;
          }
        }
      }
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to poll waiting queue for event " + eventId,
          "EventRegistrationRepositoryJdbc.pollWaitingQueue",
          exception);
    }
  }

  @Override
  public void removeFromWaitingQueue(Integer registrationId) {
    String sql = "UPDATE EVENT_REGISTRATION SET event_reg_request_status = 'PENDING' WHERE id = ?";
    try (Connection connection = ConnectionManager.get();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, registrationId);
      log.debug("remove registration id={} from waiting queue: {}", registrationId, sql);
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to remove registration " + registrationId + " from waiting queue",
          "EventRegistrationRepositoryJdbc.removeFromWaitingQueue",
          exception);
    }
  }

  private void removeFromWaitingQueue(Integer registrationId, Connection connection)
      throws SQLException {
    String sql = "UPDATE EVENT_REGISTRATION SET event_reg_request_status = 'PENDING' WHERE id = ?";
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, registrationId);
      log.debug("remove registration id={} from waiting queue (same tx): {}", registrationId, sql);
      statement.executeUpdate();
    }
  }

  @Override
  public Collection<EventRegistration> findAllInWaitingQueue() {
    Collection<EventRegistration> result = new ArrayList<>();
    String sql =
        """
        SELECT * FROM EVENT_REGISTRATION WHERE event_reg_request_status = 'WAITING'
        """;
    try (Connection connection = ConnectionManager.get();
        var statement = connection.prepareStatement(sql)) {
      log.debug("find all registrations in waiting queue: {}", sql);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          result.add(mapRegistration(rs));
        }
      }
      return result;
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to load registrations from waiting queue",
          "EventRegistrationRepositoryJdbc.findAllInWaitingQueue",
          exception);
    }
  }

  private EventRegistration mapRegistration(ResultSet rs) throws SQLException {
    return EventRegistration.builder()
        .id(rs.getInt("id"))
        .participantId(rs.getInt("participant_id"))
        .eventId(rs.getInt("event_id"))
        .eventRegRequestStatus(
            EventRegRequestStatus.fromString(rs.getString("event_reg_request_status")))
        .description(rs.getString("description"))
        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
        .build();
  }
}
