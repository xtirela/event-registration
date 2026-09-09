package com.eventreg.participantservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ParticipantServiceApplicationTests {

  @Test
  void contextLoads() {}
}
