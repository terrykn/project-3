package ruclinic;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import util.*;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Scanner;

public class ClinicManagerController implements Initializable {
    List<Appointment> allAppts = new List<Appointment>(); // list of appointments
    List<Provider> allProviders = new List<Provider>(); // list of providers from the providers.txt
    List<Patient> allPatients = new List<Patient>(); // list of patients
    List<Technician> allTechnicians = new List<Technician>(); // list of technicians (rotational),can be used with Sort.rotateTechnicians
    private final int LESSTHAN = -1;
    private final int GREATERTHAN = 1;
    private final int EQUAL = 0;

    public final int NEXTTECHNICIANINDEX = 0; // index of the next technician to check for availability in rotation list
    @FXML
    private RadioButton officeVisit, imagingService; // radio buttons for apptTypeClicked
    @FXML
    private ComboBox timeslot, providerOrImaging;
    @FXML
    private DatePicker apptDate, dob;
    @FXML
    private TextField fname, lname;
    @FXML
    private Button scheduleButton, cancelButton;

    /**
     * initialize data for start
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // load provider list and add providers that are DOCTORS to choice drop down
        loadProvidersList();
        for (Provider prov : allProviders) {
            if (prov instanceof Doctor) {
                this.providerOrImaging.getItems().add(prov.getProfile().getFname() + " " + prov.getProfile().getLname());
            }
        }
        // add timeslot options to choice drop down (1-12)
        for(int i = 1; i <= 12; i++) {
            timeslot.getItems().add(new Timeslot(i));
        }

    }

    @FXML
    void apptTypeClicked(ActionEvent event) {
        /**
        if (officeVisit.isSelected()) {
            provider.setDisable(false);
            provider.setOpacity(1.0);
        } else if (imagingService.isSelected()) {
            provider.setDisable(true);
            provider.setOpacity(0.3);
        }
        */
        if (officeVisit.isSelected()) {
            providerOrImaging.getItems().clear();
            for (Provider prov : allProviders) {
                if (prov instanceof Doctor) {
                    this.providerOrImaging.getItems().add(prov.getProfile().getFname() + " " + prov.getProfile().getLname());
                }
            }
            providerOrImaging.setPromptText("Select Provider...");
        } else if (imagingService.isSelected()) {
            providerOrImaging.getItems().clear();
            for (Radiology radiology : Radiology.values()) {
                this.providerOrImaging.getItems().add(radiology.name());
            }
            providerOrImaging.setPromptText("Select Service...");
        }
    }

    @FXML
    void scheduleButtonClicked(ActionEvent event) {
        // MAKE SURE INPUTS ARE NOT NULL, return if so, and check date validities
        if(apptDate.getValue() == null || dob.getValue() == null || fname.getText().isEmpty() || lname.getText().isEmpty() || timeslot.getValue() == null || providerOrImaging.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Missing Data Tokens");
            alert.setContentText("Please fill in all fields.");
            alert.showAndWait();
            return;
        }
        Object[] validatingData = isValid_ApptDate(new Date(apptDate.getValue()));
        if(!(Boolean) validatingData[0]) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Appointment Date is Invalid");
            alert.setContentText((String) validatingData[1]);
            alert.showAndWait();
            return;
        }
        validatingData = isValid_DOB(new Date(dob.getValue()));
        if(!(Boolean) validatingData[0]) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Date of Birth is Invalid");
            alert.setContentText((String) validatingData[1]);
            alert.showAndWait();
            return;
        }
        // check if appt already exists for this person at date and time
        Person patientFromInput = (Person) checkPatientExists(new Patient(new Profile(fname.getText(), lname.getText(), new Date(dob.getValue()))));
        Date apptDateFromInput = new Date(apptDate.getValue());
        if (officeVisit.isSelected()) {
            Appointment newAppt = new Appointment(apptDateFromInput,Timeslot.getTimeslotNumber(convertTimeslotChoiceToTimeslot()),patientFromInput, (Person) convertProviderChoiceToProvider());
            if (allAppts.contains(newAppt)) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Input");
                alert.setHeaderText("Appointment Already Exists");
                alert.setContentText(newAppt.getPatient() + " has an existing appointment at the same time slot.");
                alert.setHeight(250);
                alert.showAndWait();
                return;
            }
            if(!isProviderAvailable(convertProviderChoiceToProvider(), newAppt.getDate(), newAppt.getTimeslot())) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Input");
                alert.setHeaderText("Provider Not Available");
                alert.setContentText(convertProviderChoiceToProvider().getProfile() + " is not available at slot " + Timeslot.getTimeslotNumber(newAppt.getTimeslot()));
                alert.setHeight(250);
                alert.showAndWait();
                return;
            }
            allAppts.add(newAppt);
            // add appt to patients visit list
            if (newAppt.getPatient() instanceof Patient) {
                Patient realPatientObj = (Patient) newAppt.getPatient();
                realPatientObj.addVisit(new Visit(newAppt)); // add visit to patient
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Appointment Scheduled");
            alert.setHeaderText("Appointment Type: Office Visit");
            alert.setContentText("Appointment for " + newAppt.patient + " booked at " + newAppt.getDate() + " " + newAppt.getTimeslot() + " with " + newAppt.getProvider());
            alert.setHeight(250);
            alert.showAndWait();
        } else if (imagingService.isSelected()) {
            // check if next technician is available at timeslot, if not rotate
            // if it is available, check if the imaging room at the technicians location is available during that timeslot
            for(int i = 0; i < allTechnicians.size(); i++) { // for the length of the list,
                if (isProviderAvailable((Provider) this.allTechnicians.get(NEXTTECHNICIANINDEX), apptDateFromInput, convertTimeslotChoiceToTimeslot())) { // if the technician is available at that timeslot
                    // check if the room is available at that timeslot
                    if (!isRoomAvailable(((Provider) this.allTechnicians.get(NEXTTECHNICIANINDEX)).getLocation(), convertRadiologyChoiceToRadiology(), apptDateFromInput, convertTimeslotChoiceToTimeslot())) {
                        Sort.rotateTechnicians(this.allTechnicians);
                        continue;
                    }
                    Person patient = checkPatientExists(new Patient(new Profile(fname.getText(), lname.getText(), new Date(dob.getValue()))));
                    Appointment newAppointment = new Imaging(apptDateFromInput, Timeslot.getTimeslotNumber(convertTimeslotChoiceToTimeslot()), patientFromInput, (Provider) this.allTechnicians.get(NEXTTECHNICIANINDEX), convertRadiologyChoiceToRadiology());
                    if (allAppts.contains(newAppointment)) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Invalid Input");
                        alert.setHeaderText("Appointment Already Exists");
                        alert.setContentText(newAppointment.getPatient() + " has an existing appointment at the same time slot.");
                        alert.setHeight(250);
                        alert.showAndWait();
                        return;
                    }
                    allAppts.add(newAppointment); // add to appt calendar and
                    if (patient instanceof Patient) {
                        Patient realPatientObj = (Patient) patient;
                        realPatientObj.addVisit(new Visit(newAppointment)); // add visit to patient
                    }
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Appointment Scheduled");
                    alert.setHeaderText("Appointment Type: Imaging Service");
                        alert.setContentText("Appointment for " + newAppointment.patient + " booked at " + newAppointment.getDate() + " " + newAppointment.getTimeslot() + " with " + newAppointment.getProvider() + " for " + providerOrImaging.getValue());
                    alert.setHeight(250);
                    alert.showAndWait();
                    Sort.rotateTechnicians(this.allTechnicians);
                    return;
                } else {
                    Sort.rotateTechnicians(this.allTechnicians);
                }
            }
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Technician Not Available");
            alert.setContentText(convertProviderChoiceToProvider().getProfile() + " is not available at slot " + timeslot.getValue());
            alert.setHeight(250);
            alert.showAndWait();
            return;
        }
    }
    /**
     * Check if a date is a valid appointment date, which must be:
     * valid calendar date, not today, not a day before today, not a weekend, and within 6 months of today
     * @param date the date to check
     * @return True if valid, false otherwise
     */
    private Object[] isValid_ApptDate(Date date) {
        String returnStr = "";
        if (!date.isValid()) {
            returnStr = "Appointment date: " + date + " is not a valid calendar date";
        } else if (date.isToday() || date.compareTo(Date.TODAY()) == LESSTHAN) {
            returnStr = "Appointment date: " + date + " is today or a date before today.";
        } else if (!date.isWeekday()) {
            returnStr = "Appointment date: " + date + " is Saturday or Sunday.";
        } else if (date.compareTo(Date.sixMonthsFromToday()) != LESSTHAN) {
            returnStr = "Appointment date: " + date + " is not within six months.";
        }
        boolean bool = date.isValid() && !(date.isToday()) && (date.compareTo(Date.TODAY()) != LESSTHAN) && date.isWeekday() && (date.compareTo(Date.sixMonthsFromToday()) == LESSTHAN);
        return new Object[] {bool, returnStr};
    }
    /**
     * Checks if the instance of Date is a valid date of birth for a patient
     * @param date the date to check
     * @return true if valid, false otherwise
     */
    private Object[] isValid_DOB(Date date) {
        String returnStr = "";
        if (!(date.isValid())) {
            returnStr = "Patient dob: " + date + " is not a valid calendar date";
        } else if (date.compareTo(Date.TODAY()) == GREATERTHAN || date.compareTo(Date.TODAY()) == EQUAL) {
            returnStr = "Patient dob: " + date + " is today or a date after today.";
        }
        boolean bool = date.isValid() && !(date.isToday()) && (date.compareTo(Date.TODAY()) != GREATERTHAN);
        return new Object[] {bool,returnStr};
    }
    /**
     * Read from the providers.txt and make the list of all the different providers
     * provider s can be D (doctors) or T (technicians), which are both extensions
     * of the provider class with their own unique attributes
     */
    private void loadProvidersList() {
        try {
            File file = new File("C:\\Users\\olivi\\IdeaProjects\\project-3\\src\\main\\java\\ruclinic\\providers.txt");
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] providerInfo = line.split("  "); // split every 2 spaces
                // sort each of the providerInfo into their respective attributes
                String type = providerInfo[0]; // "D" for Doctor, "T" for Technician
                String firstName = providerInfo[1];
                String lastName = providerInfo[2];
                Date birthDate = new Date(providerInfo[3]); // convert providerInfo[3] to Date object
                Location location = Location.getLocationEnum(providerInfo[4]);
                // Create a Profile instance using above info
                Profile profile = new Profile(firstName, lastName, birthDate);
                if (type.equals("D")) {
                    Specialty specialty = Specialty.getSpecialtyEnum(providerInfo[5]);
                    String npi = providerInfo[6];
                    Doctor newDoctor = new Doctor(profile, location, specialty, npi);
                    this.allProviders.add(newDoctor);
                } else if (type.equals("T")) {
                    String rate = providerInfo[5];
                    Technician newTechnician = new Technician(profile, location, Integer.parseInt(rate));
                    this.allProviders.add(newTechnician);
                    this.allTechnicians.add(newTechnician); // added to the rotational list
                } else {
                    System.out.println("Invalid provider type.");
                    return;
                }
            }
            System.out.println("Providers loaded to the list.");
            scanner.close();
        } catch (Exception e) {
            System.out.println("An error occurred when trying to load in that file.");
        }
    }
    /**
     * convert the provider chosen from the dropdown to a Provider object
     * @return the Provider object from the allProviders array
     */
    private Provider convertProviderChoiceToProvider() {
        String providerName = (String) providerOrImaging.getValue();
        for (Provider prov : allProviders) {
            if (prov.getProfile().getFname().equals(providerName.split(" ")[0]) && prov.getProfile().getLname().equals(providerName.split(" ")[1])) {
                return prov;
            }
        }
        return null;
    }
    /**
     * convert the radiology service chosen from the dropdown to a Radiology Enum Obj
     * @return  Radiology enum val
     */
    private Radiology convertRadiologyChoiceToRadiology() {
        String radiologyName = (String) providerOrImaging.getValue();
        return Radiology.valueOf(radiologyName);
    }
    /**
     * convert timeslot chosen to a Timeslot object
     */
    private Timeslot convertTimeslotChoiceToTimeslot() {
        return (Timeslot) timeslot.getValue();
    }
    /**
     * checks if a patient already exists from allPatients, if not, add them to the list
     * and returns the patient object
     * @param person the patient object to check
     * @return the Person object found and/or added
     */
    private Patient checkPatientExists(Person person) {
        if (person instanceof Patient) {
            Patient patient = (Patient) person;
            for (Patient p : allPatients) {
                if (p.equals(patient)) {
                    return p;
                }
            }
            allPatients.add(patient);
            return patient;
        } else {
            System.out.println("The person is not a patient.");
            return null;
        }
    }
    /**
     * check if Provider (can be either Doctor or Technician) is available at a timeslot
     * @param provider the provider to check
     * @param apptDate date of appt to check
     * @param timeslot the timeslot to check if available
     */
    private boolean isProviderAvailable(Provider provider, Date apptDate, Timeslot timeslot) {
        Iterator<Appointment> iterator = this.allAppts.iterator();
        while (iterator.hasNext()) {
            Appointment appointment = iterator.next();
            if (appointment.getProvider().equals(provider) && appointment.getDate().equals(apptDate) && appointment.getTimeslot().equals(timeslot)) {
                return false;
            }
        }
        return true;
    }
    /**
     * check if the room at a given location is available at a timeslot and date
     * @param location the location to check
     * @param room Radiology enum, room to check
     * @param apptDate the date of the appointment
     * @param timeslot the timeslot to check
     */
    private boolean isRoomAvailable(Location location, Radiology room, Date apptDate, Timeslot timeslot) {
        Iterator<Appointment> iterator = this.allAppts.iterator();
        while (iterator.hasNext()) {
            Appointment appointment = iterator.next();
            if (appointment instanceof Imaging)  {
                Imaging imaging = (Imaging) appointment;
                // if the room is already booked at that timeslot and date at that location
                Provider provider = (Provider) imaging.getProvider(); // provider of appt
                if (imaging.getRoom().equals(room) && imaging.getDate().equals(apptDate) && imaging.getTimeslot().equals(timeslot) && (provider.getLocation().equals(location))) {
                    return false;
                }
            }
        }
        return true;
    }
}

