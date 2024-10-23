package ruclinic;
/**
 * Superclass representing a person in the clinic system with a profile
 * @author Terry Nguyen, Olivia Schroeder
 */
public class Person implements Comparable<Person>{
    protected Profile profile;
    /**
     * default constructor
     */
    public Person(Profile profile) {
        this.profile = profile;
    }
    /**
     * @return the profile of the person
     */
    public Profile getProfile() {return this.profile;}
    /**
     * compare two persons based on their profiles (uses profile comparison)
     * @param p the person to compare to
     * @return int result of comparison: -1 (less), zero (equal), or 1 (more)
     */
    @Override
    public int compareTo(Person p) {
        return this.profile.compareTo(p.profile);
    }
    /**
     * @return the string of the person in the format
     */
    @Override
    public String toString() {
        return this.profile.toString();
    }
    /**
     * see if two persons equal
     * @param o the object to compare to
     * @return true if the persons are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Person)) {
            return false;
        }
        Person p = (Person) o;
        return this.profile.equals(p.profile);
    }
}