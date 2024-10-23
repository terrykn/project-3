package ruclinic;
/**
 * Represents Doctor in the clinic containing information about specialty and doctor NPI
 * @author Terry Nguyen, Olivia Schroeder
 */
public class Doctor extends Provider {
    private Specialty specialty;//encapsulate the rate per visit based on specialty
    private String npi; //National Provider Identification unique to the doctor
    /**
     * Constructor for Doctor
     * @param profile the profile of the doctor
     * @param location the location of the doctor
     * @param specialty the specialty of the doctor
     * @param npi the National Provider Identification of the doctor
     */
    public Doctor(Profile profile, Location location, Specialty specialty, String npi) {
        super(location, profile);
        this.specialty = specialty;
        this.npi = npi;
    }
    /**
     * @return the specialty of the doctor
     */
    public Specialty getSpecialty() {return this.specialty;}
    /**
     * @return the National Provider Identification (npi) of the doctor
     */
    public String getNPI() {return this.npi;}
    /**
     * @return the rate of the doctor
     */
    @Override
    public int rate() {
        return this.getSpecialty().getCharge();
    }
    /**
     * @return the string of the doctor in the format
     */
    @Override
    public String toString() {
        return super.toString() + "[" + this.specialty.name() + ", #" + this.npi + "]";
    }
    /**
     * see if two doctor objects equal by their npi
     * @param o the object to compare to
     * @return true if the two doctors have the same npi, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Doctor) {
            Doctor d = (Doctor) o;
            return this.npi.equals(d.npi);
        }
        return false;
    }
}