/*
package com.example.clinic;
import util.Date;
import util.List;
import util.SortKey;
import util.ApptPrintTypes;
import util.Sort;
import java.util.Scanner;
import java.util.Iterator; // this is needed from the methods she gave us in List.
import java.io.File; // needs to be used with Scanner to read from file
/**
 * User interface class to process the command lines entered on the terminal
 * @author Terry Nguyen, Olivia Schroeder
 */

/*
public class ClinicManager {
    List<Appointment> allAppts = new List<Appointment>(); // list of appointments
    List<Provider> allProviders = new List<Provider>(); // list of providers from the providers.txt
    List<Patient> allPatients = new List<Patient>(); // list of patients
    List<Technician> allTechnicians = new List<Technician>(); // list of technicians (rotational),can be used with Sort.rotateTechnicians
    Scanner scan = new Scanner(System.in);
    public final int NEXTTECHNICIANINDEX = 0; // index of the next technician to check for availability in rotation list
    private final int LESSTHAN = -1;
    private final int GREATERTHAN = 1;
    private final int EQUAL = 0;
    /**
     * Runs the Scheduler until Q is entered to terminate it
     */

/*
    public void run() {
        loadProvidersList();
        System.out.println("Clinic Manager is running...");
        while(true) {
            String[] userInput = breakUpInput(scan.nextLine());
            String command;
            if (userInput.length == 0) {continue;} else {
                command = userInput[0];
            }
            switch (command){
                case "D": // schedule doctor appointment
                    d_pressed(userInput);
                    break;
                case "T": // schedule imaging appointment
                    t_pressed(userInput);
                    break;
                case "C": // cancel (same as Project 1)
                    c_pressed(userInput);
                    break;
                case "R": // reschedule (same as Project 1)
                    r_pressed(userInput);
                    break;
                case "PA": // print appts ordered by: appt date, time, providers name (same as Project 1)
                    pa_pressed();
                    break;
                case "PP": // print appts ordered by: patient (same as Project 1)
                    pp_pressed();
                    break;
                case "PL": // print appts ordered by: county name, appt date and time (same as Project 1)
                    pl_pressed();
                    break;
                case "PS": // billing statement for all patients (same as Project 1)
                    ps_pressed();
                    break;
                case "PO": // print office appts ordered y: county name, appt date and time
                    po_pressed();
                    break;
                case "PI": // print imaging appts ordered by: county name, appt date and time
                    pi_pressed();
                    break;
                case "PC": // print expected credit amounts for providers for seeing patients ordered by provider profile
                    pc_pressed();
                    break;
                case "Q": // quit (same as Project 1)
                    q_pressed();
                    return;
                case "":
                    break;
                default:
                    System.out.println("Invalid command!");
                    break;
            }
        }
    }
    /**
     * Breaks up the user input into specified fields so the command can be processed
     * @param input String contaning user input to be split
     * @return String array containing the fields
     */
