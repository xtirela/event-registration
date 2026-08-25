package com.eventreg.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.eventreg.model.enums.ParticipantGender;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantCreateRequest {
  private String firstName;
  private String lastName;
  private String email;
  private int age;
  private ParticipantGender participantGender;
}
