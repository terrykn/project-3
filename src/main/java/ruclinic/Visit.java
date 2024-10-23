package ruclinic;
/**
 * Class defining nodes in a singly linked list maintaining the list of visits
 * @author Terry Nguyen, Olivia Schroeder
 */
public class Visit {
    private Appointment appointment; //a reference to an appointment object
    private Visit next; //a ref. to the next appointment object in the list
    /**
     * Constructor for a new Visit node with specified Appointment object
     * @param appointment Appointment object containing the appointment for this node
     */
    public Visit(Appointment appointment) {
        this.appointment = appointment;
        this.next = null;
    }
    /**
     * Gets the current Visit node's next node
     * @return Returns the next visit node
     */
    public Visit getNext() {return this.next;}
    /**
     * Sets the current Visit node's next node
     * @param apptVisit Visit node to be set as current's next
     */
    public void setNext(Visit apptVisit) {
        this.next = apptVisit;
    }
    /**
     * Gets the cost of the current Visit node
     * @return Integer containing cost of the visit
     */
    public int getCharge() { return ((Provider) this.appointment.getProvider()).rate();}
    /**
     * Gets the current Visit node's appointment
     * @return Appointment object associated with the current Visit node instance
     */
    public Appointment getAppt() {return this.appointment;}
}
