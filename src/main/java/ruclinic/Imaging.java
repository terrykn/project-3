package ruclinic;
import util.Date;
/**
 * Manages imaging appointments
 * @author Terry Nguyen, Olivia Schroeder
 */
public class Imaging extends Appointment {
    private Radiology room;
    /**
     * Constructor for an Imaging object with parameters for the super class and its own instance var
     * @param apptDate the date of the imaging appointment
     * @param timeslotNum the timeslot number of the imaging appointment
     * @param patient the patient for the imaging appointment
     * @param provider the provider for the imaging appointment
     * @param room the room for the imaging appointment
     */
    public Imaging(Date apptDate, int timeslotNum, Person patient, Person provider, Radiology room) {
        super(apptDate, timeslotNum, patient, provider);
        this.room = room;
    }
    /**
     * Get the room for the imaging appointment
     * @return the room for the imaging appointment
     */
    public Radiology getRoom() {return this.room;}
    /**
     * Set the room for the imaging appointment
     * @param room the room for the imaging appointment
     */
    public void setRoom(Radiology room) {this.room = room;}
    /**
     * Override the toString method to include the room
     * @return the string representation of the imaging appointment
     */
    @Override
    public String toString() {
        return super.toString() + "[" + room.toString() + "]";
    }

}