/*
    private String[] breakUpInput(String input) {return input.split(",");}

    /**
     * When q is pressed, close scanner to stop input and terminate program
     *//*
    private void q_pressed() {
        scan.close();
        System.out.println("Clinic Manager terminated.");
    }
    /**
     * D command - schedules a doctor appointment, Assuming appointment information is valid
     * @param userInput string array containing the user input
     */
/*
    private void d_pressed(String[] userInput) {
        if (!checkInputRequirements1_5(userInput)) {return;} // check if requirements 1-5 are met
        try {
            int npi = Integer.parseInt(userInput[6]); // make sure it's a number
        } catch (NumberFormatException e) {
            System.out.println(userInput[6] + " - provider doesn't exist.");
            return;
        }
        Doctor doctor = findDoctorByNPI(userInput[6]);
        if (doctor == null) {
            System.out.println(userInput[6] + " - provider doesn't exist.");
            return;
        }
        // find the patient object in allPatients, if not found, add them to the list
        Person patient = checkPatientExists(new Patient(new Profile(userInput[3], userInput[4], userInput[5])));
        Appointment newAppointment = new Appointment(userInput[1], userInput[2], patient);
        newAppointment.setProvider(doctor);
        if (allAppts.contains(newAppointment)) { // check if appointment already exists for this person at date and time
            System.out.println(newAppointment.getPatient() + " has an existing appointment at the same time slot.");
            return;
        }
        // make sure the doctor is available at that timeslot by traversing the list of appointments
        if (!isProviderAvailable(doctor, newAppointment.getDate(), newAppointment.getTimeslot())) {
            System.out.println(doctor + " is not available at slot " + Timeslot.getTimeslotNumber(newAppointment.getTimeslot()));
            return;
        }
        allAppts.add(newAppointment);
        if (patient instanceof Patient) {
            Patient realPatientObj = (Patient) patient;
            realPatientObj.addVisit(new Visit(newAppointment)); // add visit to patient
        }
        System.out.println(newAppointment + " booked.");
    }
    /**
     * T command - schedules an imaging appointment
     * Assuming appointment information is valid, checks the appointment exists before scehduling it
     * @param userInput string array containing the user input
     *//*
    private void t_pressed(String[] userInput) {
        if (!checkInputRequirements1_5(userInput)) {return;} // check if requirements 1-5 are met
        // if the imaging service is not a valid service, return
        if (!(userInput[6].toUpperCase().equals(Radiology.CATSCAN.name()) || userInput[6].toUpperCase().equals(Radiology.ULTRASOUND.name()) || userInput[6].toUpperCase().equals(Radiology.XRAY.name()))) {
            System.out.println(userInput[6] + " - imaging service not provided.");
            return;
        }
        // check if next technician is available at timeslot, if not rotate
        // if it is available, check if the imaging room at the technicians location is available during that timeslot
        for(int i = 0; i < allTechnicians.size(); i++) { // for the length of the list,
            if (isProviderAvailable((Provider) this.allTechnicians.get(NEXTTECHNICIANINDEX), new Date(userInput[1]), new Timeslot(Integer.parseInt(userInput[2])))) { // if the technician is available at that timeslot
                // check if the room is available at that timeslot
                if (!isRoomAvailable(((Provider) this.allTechnicians.get(NEXTTECHNICIANINDEX)).getLocation(), Radiology.valueOf(userInput[6].toUpperCase()), new Date(userInput[1]), new Timeslot(Integer.parseInt(userInput[2])))) {
                    Sort.rotateTechnicians(this.allTechnicians);
                    continue;
                }
                Radiology room = Radiology.valueOf(userInput[6].toUpperCase());
                Person patient = checkPatientExists(new Patient(new Profile(userInput[3], userInput[4], userInput[5])));
                Appointment newAppointment = new Imaging(new Date(userInput[1]), Integer.parseInt(userInput[2]), patient, (Provider) this.allTechnicians.get(NEXTTECHNICIANINDEX), room);
                allAppts.add(newAppointment); // add to appt calendar and
                if (patient instanceof Patient) {
                    Patient realPatientObj = (Patient) patient;
                    realPatientObj.addVisit(new Visit(newAppointment)); // add visit to patient
                }
                System.out.println(newAppointment + " booked.");
                Sort.rotateTechnicians(this.allTechnicians);
                return;
            } else {
                Sort.rotateTechnicians(this.allTechnicians);
            }
        }
        System.out.println("Cannot find an available technician at all locations for " + Radiology.valueOf(userInput[6].toUpperCase()).name() + " at slot " + Timeslot.getTimeslotNumber(new Timeslot(Integer.parseInt(userInput[2]))) + ".");
    }
    /**
     * C command - cancels an appointment
     * Assuming appointment information is valid, checks the appointment exists before removing it
     * @param userInput string array
     *//*
    private void c_pressed(String[] userInput) {
        if (userInput.length != 6) {
            System.out.println("Missing data tokens.");
            return;
        }
        Person personPatient = checkPatientExists(new Patient(new Profile(userInput[3], userInput[4], userInput[5])));
        Appointment appointment = new Appointment(userInput[1], userInput[2], personPatient);
        if (!(allAppts.contains(appointment))) { // if it does not contain the appt, return
            // based on her output, wants us to print patient in format given in userInput, not in the format of the patient name stored in the system (so cant simply print patient from the appt object we made)
            System.out.println(appointment.getDate() + " " + appointment.getTimeslot() + " " + (new Profile(userInput[3], userInput[4], userInput[5])) + " - appointment does not exist.");
            return;
        }
        if (personPatient instanceof Patient) {
            Patient patient = (Patient) personPatient;
            patient.removeVisit(appointment); // then remove the appointment from the Patients linked list of visits
            allAppts.remove(appointment); // remove the appointment from the list of all appointments
            System.out.println(appointment.getDate() + " " + appointment.getTimeslot() + " " + (new Profile(userInput[3], userInput[4], userInput[5])) + " - appointment has been canceled.");
        } else {
            System.out.println("The person associated with this appointment is not a patient.");
        }
    }
    /**
     * R command - reschedules an appointment
     * Check if an appointment exists, the new timeslot is valid, and that the new timeslot
     * is available on that same date for the same provider
     * @param userInput string array of user input
     *//*
    private void r_pressed(String[] userInput) {
        if (userInput.length != 7) {
            System.out.println("Missing data tokens.");
            return;
        }
        Appointment appointment = new Appointment(userInput[1], userInput[2], new Person(new Profile(userInput[3], userInput[4], userInput[5])));
        if (!(allAppts.contains(appointment))) { // if it does not contain the appt, return
            System.out.println(appointment.getDate() + " " + appointment.getTimeslot() + " " + appointment.getPatient() + " does not exist.");
            return;
        }
        appointment = (Appointment) allAppts.get(allAppts.indexOf(appointment));
        Person personPatient = appointment.getPatient();
        if (personPatient instanceof Patient) {
            Provider provider = (Provider) appointment.getProvider();
            Patient patient = (Patient) personPatient;
            if (!isProviderAvailable(provider, appointment.getDate(), new Timeslot(Integer.parseInt(userInput[6])))) {
                System.out.println(provider + " is not available at slot " + Timeslot.getTimeslotNumber(appointment.getTimeslot()));
                return;
            } else if (!patient.isAvailable(appointment.getDate(), new Timeslot(Integer.parseInt(userInput[6])))) {
                System.out.println(patient.getProfile() + " has an existing appointment at " + appointment.getDate() + " " + (new Timeslot(Integer.parseInt(userInput[6]))));
                return;
            }
            appointment.setTimeslot(new Timeslot(Integer.parseInt(userInput[6]))); // changing it in the allAppts array simaltaneously changes it in the visits linked list because they are the same reference
            System.out.println("Rescheduled to " + appointment);
        } else {
            System.out.println("Error in r_pressed, The person associated with this appointment is not a patient.");
        }
    }
    /**
     for when PS print billing statements is pressed, do just that
     *//*
    private void ps_pressed(){
        if(isScheduleCalendarEmpty()) {return;}
        Sort.patients(this.allPatients);
        System.out.println("\n** Billing statement ordered by patient. **");
        for (int i = 0; i < allPatients.size(); i++) {
            int numberedItem = i + 1;
            int totalCharge = 0;
            Visit ptr = this.allPatients.get(i).getVisits();
            while (ptr != null) {
                totalCharge += ptr.getCharge();
                ptr = ptr.getNext();
            }
            System.out.println("(" + numberedItem + ") " + this.allPatients.get(i) + " [due: $" + formatMoneyString(totalCharge) + "]");
        }
        System.out.println("** end of list **");
        // remove all appts from appt calendar as they are now only in the visits of each patient
        clearApptCalendar();
    }
    /**
     * pc command prints the expected credit amounts for providers for seeing patients ordered by provider profile
     *//*
    private void pc_pressed() {
        if (isScheduleCalendarEmpty()) {
            return;
        }
        Sort.providers(this.allProviders);
        System.out.println("\n** Credit amount ordered by provider. **");
        for (int i = 0; i < allProviders.size(); i++) {
            int numberedItem = i + 1;
            int totalCharge = 0;
            Provider provider = this.allProviders.get(i);
            for (int j = 0; j < allAppts.size(); j++) {
                Appointment appt = allAppts.get(j);
                if (appt.getProvider().equals(provider)) {
                    totalCharge += ((Provider) appt.getProvider()).rate();
                }
            }
            System.out.println("(" + numberedItem + ") " + provider.getProfile() + " [credit amount: $" + formatMoneyString(totalCharge) + "]");
        }
        System.out.println("** end of list **");
    }
    /**
     * PA Prints the appointments in the List object ordered by date/timeslot, provider name
     *//*
    private void pa_pressed() {
        if(isScheduleCalendarEmpty()) {return;}
        List<SortKey> sortBy = new List<SortKey>();
        sortBy.add(SortKey.DATE);
        sortBy.add(SortKey.TIMESLOT);
        sortBy.add(SortKey.PROVIDER);
        System.out.println("\n** List of appointments, ordered by date/time/provider.");
        Sort.appointment(this.allAppts, sortBy);
        printAppts(ApptPrintTypes.ALL);
        System.out.println("** end of list **");
    }
    /**
     * pp command prints the appointments ordered by patient
     * (by last name, first name, date of birth, then
     * appointment date and time).
     *//*
    private void pp_pressed() {
        if(isScheduleCalendarEmpty()) {return;}
        List<SortKey> sortBy = new List<SortKey>();
        sortBy.add(SortKey.PATIENT);
        sortBy.add(SortKey.DATE);
        sortBy.add(SortKey.TIMESLOT);
        System.out.println("\n** Appointments ordered by patient/date/time **");
        Sort.appointment(this.allAppts, sortBy);
        printAppts(ApptPrintTypes.ALL);
        System.out.println("** end of list **");
    }
    /**
     * pl command prints the appointments ordered by county name, appt date and time
     */
