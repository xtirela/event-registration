package com.eventreg.dto.response;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;
import com.eventreg.model.enums.ParticipantGender;

@Data
@Builder
public class ParticipantResponse {
  private int participantId;
  private String firstName;
  private String lastName;
  private String email;
  private int age;
  private ParticipantGender participantGender;
  private OffsetDateTime registeredAt;
}
