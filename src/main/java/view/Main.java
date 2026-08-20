package view;

import java.util.InputMismatchException;
import java.util.Scanner;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.EventRegistrationRepository;
import repository.EventRepository;
import repository.ParticipantRepository;
import repository.implementation.Jdbc.EventRegistrationRepositoryJdbc;
import repository.implementation.Jdbc.EventRepositoryJdbc;
import repository.implementation.Jdbc.ParticipantRepositoryJdbc;
import service.EventService;
import service.implementation.EventServiceImpl;
import util.ConnectionManager;

public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  @SneakyThrows
  public static void main(String[] args) {

    try (Database database =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(ConnectionManager.get()))) {

      Liquibase liquibase =
          new Liquibase(
              "db/changelog/db.changelog-master.yaml", new ClassLoaderResourceAccessor(), database);

      liquibase.update("");
    }

    //    EventService eventService = new EventServiceImpl(RepositoryConfig.load());

    ParticipantRepository participantRepo = new ParticipantRepositoryJdbc();
    EventRepository eventRepo = new EventRepositoryJdbc();
    EventRegistrationRepository regRepo = new EventRegistrationRepositoryJdbc();

    EventService eventService = new EventServiceImpl(eventRepo, participantRepo, regRepo);

    ConsoleView consoleView = new ConsoleView(eventService);

    Scanner scanner = new Scanner(System.in);

    int input;
    while (true) {
      consoleView.printMenu();
      try {
        input = scanner.nextInt();
      } catch (InputMismatchException e) {
        scanner.nextLine();
        log.warn("Неверный ввод, введите номер пункта меню");
        continue;
      }
      scanner.nextLine();
      if (input == 16) break;

      consoleView.performAction(input, scanner);
    }
    log.info("Завершение работы");
    ConnectionManager.closeAll();
  }
}
