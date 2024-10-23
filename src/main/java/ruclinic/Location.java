package ruclinic;
/**
 * Enum class defining the city locations
 * @author Terry Nguyen, Olivia Schroeder
 */
public enum Location {
    BRIDGEWATER("Somerset", "08807"),
    EDISON("Middlesex", "08817"),
    PISCATAWAY("Middlesex","08854"),
    PRINCETON("Mercer","08542"),
    MORRISTOWN("Morris","07960"),
    CLARK("Union","07066");
    private final String county;
    private final String zip;
    /**
     * Constructor for creating a new Location object with county and zip
     * @param county String containing the county associated with the location
     * @param zip String containing the zip associated with the location
     */
    Location(String county, String zip) {
        this.county = county;
        this.zip = zip;
    }
    /**
     * static method to return the location.ENUM value from a string
     * @param location String containing the location to get the ENUM value of
     * @return Location ENUM value of the location
     */
    public static Location getLocationEnum(String location) {
        for (Location loc : Location.values()) {
            if (loc.toString().equals(location.toUpperCase())) {
                return loc;
            }
        }
        return null;
    }
    /**
     * Gets the county of the current Location instance
     * @return String containing the county of the Location instance
     */
    public String getCounty() {
        return county;
    }
    /**
     * Gets the zip of the current Location instance
     * @return String containing the zip of the Location instance
     */
    public String getZip() {
        return zip;
    }
    /**
     * Compares the current Location's county with another Location's county
     * @param location Location to compare county with
     * @return Returns 0 if this location's county is the same as the other location's county, -1 if less than, and 1 if greater than
     */
    public int compareCounty(Location location) {
        if(county.compareTo(location.getCounty()) > 0){
            return 1;
        }
        else if(county.compareTo(location.getCounty()) < 0){
            return -1;
        }
        return 0;
    }
}