/*
    private void pl_pressed() {
        if(isScheduleCalendarEmpty()) {return;}
        List<SortKey> sortBy = new List<SortKey>();
        sortBy.add(SortKey.COUNTY);
        sortBy.add(SortKey.DATE);
        sortBy.add(SortKey.TIMESLOT);
        System.out.println("\n** List of appointments, ordered by county/date/time.");
        Sort.appointment(this.allAppts, sortBy);
        printAppts(ApptPrintTypes.ALL);
        System.out.println("** end of list **");
    }
    /**
     * po command to display the list of office appointments,
     * sorted by the county name, then date and time.
     */
/*
    private void po_pressed() {
        if(isScheduleCalendarEmpty()) {return;}
        List<SortKey> sortBy = new List<SortKey>();
        sortBy.add(SortKey.COUNTY);
        sortBy.add(SortKey.DATE);
        sortBy.add(SortKey.TIMESLOT);
        sortBy.add(SortKey.PROVIDER_FNAME); // if those all equal, then just sort by providers FIRSt name, didnt specify in directions, but shows in output
        System.out.println("\n** List of office appointments ordered by county/date/time.");
        Sort.appointment(this.allAppts, sortBy);
        printAppts(ApptPrintTypes.OFFICE);
        System.out.println("** end of list **");
    }
    /**
     * pi command to display the list of imaging appointments,
     * sorted by the county name, then date and time.
     *//*
    private void pi_pressed() {
        if(isScheduleCalendarEmpty()) {return;}
        List<SortKey> sortBy = new List<SortKey>();
        sortBy.add(SortKey.COUNTY);
        sortBy.add(SortKey.DATE);
        sortBy.add(SortKey.TIMESLOT);
        sortBy.add(SortKey.PROVIDER_FNAME); // if those all equal, then just sort by providers FIRSt name, didnt specify in directions, but shows in output
        System.out.println("\n** List of radiology appointments ordered by county/date/time.");
        Sort.appointment(this.allAppts, sortBy);
        printAppts(ApptPrintTypes.IMAGING);
        System.out.println("** end of list **");
    }
    /**
     * Read from the providers.txt and make the list of all the different providers
     * provider s can be D (doctors) or T (technicians), which are both extensions
     * of the provider class with their own unique attributes
     *//*
    private void loadProvidersList() {
        try {
            File file = new File("ruclinic/providers.txt");
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] providerInfo = line.split("  "); // split every 2 spaces
                // sort each of the providerInfo into their respective attributes
                String type = providerInfo[0]; // "D" for Doctor, "T" for Technician
                String firstName = providerInfo[1];
                String lastName = providerInfo[2];
                Date birthDate = new Date(providerInfo[3]); // convert providerInfo[3] to Date object
                Location location = Location.getLocationEnum(providerInfo[4]);
                // Create a Profile instance using above info
                Profile profile = new Profile(firstName, lastName, birthDate);
                if (type.equals("D")) {
                    Specialty specialty = Specialty.getSpecialtyEnum(providerInfo[5]);
                    String npi = providerInfo[6];
                    Doctor newDoctor = new Doctor(profile, location, specialty, npi);
                    this.allProviders.add(newDoctor);
                } else if (type.equals("T")) {
                    String rate = providerInfo[5];
                    Technician newTechnician = new Technician(profile, location, Integer.parseInt(rate));
                    this.allProviders.add(newTechnician);
                    this.allTechnicians.add(newTechnician); // added to the rotational list
                } else {
                    System.out.println("Invalid provider type.");
                    return;
                }
            }
            System.out.println("Providers loaded to the list.");
            print_ProvidersLoadedInList();
            print_RotationalTechnicianList();
            scanner.close();
        } catch (Exception e) {
            System.out.println("An error occurred when trying to load in that file.");
        }
    }
    /**
     * print providers list using the custom iterator like the directions wanted
     *//*
    private void print_ProvidersLoadedInList() {
        Sort.providers(this.allProviders);
        Iterator<Provider> iterator = this.allProviders.iterator();
        while (iterator.hasNext()) {
            Provider provider = iterator.next();
            if (provider instanceof Doctor) {
                Doctor doctor = (Doctor) provider;
                System.out.println(provider.toString());
            } else if (provider instanceof Technician) {
                Technician technician = (Technician) provider;
                System.out.println(provider.toString());
            }
        }
        System.out.println();
    }
    /**
     * print the rotational technician list
     *//*
    private void print_RotationalTechnicianList() {
        System.out.println("Rotation list for the technicians.");
        Sort.reverseTechnicians(this.allTechnicians);
        Iterator<Technician> iterator = this.allTechnicians.iterator();
        while (iterator.hasNext()) {
            Technician technician = iterator.next();
            System.out.print(technician.profile.getFname().toUpperCase() + " " +
                    technician.profile.getLname().toUpperCase() + " (" +
                    technician.getLocation().name() + ")");
            if (iterator.hasNext()) {
                System.out.print(" --> ");
            }
        }
        System.out.println("\n");
    }
    /**
     * checks requirements 1-5 for commands D and T
     * 1) check if apptDate isValid_ApptDate
     * 2) if timeslot exists (is valid)
     * 3) if DOB is valid
     * 4) she has this listed as 4 but based on output & logic this is really #1, check if missing data tokens
     * 5) An appointment with the same patient profile, date, and timeslot already exists.
     * @param userInput the user input sliced array
     * @return true if all requirements are met, false otherwise
     *//*
    private boolean checkInputRequirements1_5(String[] userInput) {
        if (userInput.length != 7) {
            System.out.println("Missing data tokens.");
            return false;
        }
        if (!isValid_ApptDate((new Date(userInput[1])))) {return false;}
        if (!Timeslot.isValidTimeslot(userInput[2])) {
            System.out.println(userInput[2] + " is not a valid time slot.");
            return false;
        }
        if (!isValid_DOB(new Date(userInput[5]))) {return false;}
        Appointment newAppt = new Appointment(userInput[1], userInput[2], new Person(new Profile(userInput[3], userInput[4], userInput[5])));;
        if (allAppts.contains(newAppt)) {
            System.out.println(newAppt.getPatient() + " has an existing appointment at the same time slot.");
            return false;
        }
        return true;
    }
    /**
     * checks if a patient already exists from allPatients, if not, add them to the list
     * and returns the patient object
     * @param person the patient object to check
     * @return the Person object found and/or added
     *//*
    private Patient checkPatientExists(Person person) {
        if (person instanceof Patient) {
            Patient patient = (Patient) person;
            for (Patient p : allPatients) {
                if (p.equals(patient)) {
                    return p;
                }
            }
            allPatients.add(patient);
            return patient;
        } else {
            System.out.println("The person is not a patient.");
            return null;
        }
    }
    /**
     * find a doctor by their NPI n
     * @param npiString the NPI string to search for
     * @return the Doctor object found, or null if not found
     *//*
    private Doctor findDoctorByNPI(String npiString) {
        Iterator<Provider> iterator = this.allProviders.iterator();
        while (iterator.hasNext()) {
            Provider provider = iterator.next();
            if (provider instanceof Doctor) {
                Doctor doctor = (Doctor) provider;
                if (doctor.getNPI().equals(npiString)) {
                    return doctor;
                }
            }
        }
        return null;
    }
    /**
     * check if Provider (can be either Doctor or Technician) is available at a timeslot
     * @param provider the provider to check
     * @param apptDate date of appt to check
     * @param timeslot the timeslot to check if available
     *//*
    private boolean isProviderAvailable(Provider provider, Date apptDate, Timeslot timeslot) {
        Iterator<Appointment> iterator = this.allAppts.iterator();
        while (iterator.hasNext()) {
            Appointment appointment = iterator.next();
            if (appointment.getProvider().equals(provider) && appointment.getDate().equals(apptDate) && appointment.getTimeslot().equals(timeslot)) {
                return false;
            }
        }
        return true;
    }
    /**
     * check if the room at a given location is available at a timeslot and date
     * @param location the location to check
     * @param room Radiology enum, room to check
     * @param apptDate the date of the appointment
     * @param timeslot the timeslot to check
     *//*
    private boolean isRoomAvailable(Location location, Radiology room, Date apptDate, Timeslot timeslot) {
        Iterator<Appointment> iterator = this.allAppts.iterator();
        while (iterator.hasNext()) {
            Appointment appointment = iterator.next();
            if (appointment instanceof Imaging)  {
                Imaging imaging = (Imaging) appointment;
                // if the room is already booked at that timeslot and date at that location
                Provider provider = (Provider) imaging.getProvider(); // provider of appt
                if (imaging.getRoom().equals(room) && imaging.getDate().equals(apptDate) && imaging.getTimeslot().equals(timeslot) && (provider.getLocation().equals(location))) {
                    return false;
                }
            }
        }
        return true;
    }
    /**
     * Helper method for printing appointments to view how they are currently sorted by key
     * @param key the ApptPrintTypes enum key of what to print
     *//*
    private void printAppts(ApptPrintTypes key) {
        // Print the sorted appointments using the custom iterator
        Iterator<Appointment> iterator = this.allAppts.iterator();
        while (iterator.hasNext()) {
            Appointment appt = iterator.next();
            if (appt != null) {
                if(key == ApptPrintTypes.ALL) {
                    System.out.println(appt);
                } else if (key == ApptPrintTypes.IMAGING) {
                    if (appt instanceof Imaging) {
                        System.out.println(appt);
                    }
                } else if (key == ApptPrintTypes.OFFICE) {
                    if (!(appt instanceof Imaging)) { // if not imaging type its office type
                        System.out.println(appt);
                    }
                } else {
                    System.out.println("!!! Error in key input for printing appointments in ClinicManager, method printAppts");
                }
            }
        }
    }
    /**
     * helper method for print commands to check if schedule calendar is empty
     * @return true if the schedule calendar is empty, false otherwise
     *//*
    private boolean isScheduleCalendarEmpty() {
        if (this.allAppts.size() == 0) {
            System.out.println("Schedule calendar is empty.");
            return true;
        }
        return false;
    }
    /**
     * Formats a number into the proper format including commas and periods "1,300.00"
     * @param amount int type to convert to format with two decimal places
     * @return String containing the amount in the proper format
     *//*
    private String formatMoneyString(double amount) {
        return String.format("%,.2f", amount);
    }
    /**
     * Clear the appt calendar (this.allAppts) but keep its length, but its size() is going to be zero because all appts are removed.
     *//*
    private void clearApptCalendar() {
        while (this.allAppts.size() > 0) {
            this.allAppts.remove(this.allAppts.get(0)); // all elements shift left so next element to remove is always index 0
        }
    }
    /**
     * Check if a date is a valid appointment date, which must be:
     * valid calendar date, not today, not a day before today, not a weekend, and within 6 months of today
     * @param date the date to check
     * @return True if valid, false otherwise
     *//*
    private boolean isValid_ApptDate(Date date) {
        if (!date.isValid()) {
            System.out.println("Appointment date: " + date + " is not a valid calendar date");
        } else if (date.isToday() || date.compareTo(Date.TODAY()) == LESSTHAN) {
            System.out.println("Appointment date: " + date + " is today or a date before today.");
        } else if (!date.isWeekday()) {
            System.out.println("Appointment date: " + date + " is Saturday or Sunday.");
        } else if (date.compareTo(Date.sixMonthsFromToday()) != LESSTHAN) {
            System.out.println("Appointment date: " + date + " is not within six months.");
        }
        return date.isValid() && !(date.isToday()) && (date.compareTo(Date.TODAY()) != LESSTHAN) && date.isWeekday() && (date.compareTo(Date.sixMonthsFromToday()) == LESSTHAN);
    }
    /**
     * Checks if the instance of Date is a valid date of birth for a patient
     * @param date the date to check
     * @return true if valid, false otherwise
     *//*
    private boolean isValid_DOB(Date date) {
        if (!(date.isValid())) {
            System.out.println("Patient dob: " + date + " is not a valid calendar date");
        } else if (date.compareTo(Date.TODAY()) == GREATERTHAN || date.compareTo(Date.TODAY()) == EQUAL) {
            System.out.println("Patient dob: " + date + " is today or a date after today.");
        }
        return date.isValid() && !(date.isToday()) && (date.compareTo(Date.TODAY()) != GREATERTHAN);
    }
}
 */