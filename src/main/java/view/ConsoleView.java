package view;

import dto.*;
import exception.EventRegException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Predicate;
import model.Event;
import model.Participant;
import model.enums.EventGenderRequirement;
import model.enums.EventRegRequestStatus;
import model.enums.EventRegistrationStatus;
import model.enums.ParticipantGender;
import service.EventService;

public class ConsoleView {
  private final EventService eventService;

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
    System.out.println("7. Показать все мероприятия c фильтрацией");
    System.out.println("8. Показать все мероприятия c группировкой");
    System.out.println("9. Показать участника");
    System.out.println("10. Показать всех участников");
    System.out.println("11. Показать всех участников с сортировкой");
    System.out.println("12. Показать заявку регистрации");
    System.out.println("13. Показать все заявки регистрации");
    System.out.println("14. Отменить последнее действие");
    System.out.println("15. Очистить консоль");
    System.out.println("16. Выход");
  }

  private static void clearConsole() {
    for (int i = 0; i < 50; i++) {
      System.out.println();
    }
  }

  public void performAction(int action, Scanner scanner) {
    try {
      switch (action) {
        case 1:
          System.out.println(
              "Введите данные участника через запятую без пробелов \nимя,фамилия,email,возраст,пол(MALE/FEMALE/NOT_SPECIFIED): ");
          String[] input = scanner.nextLine().split(",");

          if (input.length != 5) {
            throw new exception.IllegalArgumentEventRegException(
                "incorrect amount of elements", "performAction");
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
              "Введите данные мероприятия через запятую без пробелов \nназвание,место,дата(2026-12-31T20:00:00+03:00),продолжительность_часов,мин_возраст,макс_участников,гендер(NONE/MALE_ONLY/FEMALE_ONLY): ");
          String[] eventInput = scanner.nextLine().split(",");
          if (eventInput.length != 7) {
            throw new exception.IllegalArgumentEventRegException(
                "incorrect amount of elements", "performAction");
          }
          EventCreateRequest eventCreateRequest =
              EventCreateRequest.builder()
                  .eventName(eventInput[0])
                  .location(eventInput[1])
                  .eventDate(OffsetDateTime.parse(eventInput[2]))
                  .eventDuration(Duration.ofHours(Long.parseLong(eventInput[3])))
                  .ageRequired(Integer.parseInt(eventInput[4]))
                  .maxParticipantAmount(Integer.parseInt(eventInput[5]))
                  .genderRequirement(EventGenderRequirement.fromString(eventInput[6]))
                  .build();

          System.out.println(eventService.createEvent(eventCreateRequest));

          break;
        case 3:
          System.out.println(
              "Введите данные через запятую без пробелов \nid участника для регистрации, id события на которое его зарегистрировать: ");
          String[] participantRegInput = scanner.nextLine().split(",");
          if (participantRegInput.length != 2) {
            throw new exception.IllegalArgumentEventRegException(
                "incorrect amount of elements", "performAction");
          }
          int participantId = Integer.parseInt(participantRegInput[0]);
          int eventId = Integer.parseInt(participantRegInput[1]);
          EventRegRequest eventRegRequest =
              EventRegRequest.builder().participantId(participantId).eventId(eventId).build();

          EventRegResponse eventRegResponse = eventService.registerParticipant(eventRegRequest);

          if (eventService
              .getEventById(eventId)
              .getEventRegistrationStatus()
              .equals(EventRegistrationStatus.ALL_RESERVED)) {
            System.out.println(
                "Все места на событие забронированы, перевести участника в очередь ожидания? Y/N");
            String putInQueueInput = scanner.nextLine();
            if (putInQueueInput.equals("Y")) {
              eventService.changeRegistrationRequestStatus(
                  eventRegResponse.getRegistrationId(),
                  EventRegRequestStatus.WAITING,
                  "put in waiting queue for free spots",
                  true);
              System.out.println("Участник успешно положен в очередь ожидания");
            } else if (!putInQueueInput.equals("N")) {
              System.out.println(
                  "Введено неправильное значение, по умолчанию участник не будет переведён в очередь ожидания");
            }
          }
          System.out.println(eventRegResponse);

          break;
        case 4:
          System.out.print("Введите id регистрации для отмены: ");
          int regId = scanner.nextInt();
          scanner.nextLine();
          System.out.println(
              eventService.changeRegistrationRequestStatus(
                  regId, EventRegRequestStatus.CANCELLED, "cancelled reg request", true));
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
          break;
        case 7:
          System.out.println(
              "Введите фильтр через запятую (пустые поля = без фильтра): \nдата(2026-12-31T20:00:00+03:00),место,мин_возраст: ");
          String[] filterInput = scanner.nextLine().split(",");

          if (filterInput.length != 3) {
            throw new exception.IllegalArgumentEventRegException(
                "incorrect amount of elements", "performAction");
          }

          OffsetDateTime filterDate =
              filterInput[0].isBlank() ? null : OffsetDateTime.parse(filterInput[0]);
          String filterLocation = filterInput[1];

          Integer filterAgeRequired =
              filterInput[2].isBlank() ? null : Integer.parseInt(filterInput[2]);

          Predicate<Event> dateFilterPredicate =
              event -> (filterDate == null || event.getEventDate().equals(filterDate));

          Predicate<Event> locationFilterPredicate =
              event ->
                  (filterLocation == null
                      || filterLocation.isEmpty()
                      || event.getLocation().equals(filterLocation));

          Predicate<Event> ageFilterPredicate =
              event -> (filterAgeRequired == null || event.getAgeRequired() == filterAgeRequired);

          List<Predicate<Event>> predicates =
              List.of(dateFilterPredicate, locationFilterPredicate, ageFilterPredicate);

          System.out.println("Список мероприятий (с фильтрацией): ");
          eventService.getEventsFiltered(predicates).forEach(System.out::println);
          break;
        case 8:
          System.out.println("Список мероприятий (с группировкой по месту): ");
          Map<String, List<EventResponse>> groupedEvents =
              eventService.getEventsGrouped(Event::getLocation);

          groupedEvents.forEach((location, events) -> System.out.println(location + ": " + events));
          break;
        case 9:
          System.out.print("Введите id участника: ");
          int participantDetailId = scanner.nextInt();
          scanner.nextLine();
          System.out.println(eventService.getParticipantById(participantDetailId));
          break;
        case 10:
          System.out.println("Список участников:");
          eventService.getParticipants().forEach(System.out::println);
          break;
        case 11:
          System.out.println("Поле сортировки (AGE/NAME/GENDER/REGISTERED_AT): ");

          String sortField = scanner.nextLine().toUpperCase();

          Comparator<Participant> comparator =
              switch (sortField) {
                case "AGE" -> Comparator.comparingInt(Participant::getAge);
                case "NAME" -> Comparator.comparing(Participant::getFirstName);
                case "GENDER" -> Comparator.comparing(Participant::getParticipantGender);
                case "REGISTERED_AT" -> Comparator.comparing(Participant::getRegisteredAt);
                default ->
                    throw new exception.IllegalArgumentEventRegException(
                        "Некорректное поле сортировки", "performAction");
              };
          System.out.println("Список участников (с сортировкой): ");
          eventService.getParticipantsSorted(comparator).forEach(System.out::println);
          break;
        case 12:
          System.out.print("Введите id заявки: ");
          int requestId = scanner.nextInt();
          scanner.nextLine();
          System.out.println(eventService.getRegistrationRequestById(requestId));
          break;
        case 13:
          System.out.println("Все заявки регистрации:");
          eventService.getRegistrationRequests().forEach(System.out::println);
          break;
        case 14:
          System.out.println(eventService.undoLatestAction());
          break;
        case 15:
          clearConsole();
          break;
        default:
          System.out.println("Введено некорректное значение!");
          break;
      }
    } catch (EventRegException eventRegException) {
      System.out.println(
          "ОШИБКА: "
              + eventRegException.getMessage()
              + " операция: "
              + eventRegException.getOperation()
              + " тип: "
              + eventRegException.getClass());
    } catch (IllegalArgumentException illegalArgumentException) {
      String operation =
          illegalArgumentException instanceof exception.IllegalArgumentEventRegException iae
              ? iae.getOperation()
              : null;
      System.out.println(
          "ОШИБКА: " + illegalArgumentException.getMessage() + " операция: " + operation);
    }
  }
}
