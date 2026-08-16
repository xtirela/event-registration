package view;

import config.RepositoryConfig;
import java.util.InputMismatchException;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.EventService;
import service.implementation.EventServiceImpl;

public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {

    EventService eventService = new EventServiceImpl(RepositoryConfig.load());
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
  }
}
