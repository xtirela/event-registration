package model.enums;

public enum ParticipantGender {
    MALE, FEMALE, NOT_SPECIFIED;
    public static ParticipantGender fromString(String input)
    {
        if (input == null) return null;
        try{
            return ParticipantGender.valueOf(input.toUpperCase());
        }
        catch(IllegalArgumentException ex){
            return null;
        }
    }
}
