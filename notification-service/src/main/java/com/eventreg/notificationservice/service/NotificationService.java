package com.eventreg.notificationservice.service;

import com.eventreg.notificationservice.dto.request.SendEventNotificationRequest;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service responsible for dispatching email notifications via Resend. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final Resend resend;

  /** Sends an email notification when a new event is created. */
  public void sendEventCreatedNotification(SendEventNotificationRequest request, String toEmail) {
    String date = request.getEventDate().format(DateTimeFormatter.ISO_DATE_TIME);

    CreateEmailOptions options =
        CreateEmailOptions.builder()
            .from("EventReg <onboarding@resend.dev>")
            .to(toEmail)
            .subject("Новое событие: " + request.getEventName())
            .text(
                "Событие \""
                    + request.getEventName()
                    + "\" создано.\nДата: "
                    + date
                    + "\nДлительность: "
                    + request.getEventDurationMinutes()
                    + " мин.\n\n"
                    + request.getEventDescription())
            .build();

    try {
      resend.emails().send(options);
      log.info("Notification sent for event {}", request.getEventName());
    } catch (Exception e) {
      log.error("Failed to send notification for event {}", request.getEventName(), e);
      throw new RuntimeException("Failed to send notification", e);
    }
  }
}
