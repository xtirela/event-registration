package com.eventreg.notificationservice.controller;

import com.eventreg.notificationservice.dto.request.SendEventNotificationRequest;
import com.eventreg.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Internal controller for sending notifications without public exposure. */
@Hidden
@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

  private final NotificationService notificationService;

  @PostMapping("/event-created")
  public ResponseEntity<Void> sendEventCreated(
      @RequestBody SendEventNotificationRequest request, @RequestParam String toEmail) {
    notificationService.sendEventCreatedNotification(request, toEmail);
    return ResponseEntity.ok().build();
  }
}
