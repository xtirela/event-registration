package model;

import lombok.Builder;
import lombok.Data;
import model.enums.ParticipantGender;

import java.time.OffsetDateTime;

@Data
@Builder
public class Participant {
    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private int age;
    private ParticipantGender participantGender;
    private OffsetDateTime createdAt;
}
//TODO: добавить в сервисе корректность email и других данных при созданни
//TODO: Добавить ограничение возраста: больше 0 меньше 150