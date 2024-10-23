package ruclinic;
import util.Date;
/**
 * Represents a patient in the clinic with appointment visit information
 * @author Terry Nguyen, Olivia Schroeder
 */
public class Patient extends Person {
    private Visit visit;
    /**
     * Patient constructor with a profile
     * @param profile profile to build patient off of
     */
    public Patient(Profile profile) {
        super(profile);
        this.visit = null;
    }
    /**
     * Adds an appointment visit to the end of the visits linked list
     * @param apptVisit Visit object to be added
     */
    public void addVisit(Visit apptVisit) {
        if (this.visit == null) {
            this.visit = apptVisit;
            return;
        }
        Visit ptr = this.visit;
        while (ptr.getNext() != null) {
            ptr = ptr.getNext();
        }
        ptr.setNext(apptVisit);
    }
    /**
     * Removes a visit associated with a specified appointment (for when an appointment gets cancelled)
     * @param apptToRemove Appointment to remove from the visits linked list
     */
    public void removeVisit(Appointment apptToRemove) {
        if (this.visit == null) {
            return;
        }
        Visit ptr = this.visit;
        // 1st case: the root visit node needs to be removed
        if (ptr != null && ptr.getAppt().equals(apptToRemove)) {
            this.visit = ptr.getNext(); // Update head
            return;
        }
        // 2nd case: a node besides the root node needs to be removed
        while (ptr != null && ptr.getNext() != null) {
            if (ptr.getNext().getAppt().equals(apptToRemove)) { // then this node's next node is the one that has to be removed
                ptr.setNext(ptr.getNext().getNext());
                return;
            }
            ptr = ptr.getNext();
        }
    }
    /**
     * Traverses the visits linked list to compute the total charge for the current Patient instance
     * @return Integer containing the total charge of all the visits for this patient
     */
    public int charge() {
        Visit ptr = visit;
        int charge = 0;
        while (ptr != null) {
            charge += ptr.getCharge();
            ptr = ptr.getNext();
        }
        return charge;
    }
    /**
     * Traverses the visits linked list for the current Patient instance
     * it finds if patient is available at a certain timeslot & date
     * @param date Date object to check for availability
     * @param timeslot Timeslot object to check for availability
     * @return boolean true if patient is available, false otherwise
     */
    public boolean isAvailable(Date date, Timeslot timeslot) {
        Visit ptr = visit;
        while (ptr != null) {
            if (ptr.getAppt().getDate().equals(date) && ptr.getAppt().getTimeslot().equals(timeslot)) {
                return false;
            }
            ptr = ptr.getNext();
        }
        return true;
    }
    /**
     * Gets the profile associated with the current Patient instance
     * @return Profile object of this patient
     */
    public Profile getProfile() {return profile;}
    /**
     * Gets the visits linked list head associated with the current Patient instance
     * @return Head of the visits linked list of the current patient
     */
    public Visit getVisits() {return visit;}
    /**
     * check if two patients equal
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Patient) {
            Patient other = (Patient) o;
            return this.profile.equals(other.profile);
        }
        return false;
    }
}