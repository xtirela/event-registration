package com.eventreg.service.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
  private final JavaMailSender mailSender;

  public void sendEventConfirmation(String to, String eventName, String description) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(to);
    message.setSubject("Регистрация на событие: " + eventName);
    message.setText(description);
    mailSender.send(message);
  }
}
