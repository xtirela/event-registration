package dto;

import lombok.Builder;
import lombok.Data;
import model.enums.ParticipantGender;

@Data
@Builder
public class ParticipantResponse {
  private String firstName;
  private String lastName;
  private String email;
  private int age;
  private ParticipantGender participantGender;
}
