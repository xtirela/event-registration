package model.enums;

public enum EventRegistrationStatus {
    ALL_RESERVED, RESERVATIONS_CLOSED, RESERVATIONS_OPEN;
    public static EventRegistrationStatus fromString(String input)
    {
        if (input == null) return null;
        try{
            return EventRegistrationStatus.valueOf(input.toUpperCase());
        }
        catch(IllegalArgumentException ex){
            return null;
        }
    }
}
