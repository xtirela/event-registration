package repository.implementation.Jdbc;

import exception.SQLEventRegException;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import model.Participant;
import model.enums.ParticipantGender;
import repository.ParticipantRepository;
import util.ConnectionManager;

@Slf4j
public class ParticipantRepositoryJdbc implements ParticipantRepository {

  @Override
  public Participant save(Participant participant) {
    String sql =
        """
        INSERT INTO PARTICIPANT(first_name, last_name, email, age, participant_gender, registered_at)
        values(?,?,?,?,?,?)
        """;
    Connection connection = ConnectionManager.get();
    try {
      connection.setAutoCommit(false);

      try (var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        statement.setString(1, participant.getFirstName());
        statement.setString(2, participant.getLastName());
        statement.setString(3, participant.getEmail());
        statement.setInt(4, participant.getAge());
        statement.setString(5, participant.getParticipantGender().name());
        statement.setObject(6, participant.getRegisteredAt());

        log.debug("save participant: {}", sql);
        statement.executeUpdate();

        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
          if (generatedKeys.next()) {
            participant.setId(generatedKeys.getInt(1));
          }
        }

        connection.commit();
        log.debug("participant saved, id={}", participant.getId());
        return participant;
      }
    } catch (SQLException exception) {
      try {
        connection.rollback();
      } catch (SQLException rollbackException) {
        log.warn("Failed to rollback after participant save error", rollbackException);
      }
      throw new SQLEventRegException(
          "Failed to save participant with email " + participant.getEmail(),
          "ParticipantRepositoryJdbc.save",
          exception);
    } finally {
      try {
        connection.close();
      } catch (SQLException closeException) {
        log.warn("Failed to close connection after participant save", closeException);
      }
    }
  }

  @Override
  public Participant update(Participant participant) {
    String sql =
        """
        UPDATE PARTICIPANT
        SET first_name = ?,
            last_name = ?,
            email = ?,
            age = ?,
            participant_gender = ?
        WHERE id = ?
        """;
    Connection connection = ConnectionManager.get();
    try {
      connection.setAutoCommit(false);

      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, participant.getFirstName());
        statement.setString(2, participant.getLastName());
        statement.setString(3, participant.getEmail());
        statement.setInt(4, participant.getAge());
        statement.setString(5, participant.getParticipantGender().name());
        statement.setInt(6, participant.getId());

        log.debug("update participant: {}", sql);
        statement.executeUpdate();
      }

      connection.commit();
      log.debug("participant updated, id={}", participant.getId());
      return participant;
    } catch (SQLException exception) {
      try {
        connection.rollback();
      } catch (SQLException rollbackException) {
        log.warn("Failed to rollback after participant update error", rollbackException);
      }
      throw new SQLEventRegException(
          "Failed to update participant " + participant.getId(),
          "ParticipantRepositoryJdbc.update",
          exception);
    } finally {
      try {
        connection.close();
      } catch (SQLException closeException) {
        log.warn("Failed to close connection after participant update", closeException);
      }
    }
  }

  @Override
  public Participant findById(Integer id) {
    String sql =
        """
        SELECT * FROM PARTICIPANT WHERE id = ?
        """;
    try (Connection connection = ConnectionManager.get();
        var statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);

      log.debug("find participant by id={}: {}", id, sql);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return mapParticipant(rs);
        }
        return null;
      }
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to find participant by id " + id,
          "ParticipantRepositoryJdbc.findById",
          exception);
    }
  }

  @Override
  public Collection<Participant> findAll() {
    Collection<Participant> response = new ArrayList<>();
    String sql =
        """
        SELECT * FROM PARTICIPANT
        """;
    try (Connection connection = ConnectionManager.get();
        var statement = connection.prepareStatement(sql)) {
      log.debug("find all participants: {}", sql);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          response.add(mapParticipant(rs));
        }
      }
      return response;
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to load all participants", "ParticipantRepositoryJdbc.findAll", exception);
    }
  }

  @Override
  public List<Participant> searchByFragment(String fragment) {
    List<Participant> result = new ArrayList<>();
    String sql =
        """
                SELECT * FROM PARTICIPANT WHERE first_name ILIKE ?
                """;
    try (Connection connection = ConnectionManager.get();
        var statement = connection.prepareStatement(sql)) {
      String fragmentInsert = "%" + fragment + "%";
      statement.setString(1, fragmentInsert);

      log.debug("search by fragment={}: {}", fragment, sql);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          result.add(mapParticipant(rs));
        }
        return result;
      }
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to search by fragment " + fragment,
          "ParticipantRepositoryJdbc.searchByFragment",
          exception);
    }
  }

  @Override
  public void delete(Integer id) {
    String sql = "DELETE FROM participant WHERE id = ?";
    try (Connection connection = ConnectionManager.get();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      log.debug("delete participant id={}: {}", id, sql);
      statement.executeUpdate();
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to delete participant with id " + id,
          "ParticipantRepositoryJdbc.delete",
          exception);
    }
  }

  @Override
  public boolean existsById(Integer id) {
    String sql = "SELECT * FROM PARTICIPANT WHERE id = ?";
    try (Connection connection = ConnectionManager.get();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setInt(1, id);
      log.debug("exists participant by id={}: {}", id, sql);
      try (ResultSet rs = statement.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to check participant existence by id " + id,
          "ParticipantRepositoryJdbc.existsById",
          exception);
    }
  }

  @Override
  public boolean existsByEmail(String email) {
    String sql = "SELECT * FROM PARTICIPANT WHERE email = ?";
    try (Connection connection = ConnectionManager.get();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, email);
      log.debug("exists participant by email={}: {}", email, sql);
      try (ResultSet rs = statement.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException exception) {
      throw new SQLEventRegException(
          "Failed to check participant existence by email " + email,
          "ParticipantRepositoryJdbc.existsByEmail",
          exception);
    }
  }

  private Participant mapParticipant(ResultSet rs) throws SQLException {
    return Participant.builder()
        .id(rs.getInt("id"))
        .firstName(rs.getString("first_name"))
        .lastName(rs.getString("last_name"))
        .participantGender(ParticipantGender.fromString(rs.getString("participant_gender")))
        .age(rs.getInt("age"))
        .email(rs.getString("email"))
        .registeredAt(rs.getObject("registered_at", OffsetDateTime.class))
        .build();
  }
}
