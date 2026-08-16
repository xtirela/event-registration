package view;

import dto.*;
import exception.EventRegException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Predicate;
import model.Event;
import model.Participant;
import model.enums.EventGenderRequirement;
import model.enums.EventRegRequestStatus;
import model.enums.EventRegistrationStatus;
import model.enums.ParticipantGender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.EventService;

public class ConsoleView {
  private final EventService eventService;

  private static final Logger log = LoggerFactory.getLogger(ConsoleView.class);

  public ConsoleView(EventService service) {
    eventService = service;
  }

  public void printMenu() {
    log.info("=== Меню ===");
    log.info("1. Создать участника");
    log.info("2. Добавить мероприятие");
    log.info("3. Зарегистрировать участника на мероприятие");
    log.info("4. Отменить регистрацию");
    log.info("5. Показать детали мероприятия");
    log.info("6. Показать все мероприятия");
    log.info("7. Показать все мероприятия c фильтрацией");
    log.info("8. Показать все мероприятия c группировкой");
    log.info("9. Показать участника");
    log.info("10. Показать всех участников");
    log.info("11. Показать всех участников с сортировкой");
    log.info("12. Показать заявку регистрации");
    log.info("13. Показать все заявки регистрации");
    log.info("14. Отменить последнее действие");
    log.info("15. Очистить консоль");
    log.info("16. Выход");
  }

  private static void clearConsole() {
    for (int i = 0; i < 50; i++) {
      log.info("\n");
    }
  }

  public void performAction(int action, Scanner scanner) {
    try {
      switch (action) {
        case 1:
          log.info(
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
          log.info("{}", eventService.createParticipant(participantCreateRequest));
          break;
        case 2:
          log.info(
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

          log.info("{}", eventService.createEvent(eventCreateRequest));

          break;
        case 3:
          log.info(
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
            log.info(
                "Все места на событие забронированы, перевести участника в очередь ожидания? Y/N");
            String putInQueueInput = scanner.nextLine();
            if (putInQueueInput.equals("Y")) {
              eventService.changeRegistrationRequestStatus(
                  eventRegResponse.getRegistrationId(),
                  EventRegRequestStatus.WAITING,
                  "put in waiting queue for free spots",
                  true);
              log.info("Участник успешно положен в очередь ожидания");
            } else if (!putInQueueInput.equals("N")) {
              log.warn(
                  "Введено неправильное значение, по умолчанию участник не будет переведён в очередь ожидания");
            }
          }
          log.info("{}", eventRegResponse);

          break;
        case 4:
          log.info("Введите id регистрации для отмены: ");
          int regId = scanner.nextInt();
          scanner.nextLine();
          log.info(
              "{}",
              eventService.changeRegistrationRequestStatus(
                  regId, EventRegRequestStatus.CANCELLED, "cancelled reg request", true));
          break;
        case 5:
          log.info("Введите id мероприятия для поиска: ");
          int eventDetailId = scanner.nextInt();
          scanner.nextLine();
          log.info("{}", eventService.getEventById(eventDetailId));
          break;
        case 6:
          log.info("{}", "Список мероприятий: ");
          eventService.getEvents().forEach(event -> log.info("{}", event));
          log.info("{}", "\n");
          break;
        case 7:
          log.info(
              "{}",
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

          log.info("{}", "Список мероприятий (с фильтрацией): ");
          eventService.getEventsFiltered(predicates).forEach(event -> log.info("{}", event));
          break;
        case 8:
          log.info("{}", "Список мероприятий (с группировкой по месту): ");
          Map<String, List<EventResponse>> groupedEvents =
              eventService.getEventsGrouped(Event::getLocation);

          groupedEvents.forEach((location, events) -> log.info("{}", location + ": " + events));
          break;
        case 9:
          log.info("Введите id участника: ");
          int participantDetailId = scanner.nextInt();
          scanner.nextLine();
          log.info("{}", eventService.getParticipantById(participantDetailId));
          break;
        case 10:
          log.info("{}", "Список участников:");
          eventService.getParticipants().forEach(participant -> log.info("{}", participant));
          break;
        case 11:
          log.info("{}", "Поле сортировки (AGE/NAME/GENDER/REGISTERED_AT): ");

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
          log.info("{}", "Список участников (с сортировкой): ");
          eventService.getParticipantsSorted(comparator).forEach(participant -> log.info("{}", participant));
          break;
        case 12:
          log.info("Введите id заявки: ");
          int requestId = scanner.nextInt();
          scanner.nextLine();
          log.info("{}", eventService.getRegistrationRequestById(requestId));
          break;
        case 13:
          log.info("{}", "Все заявки регистрации:");
          eventService.getRegistrationRequests().forEach(request -> log.info("{}", request));
          break;
        case 14:
          log.info("{}", "Подвердите последнее действие для отмены: Y/N");
          log.info("{}", eventService.getLatestAction());
          String undoActionInput = scanner.nextLine();
          if (undoActionInput.equals("Y")) {
            log.info("{}", eventService.undoLatestAction());
          } else if (!undoActionInput.equals("N")) {
            log.warn("{}", "Введено неправильное значение, по умолчанию действие не отменено");
          }

          break;
        case 15:
          clearConsole();
          break;
        default:
          log.warn("{}", "Введено некорректное значение!");
          break;
      }
    } catch (EventRegException eventRegException) {
      log.error(
          "{}",
          "ОШИБКА: "
              + eventRegException.getMessage()
              + " операция: "
              + eventRegException.getOperation()
              + " тип: "
              + eventRegException.getClass(),
          eventRegException);
    } catch (NumberFormatException numberFormatException) {
      log.error(
          "{}",
          "ОШИБКА: " + "введены неправильные значения чисел " + numberFormatException.getCause(),
          numberFormatException);
    } catch (DateTimeParseException dateTimeParseException) {
      log.error(
          "{}",
          "ОШИБКА: " + "введены неправильные значения даты " + dateTimeParseException.getCause(),
          dateTimeParseException);
    }
  }
}
