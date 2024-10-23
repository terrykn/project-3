package util;
/**
 * Enum is used in the private Sort class method, compareAppointments,
 * to determine which key to sort by
 * @author Terry Nguyen, Olivia Schroeder
 */
public enum SortKey {
    DATE,
    TIMESLOT,
    PROVIDER,
    PROVIDER_FNAME,
    COUNTY,
    PATIENT;
}
