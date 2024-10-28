package ruclinic;
import util.Date;
/**
 * Represents profile containing information about a person
 * @author
 */
public class Profile implements Comparable<Profile>{
    private String fname;
    private String lname;
    private Date dob;
    /** Constructor for Profile with date as String*/
    public Profile(String fname, String lname, String dateString) {
        this.fname = fname;
        this.lname = lname;
        this.dob = new Date(dateString);
    }
    /** Constructor for Profile with date as Date object*/
    public Profile(String fname, String lname, Date date) {
        this.fname = fname;
        this.lname = lname;
        this.dob = date;
    }
    /**
     * @return the first name of the profile
     */
    public String getFname() {return this.fname;}
    /**
     * @return the last name of the profile
     */
    public String getLname() {return this.lname;}
    /**
     * @return the DOB of the profile
     */
    public Date getDOB() {return this.dob;}
    /**
     two profiles equal when they have the same fname, lname, and dob
     @param profileToCompare obj we need to make sure is another profile, then compare
     @return true if profiles equal, false otherwise
     */
    @Override
    public boolean equals(Object profileToCompare) {
        if (profileToCompare instanceof Profile profileCasted) {
            return this.compareTo(profileCasted) == 0;
        }
        return false;
    }
    /**
     Return a textual representation of this profile in the format FirstName LastName DOB
     @return   string data type, textual representation of the profile.
     */
    @Override
    public String toString() {
        return this.fname + " " + this.lname + " " + this.dob;
    }
    /**
     Compare profiles based on last name, first name, and their date of birth.
     *   - negative if this profile comes before profileToCompare
     *   - 0 if they are equal
     *   - positive if this profile comes after profileToCompare
     * @param profileToCompare the profile object to be compared
     * @return int result of comparison: -1 (less), zero (equal), or 1 (more)
     */
    @Override
    public int compareTo(Profile profileToCompare) {
        // Compare last names first, if they DO equal then it will go on to the first name comparison and so on
        int lastNameComparison = this.lname.toUpperCase().compareTo(profileToCompare.lname.toUpperCase());
        if (lastNameComparison < 0) {
            return -1;
        } else if (lastNameComparison > 0) {
            return 1;
        }
        int firstNameComparison = this.fname.toUpperCase().compareTo(profileToCompare.fname.toUpperCase());
        if (firstNameComparison < 0) {
            return -1;
        } else if (firstNameComparison > 0) {
            return 1;
        }
        return this.dob.compareTo(profileToCompare.dob);
    }
}
