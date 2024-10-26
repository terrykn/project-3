package ruclinic;
/**
 * Keeps track of different timeslots for appointments
 * @author Terry Nguyen, Olivia Schroeder
 */
public class Timeslot implements Comparable<Timeslot> {
    private int hour;
    private int minute;
    private final int NOON = 12;
    /**
     * Constructor for Timeslot, check externally with static method ifValidTimeslot for printing error
     * @param timeslotNumber the number of the timeslot
     */
    public Timeslot(int timeslotNumber) {
        // find the hour and minute from the timeslot number
        if (timeslotNumber < 1 || timeslotNumber > 12) {
            this.hour = 0;
            this.minute = 0;
            return;
        }
        int baseHour = 9 + (timeslotNumber - 1) / 2;
        int baseMinute = ((timeslotNumber - 1) % 2) * 30;
        // Adjust for the skipped times [12:00pm -> 1:30pm]
        if (baseHour >= 12) {
            baseHour += 2;
        }
        this.hour = baseHour;
        this.minute = baseMinute;
    }
    /**
     * another constructor given a string in format like 10:20 to convert it to a timeslot obj
     * @param timeslotString the string to convert to a timeslot object
     */
    public Timeslot(String timeslotString) {
        System.out.println("here");
        // Split the time and the period (AM/PM)
        String[] timeParts = timeslotString.split(" ");
        String time = timeParts[0]; // "10:20"
        String period = timeParts[1]; // "am" or "pm"
        // Split the hour and minutes
        String[] hourMinute = time.split(":");
        int hour = Integer.parseInt(hourMinute[0]); // Get the hour part
        int minutes = Integer.parseInt(hourMinute[1]); // Get the minute part
        // Convert to military time if needed
        if (period.equals("PM")) {
            hour += 12; // Convert PM hours to military time
        }
    }
    /**
     * static method, called Timeslot.isValidTimeslot, check if string (that's supposed to be a timeslot) is a valid timeslot
     * @param timeslotString the string to check
     * @return true if the string is a valid timeslot, false otherwise
     */
    public static boolean isValidTimeslot(String timeslotString) {
        try {
            int timeslot = Integer.parseInt(timeslotString);
            if (new Timeslot(timeslot).hour == 0 && new Timeslot(timeslot).minute == 0) {
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    /**
     * Given a Timeslot object, return the timeslot number (1-12)
     * @param timeslot the Timeslot object
     * @return the timeslot number (1-12)
     */
    public static int getTimeslotNumber(Timeslot timeslot) {
        int baseHour = timeslot.hour;
        int baseMinute = timeslot.minute;
        // Adjust for the skipped times [12:00pm -> 1:30pm]
        if (baseHour >= 14) {
            baseHour -= 2;
        }
        int timeslotNumber = ((baseHour - 9) * 2) + (baseMinute / 30) + 1;
        return timeslotNumber;
    }
    /**
     * Compare to another Timeslot object
     * @param o the other Timeslot object to compare to
     * @return -1 if this Timeslot is less than the other, 1 if greater, 0 if equal
     */
    @Override
    public int compareTo(Timeslot o) {
        if (this.hour < o.hour) {
            return -1;
        } else if (this.hour > o.hour) {
            return 1;
        } else {
            if (this.minute < o.minute) {
                return -1;
            } else if (this.minute > o.minute) {
                return 1;
            } else {
                return 0;
            }
        }
    }
    /**
     * see if two Timeslot objects are equal
     * @param o the other object to compare to
     * @return true if the two Timeslot objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Timeslot) {
            Timeslot other = (Timeslot) o;
            return this.hour == other.hour && this.minute == other.minute;
        }
        return false;
    }
    /**
     * return the string of the timeslot in the format hour:minute AM/PM
     * @return the string of the timeslot
     */
    @Override
    public String toString() {
        String morningOrNight = "AM";
        int hourToPrint = this.hour;
        String minutesToPrint = String.format("%02d", this.minute); // Format minutes to two digits
        if (this.hour >= NOON) {
            morningOrNight = "PM";
            hourToPrint = this.hour - NOON;
        }
        return hourToPrint + ":" + minutesToPrint + " " + morningOrNight;
    }
}

