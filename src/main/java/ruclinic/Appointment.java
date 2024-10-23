package ruclinic;
import util.Date;
/**
 * Represents appointment object defining details for an appointment
 * @author Terry Nguyen, Olivia Schroeder
 */
public class Appointment implements Comparable <Appointment> {
    protected Date date;
    protected Timeslot timeslot;
    protected Person patient;
    protected Person provider;
    /**
     * default constructor for an Appointment object
     */
    public Appointment() {
        this.date = new Date();
        this.timeslot = null;
        this.patient = null;
        this.provider = null;
    }
    /**
     * Constructor for an Appointment object with parameters for the appointment date, timeslot, and profile.
     * @param dateString Date object containing date of the new Appointment object
     * @param timeslotString Timeslot object containing the time of the new Appointment object
     * @param patient person profile of the patient associated with the Appointment object
     */
    public Appointment(String dateString, String timeslotString, Person patient) {
        this.date = new Date(dateString);
        this.timeslot = new Timeslot(Integer.parseInt(timeslotString));
        this.patient = patient;
        this.provider = null;
    }
    /**
     * Constructor for an Appointment object with parameters for the appointment date, timeslot, and profile.
     * @param apptDate Date object containing date of the new Appointment object
     * @param timeslotNum Timeslot number for the time of the new Appointment object
     * @param patient person object containing the profile of the patient associated with the Appointment object
     * @param provider person object containing the provider associated with the Appointment object
     */
    public Appointment(Date apptDate, int timeslotNum, Person patient, Person provider) {
        this.date = apptDate;
        this.timeslot = new Timeslot(timeslotNum);
        this.patient = patient;
        this.provider = provider;
    }
    /**
     * Gets the Person patient parameter of the current Appointment instance
     * @return Person associated with the appointment
     */
    public Person getPatient() {return this.patient;}
    /**
     * get Person provider for this instance
     * @return Person that is the provider for this appt
     */
    public Person getProvider() {return this.provider;}
    /**
     * Gets the Date parameter of the current Appointment instance
     * @return Date associated with the appointment
     */
    public Date getDate() {return this.date;}
    /**
     * Gets the Timeslot parameter of the current Appointment instance
     * @return Timeslot associated with the appointment
     */
    public Timeslot getTimeslot() {return timeslot;}
    /**
     * Sets the Timeslot object of the current Appointment instance
     * @return Timeslot to set for this appointment
     */
    public void setTimeslot(Timeslot timeslot) {this.timeslot = timeslot;}
    /**
     * set Person for provider
     * @param provider the provider to set
     */
    public void setProvider(Person provider) {this.provider = provider;}
    /**
     * compare two appointments (based on their date, time, provider, and patient)
     * @return int result of comparison, 0 if equal, -1 if less than, 1 if greater than
     */
    @Override
    public int compareTo(Appointment o) {
        if (this.date.compareTo(o.date) < 0) {
            return -1;
        } else if (this.date.compareTo(o.date) > 0) {
            return 1;
        } else {
            if (this.timeslot.compareTo(o.timeslot) < 0) {
                return -1;
            } else if (this.timeslot.compareTo(o.timeslot) > 0) {
                return 1;
            } else {
                if (this.patient.compareTo(o.patient) < 0) {
                    return -1;
                } else if (this.patient.compareTo(o.patient) > 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }
    /**
     * make the appointment into a string
     */
    @Override
    public String toString() {
        return date.toString() + " " + timeslot.toString() + " " + patient.toString() + " " + provider.toString();
    }
    /**
     * see if two appointments equal each other
     * @param o the other object to compare to
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Appointment) {
            Appointment other = (Appointment) o;
            return this.compareTo(other) == 0;
        }
        return false;
    }
}