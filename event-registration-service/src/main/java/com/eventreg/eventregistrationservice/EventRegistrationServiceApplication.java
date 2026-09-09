package com.eventreg.eventregistrationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Spring Boot application entry point for the event registration service. */
@SpringBootApplication
@EnableFeignClients
public class EventRegistrationServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(EventRegistrationServiceApplication.class, args);
  }
}
