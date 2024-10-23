package ruclinic;
/**
 * Provider class keeps track of provider location and charging rate
 * @author Terry Nguyen, Olivia Schroeder
 */
public abstract class Provider extends Person {
    private Location location; // to keep track of practice location
    public Provider(Location location, Profile profile) {
        super(profile);
        this.location = location;
    }
    public abstract int rate(); // returns providers charging rate per visit for patients
    /**
     * @return the profile of the provider
     */
    public Profile getProfile() {return this.profile;}
    /**
     * @return the location of the provider
     */
    public Location getLocation() {return this.location;}
    /**
     * toString method for provider
     * @return
     */
    @Override
    public String toString() {
        return "[" + super.toString() + ", " + this.location + ", " + this.location.getCounty() + " " + this.location.getZip() + "]";
    }
    /**
     * Compare providers based on their profiles
     * @param providerToCompare the provider object to be compared
     * @return boolean result if their profiles are equal
     */
    @Override
    public boolean equals(Object providerToCompare) {
        if (providerToCompare instanceof Provider providerCasted) {
            return this.compareTo(providerCasted) == 0;
        }
        return false;
    }
}