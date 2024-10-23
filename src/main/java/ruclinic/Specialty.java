package ruclinic;
/**
 * Enum class defining charge rates for different doctor specialties
 * @author
 */
public enum Specialty {
    FAMILY (250),
    PEDIATRICIAN (300),
    ALLERGIST (350);
    private final int charge;
    /**
     * Constructor for Specialty
     * @param the charge for this specialty
     */
    Specialty(int charge) {
        this.charge = charge;
    }
    /**
     * get the specialty enum that matches this string, if it exists
     * @param specialty   the string to match to a specialty
     */
    public static Specialty getSpecialtyEnum(String specialty) {
        for (Specialty spec : Specialty.values()) {
            if (spec.name().equals(specialty.toUpperCase())) {
                return spec;
            }
        }
        return null;
    }
    /**
     * Get the charge for this specialty
     * @return the charge for this specialty
     */
    public int getCharge() {
        return charge;
    }
    /**
     Return a textual representation
     @return string of how much this specialty charges
     */
    @Override
    public String toString() {
        return this.name() + " - $"+ charge;
    }
}
