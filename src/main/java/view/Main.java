package view;

import java.util.Scanner;
import service.EventService;
import service.implementation.EventServiceImpl;

public class Main {
  public static void main(String[] args) {
    EventService eventService = new EventServiceImpl();
    ConsoleView consoleView = new ConsoleView(eventService);

    Scanner scanner = new Scanner(System.in);

    while (true) {
      consoleView.printMenu();
      int input = scanner.nextInt();
      scanner.nextLine();
      if (input == 14) break;

      consoleView.performAction(input, scanner);
    }
    System.out.println("Завершение работы");
  }
}
