package com.eventreg.participantservice;

import org.springframework.boot.SpringApplication;

public class TestParticipantServiceApplication {

  public static void main(String[] args) {
    SpringApplication.from(ParticipantServiceApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
