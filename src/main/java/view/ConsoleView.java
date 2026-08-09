package view;

import dto.CancelRegRequest;
import dto.EventCreateRequest;
import dto.EventRegRequest;
import dto.ParticipantCreateRequest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Scanner;
import model.enums.EventGenderRequirement;
import model.enums.ParticipantGender;
import service.EventService;

public class ConsoleView {
  private EventService eventService;

  public ConsoleView(EventService service) {
    eventService = service;
  }

  public void printMenu() {
    System.out.println("=== Меню ===");
    System.out.println("1. Создать участника");
    System.out.println("2. Добавить мероприятие");
    System.out.println("3. Зарегистрировать участника на мероприятие");
    System.out.println("4. Отменить регистрацию");
    System.out.println("5. Показать детали мероприятия");
    System.out.println("6. Показать все мероприятия");
    System.out.println("7. Показать участника");
    System.out.println("8. Показать всех участников");
    System.out.println("9. Показать заявку регистрации");
    System.out.println("10. Показать все заявки регистрации");
    System.out.println("11. Показать события на которые зарегистрирован участник");
    System.out.println("12. Показать регистрации всех участников на события");
    System.out.println("13. Очистить консоль");
    System.out.println("14. Выход");
  }

  private static void clearConsole() {
    for (int i = 0; i < 50; i++) {
      System.out.println();
    }
  }

  public void performAction(int action, Scanner scanner) {
    switch (action) {
      case 1:
        System.out.println(
            "Введите данные участника через запятую без пробелов \nимя,фамилия,email,возраст,пол(MALE/FEMALE/NOT_SPECIFIED): ");
        String[] input = scanner.nextLine().split(",");

        if (input.length != 5) {
          System.out.println("incorrect amount of elements");
          break;
        }

        ParticipantCreateRequest participantCreateRequest =
            ParticipantCreateRequest.builder()
                .firstName(input[0])
                .lastName(input[1])
                .email(input[2])
                .age(Integer.parseInt(input[3]))
                .participantGender(ParticipantGender.fromString(input[4]))
                .build();
        System.out.println(eventService.createParticipant(participantCreateRequest).toString());
        break;
      case 2:
        System.out.println(
            "Введите данные мероприятия через запятую без пробелов \nназвание,дата(2026-12-31T20:00:00+03:00),продолжительность_часов,мин_возраст,макс_участников,гендер(NONE/MALE_ONLY/FEMALE_ONLY): ");
        String[] eventInput = scanner.nextLine().split(",");
        if (eventInput.length != 6) {
          System.out.println("incorrect amount of elements");
          break;
        }
        try {
          EventCreateRequest eventCreateRequest =
              EventCreateRequest.builder()
                  .eventName(eventInput[0])
                  .eventDate(OffsetDateTime.parse(eventInput[1]))
                  .eventDuration(Duration.ofHours(Long.parseLong(eventInput[2])))
                  .ageRequired(Integer.parseInt(eventInput[3]))
                  .maxParticipantAmount(Integer.parseInt(eventInput[4]))
                  .genderRequirement(EventGenderRequirement.fromString(eventInput[5]))
                  .build();
          System.out.println(eventService.createEvent(eventCreateRequest));
        } catch (Exception e) {
          System.out.println("incorrect element datatypes");
        }
        break;
      case 3:
        System.out.println(
            "Введите данные через запятую без пробелов \nid участника для регистрации, id события на которое его зарегистрировать: ");
        String[] participantRegInput = scanner.nextLine().split(",");
        if (participantRegInput.length != 2) {
          System.out.println("incorrect amount of elements");
          break;
        }
        int participantId = Integer.parseInt(participantRegInput[0]);
        int eventId = Integer.parseInt(participantRegInput[1]);
        EventRegRequest eventRegRequest =
            EventRegRequest.builder().participantId(participantId).eventId(eventId).build();
        System.out.println(eventService.registerParticipant(eventRegRequest).toString());

        break;
      case 4:
        System.out.print("Введите id регистрации для отмены: ");
        int regId = scanner.nextInt();
        scanner.nextLine();
        CancelRegRequest cancelRegRequest =
            CancelRegRequest.builder().eventRegistrationId(regId).build();
        System.out.println(eventService.cancelRegistration(cancelRegRequest));
        break;
      case 5:
        System.out.print("Введите id мероприятия для поиска: ");
        int eventDetailId = scanner.nextInt();
        scanner.nextLine();
        System.out.println(eventService.getEventById(eventDetailId));
        break;
      case 6:
        System.out.println("Список мероприятий: ");
        eventService.getEvents().forEach(System.out::println);
        System.out.println("\n");
        System.out.println(eventService.getEvents());
        break;
      case 7:
        System.out.print("Введите id участника: ");
        int participantDetailId = scanner.nextInt();
        scanner.nextLine();
        System.out.println(eventService.getParticipantById(participantDetailId));
        break;
      case 8:
        System.out.println("Список участников:");
        eventService.getParticipants().forEach(System.out::println);
        break;
      case 9:
        System.out.print("Введите id заявки: ");
        int requestId = scanner.nextInt();
        scanner.nextLine();
        System.out.println(eventService.getRegistrationRequestById(requestId));
        break;
      case 10:
        System.out.println("Все заявки регистрации:");
        eventService.getRegistrationRequests().forEach(System.out::println);
        break;
      case 11:
        System.out.print("Введите id участника: ");
        int pId = scanner.nextInt();
        scanner.nextLine();
        System.out.println("События участника:");
        eventService.getRegisteredParticipant(pId).forEach(System.out::println);
        break;
      case 12:
        System.out.println("Регистрации всех участников:");
        eventService.getAllRegisteredParticipants().forEach(System.out::println);
        break;
      case 13:
        clearConsole();
        break;
      default:
        System.out.println("Введено некорректное значение!");
        break;
    }
  }
}
