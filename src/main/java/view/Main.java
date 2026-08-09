package view;

import service.EventService;
import service.implementation.EventServiceImpl;

import java.util.Scanner;

public class Main {
    public static void main()
    {
        EventService eventService = new EventServiceImpl();
        ConsoleView consoleView = new ConsoleView(eventService);

        Scanner scanner = new Scanner(System.in);

        while(true)
        {
            consoleView.printMenu();
            int input = scanner.nextInt();
            scanner.nextLine();
            if (input == 14)
                break;

            consoleView.performAction(input, scanner);

        }
        System.out.println("Завершение работы");
    }
}
