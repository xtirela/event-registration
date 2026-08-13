package view;

import java.util.InputMismatchException;
import java.util.Scanner;
import service.EventService;
import service.implementation.EventServiceImpl;

public class Main {
  public static void main(String[] args) {

    EventService eventService = new EventServiceImpl();
    ConsoleView consoleView = new ConsoleView(eventService);

    Scanner scanner = new Scanner(System.in);

    int input;
    while (true) {
      consoleView.printMenu();
      try {
        input = scanner.nextInt();
      } catch (InputMismatchException e) {
        scanner.nextLine();
        System.out.println("Неверный ввод, введите номер пункта меню");
        continue;
      }
      scanner.nextLine();
      if (input == 16) break;

      consoleView.performAction(input, scanner);
    }
    System.out.println("Завершение работы");
  }
}
