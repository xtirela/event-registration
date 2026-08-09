package model.enums;


public enum EventStatus
{
    ONGOING, CANCELLED, ENDED, PLANNED;
    public static EventStatus fromString(String input)
    {
        if (input == null) return null;
        try{
            return EventStatus.valueOf(input.toUpperCase());
        }
        catch(IllegalArgumentException ex){
            return null;
        }
    }
}
