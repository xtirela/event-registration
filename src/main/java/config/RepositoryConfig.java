package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Configuration for CSV persistence paths, loaded from application.properties. */
public class RepositoryConfig {

  private final String eventCsvPath;
  private final String participantCsvPath;
  private final String registrationCsvPath;

  public RepositoryConfig(
      String eventCsvPath, String participantCsvPath, String registrationCsvPath) {
    this.eventCsvPath = eventCsvPath;
    this.participantCsvPath = participantCsvPath;
    this.registrationCsvPath = registrationCsvPath;
  }

  public static RepositoryConfig load() {
    Properties properties = new Properties();
    try (InputStream input =
        RepositoryConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
      if (input != null) {
        properties.load(input);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load application.properties", e);
    }
    return new RepositoryConfig(
        properties.getProperty("events.data.file"),
        properties.getProperty("participants.data.file"),
        properties.getProperty("registrations.data.file"));
  }

  public String getEventCsvPath() {
    return eventCsvPath;
  }

  public String getParticipantCsvPath() {
    return participantCsvPath;
  }

  public String getRegistrationCsvPath() {
    return registrationCsvPath;
  }
}
