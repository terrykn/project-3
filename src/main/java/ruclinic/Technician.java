package ruclinic;
/**
 * Represents technician in the clinic
 * @author Terry Nguyen, Olivia Schroeder
 */
public class Technician extends Provider {
    private int ratePerVisit;
    /**
     * Constructor for Technician
     * @param profile the profile of the technician
     * @param location the location of the technician
     * @param ratePerVisit the rate per visit of the technician
     */
    public Technician(Profile profile, Location location, int ratePerVisit) {
        super(location, profile);
        this.ratePerVisit = ratePerVisit;
    }
    /**
     * @return the rate of the technician
     */
    @Override
    public int rate() {
        return ratePerVisit;
    }
    /**
     * @return the string of the technician in the format
     */
    @Override
    public String toString() {
        return super.toString() + "[rate: $" + this.ratePerVisit + ".00]";
    }
    /**
     * see if two technicians are equal by their profile, location, and specialty
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Technician) {
            Technician technician = (Technician) o;
            return this.getProfile().equals(technician.getProfile()) && this.getLocation().compareTo(technician.getLocation()) == 0 && this.ratePerVisit == technician.ratePerVisit;
        }
        return false;
    }
}