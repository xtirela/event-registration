package com.eventreg.service.implementation;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale;
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion;
import net.fortuna.ical4j.util.RandomUidGenerator;
import org.springframework.stereotype.Service;

@Service
public class IcsService {
  public byte[] generateIcsFile(
      String eventName, String description, LocalDateTime start, LocalDateTime end) {
    try {
      Calendar calendar = new Calendar();
      calendar.add(new ImmutableCalScale("GREGORIAN"));
      calendar.add(new ImmutableVersion("2.0"));
      calendar.add(new ProdId("-//EventReg//RU"));

      VEvent event = new VEvent();

      Uid uid = new RandomUidGenerator().generateUid();
      event.add(uid);

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
      String startUtc =
          start
              .atZone(ZoneId.systemDefault())
              .withZoneSameInstant(ZoneId.of("UTC"))
              .format(formatter);
      String endUtc =
          end.atZone(ZoneId.systemDefault())
              .withZoneSameInstant(ZoneId.of("UTC"))
              .format(formatter);

      event.add(new DtStart<>(startUtc));
      event.add(new DtEnd<>(endUtc));

      event.add(new Summary(eventName));

      if (description != null && !description.isEmpty()) {
        event.add(new Description(description));
      }

      calendar.add(event);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      CalendarOutputter outputter = new CalendarOutputter();
      outputter.output(calendar, baos);

      return baos.toByteArray();

    } catch (Exception e) {
      throw new RuntimeException("Ошибка при создании ICS файла", e);
    }
  }
}
