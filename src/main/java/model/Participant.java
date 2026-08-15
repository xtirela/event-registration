package model;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Data;
import model.enums.ParticipantGender;

@Data
@Builder
public class Participant {
  private int id;
  private String firstName;
  private String lastName;
  private String email;
  private int age;
  private ParticipantGender participantGender;
  private OffsetDateTime registeredAt;
}
