package com.eventreg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy
public class PartyApplication {
  private static final Logger log = LoggerFactory.getLogger(PartyApplication.class);

  public static void main(String[] args) {
    SpringApplication.run(PartyApplication.class, args);
  }
}
