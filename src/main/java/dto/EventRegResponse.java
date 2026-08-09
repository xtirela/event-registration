package dto;

import lombok.Builder;
import lombok.Data;
import model.enums.EventRegRequestStatus;
import model.enums.ParticipantGender;

@Data
@Builder
public class EventRegResponse
{
    private String firstName;
    private String lastName;
    private String email;
    private int age;
    private ParticipantGender participantGender;

    private String eventName;

    private EventRegRequestStatus eventRegRequestStatus;
    @Builder.Default
    private String description = "none";
}
