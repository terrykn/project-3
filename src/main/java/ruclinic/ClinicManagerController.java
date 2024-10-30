package ruclinic;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import util.*;

import javax.swing.*;
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
    private RadioButton officeVisit, imagingService, sort_PA, sort_PP, sort_PL, sort_PO, sort_PI; // radio buttons for apptTypeClicked
    @FXML
    private ComboBox timeslot, providerOrImaging, t2_timeslot, t2_newTimeslot;
    @FXML
    private DatePicker apptDate, dob, t2_apptDate, t2_dob;
    @FXML
    private TextField fname, lname, t2_fname, t2_lname;
    @FXML
    private TextArea sortOutput;
    @FXML
    private Button scheduleButton, cancelButton, t2_rescheduleButton, chooseProviderFileButton, t1_clearInput, t2_clearInput;
    @FXML
    private TableColumn<ObservableList<Object>, String> t4_fullName, t4_dob, t4_credits, t5_fullName, t5_dob, t5_due; // t4 is provider credit, t5 is patient billing
    @FXML
    private TableView<ObservableList<Object>> t4_table, t5_table;
    /**
     * initialize data for start
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // add timeslot options to choice drop down (1-12)
        for(int i = 1; i <= 12; i++) {
            timeslot.getItems().add(new Timeslot(i));
            t2_timeslot.getItems().add(new Timeslot(i));
            t2_newTimeslot.getItems().add(new Timeslot(i));
        }
        // for tab 3, set its default when this is intialized
        sort_PA.setSelected(true);
        pa_pressed();
        // for tab 4 and 5 tableview need to set cell value factories
        t4_fullName.setCellValueFactory(cellData -> new SimpleStringProperty((String) cellData.getValue().get(0)));
        t5_fullName.setCellValueFactory(cellData -> new SimpleStringProperty((String) cellData.getValue().get(0)));
        t4_dob.setCellValueFactory(cellData -> new SimpleStringProperty((String) cellData.getValue().get(1)));
        t5_dob.setCellValueFactory(cellData -> new SimpleStringProperty((String) cellData.getValue().get(1)));
        t4_credits.setCellValueFactory(cellData -> new SimpleStringProperty((String) cellData.getValue().get(2)));
        t5_due.setCellValueFactory(cellData -> new SimpleStringProperty((String) cellData.getValue().get(2)));
    }
    /**
     * button for choosing .txt file of Providers
     * @param event of button being pressed
     */
    @FXML
    void fileButtonPressed(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files","*.txt"));
        // Get the window from the event source
        Window window = ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            try {
                try {
                    loadProvidersList(file);
                } catch (Exception e) {
                    //System.out.println("extra for testing");
                }
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid File Input");
                alert.setHeaderText("File is Incompatible");
                alert.setContentText("Please Choose Another Compatible File.");
                alert.showAndWait();
                return;
            }
        }
    }
    /**
     * for when the radio buttons are clicked in the schedule/cancel tab.
     * changes the providerOrImaging dropdown to show the correct options
     * @param event of radio button being clicked
     */
    @FXML
    void apptTypeClicked(ActionEvent event) {
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
    /**
     * for scheduling both office and imaging appts when the schedule button is clicked
     * @param event of the schedule button being pressed
     */
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
            updateData();
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
                    updateData();
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
     * for cancelling an appointment in the Schedule / Cancel tab
     * @param event of the cancel button being pressed
     */
    @FXML
    void cancelButtonClicked(ActionEvent event) {
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
        // now check if the appointment exists, if not, return
        Person personPatient = (Person) checkPatientExists(new Patient(new Profile(fname.getText(), lname.getText(), new Date(dob.getValue()))));
        Appointment appointment = new Appointment(new Date(apptDate.getValue()), Timeslot.getTimeslotNumber(convertTimeslotChoiceToTimeslot()), personPatient, (Person) convertProviderChoiceToProvider());
        if (!(allAppts.contains(appointment))) { // if it does not contain the appt, return
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Appointment Does Not Exist!");
            alert.setContentText(appointment.getDate() + " " + appointment.getTimeslot() + " " + (new Profile(fname.getText(), lname.getText(), new Date(dob.getValue()))) + " - appointment does not exist.");
            alert.setHeight(250);
            alert.showAndWait();
            return;
        }
        // now remove the appt from the allAppts list and from the persons visit array
        if (personPatient instanceof Patient) {
            Patient patient = (Patient) personPatient;
            patient.removeVisit(appointment); // then remove the appointment from the Patients linked list of visits
            allAppts.remove(appointment); // remove the appointment from the list of all appointments
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Appointment Cancellation");
            alert.setHeaderText("Appointment Canceled");
            alert.setContentText(appointment.getDate() + " " + appointment.getTimeslot() + " " + (new Profile(fname.getText(), lname.getText(), new Date(dob.getValue()))) + " - appointment has been canceled.");
            alert.setHeight(250);
            alert.showAndWait();
            updateData();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Person Not a Patient");
            alert.setContentText("The person associated with this appointment is not a patient.");
            alert.setHeight(250);
            alert.showAndWait();
        }
    }
    @FXML
    /**
     * for rescheduling an appointment in tab 2
     * @param event of the reschedule button being pressed
     */
    void rescheduleButtonClicked(ActionEvent event) {
        // MAKE SURE INPUTS ARE NOT NULL, return if so, and check dates valid?
        if(t2_apptDate.getValue() == null || t2_dob.getValue() == null || t2_fname.getText().isEmpty() || t2_lname.getText().isEmpty() || t2_timeslot.getValue() == null || t2_newTimeslot.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Missing Data Tokens");
            alert.setContentText("Please fill in all fields.");
            alert.showAndWait();
            return;
        }
        Object[] validatingData = isValid_ApptDate(new Date(t2_apptDate.getValue()));
        if(!(Boolean) validatingData[0]) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Appointment Date is Invalid");
            alert.setContentText((String) validatingData[1]);
            alert.showAndWait();
            return;
        }
        validatingData = isValid_DOB(new Date(t2_dob.getValue()));
        if(!(Boolean) validatingData[0]) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Date of Birth is Invalid");
            alert.setContentText((String) validatingData[1]);
            alert.showAndWait();
            return;
        }
        // check if the appointment exists, if not, return
        Person patientFromInput = (Person) checkPatientExists(new Patient(new Profile(t2_fname.getText(), t2_lname.getText(), new Date(t2_dob.getValue()))));
        Appointment appointment = new Appointment(new Date(t2_apptDate.getValue()), Timeslot.getTimeslotNumber((Timeslot) t2_timeslot.getValue()), patientFromInput, (Person) allProviders.get(0)); //set to random provider temporarily (this isnt considered when comparing appts)
        if (!(allAppts.contains(appointment))) { // if it does not contain the appt, return
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Appointment Does Not Exist!");
            alert.setContentText(appointment.getDate() + " " + appointment.getTimeslot() + " " + (new Profile(t2_fname.getText(), t2_lname.getText(), new Date(t2_dob.getValue()))) + " - appointment does not exist.");
            alert.setHeight(250);
            alert.showAndWait();
            return;
        }
        appointment = (Appointment) allAppts.get(allAppts.indexOf(appointment));
        // make sure not imaging appt because we cannot reschedule those
        if (appointment instanceof Imaging) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Cannot Reschedule Imaging Appt");
            alert.setContentText("Imaging appointments cannot be rescheduled.");
            alert.setHeight(250);
            alert.showAndWait();
            return;
        }
        // check if provider is available at new timeslot
        Provider prov = (Provider) appointment.getProvider();
        if(!isProviderAvailable(prov, appointment.getDate(), (Timeslot) t2_newTimeslot.getValue())) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Input");
            alert.setHeaderText("Provider Not Available");
            alert.setContentText(prov.getProfile() + " is not available at slot " + ((Timeslot) t2_newTimeslot.getValue()));
            alert.setHeight(250);
            alert.showAndWait();
            return;
        }
        // finally check that patient is available at this new timeslot as well (dont wanna double book!)
        Patient patientInQuestion = checkPatientExists(patientFromInput);
        Visit ptr = patientInQuestion.getVisits();
        while(ptr != null) {
            if (ptr.getAppt().getDate().equals(new Date(t2_apptDate.getValue())) && ptr.getAppt().getTimeslot().equals((Timeslot) t2_newTimeslot.getValue())) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Input");
                alert.setHeaderText("Patient Not Available");
                alert.setContentText("Patient Already Has Appointment at new timeslot, " + t2_newTimeslot.getValue());
                alert.setHeight(250);
                alert.showAndWait();
                return;
            }
            ptr = ptr.getNext();
        }
        // now after all of that, we can finally rescchedule the appointment, changing it here also changes it in the visits linked list of the patient because they are same reference
        appointment.setTimeslot((Timeslot) t2_newTimeslot.getValue());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Successful Reschedule");
        alert.setHeaderText("Appointment Successfully Rescheduled");
        alert.setContentText("Appointment for " + appointment.getPatient() + " has been rescheduled to " + appointment.getDate() + " " + appointment.getTimeslot());
        alert.setHeight(250);
        alert.showAndWait();
        updateData();
        return;
    }
    @FXML
    /**
     * for when sort radio buttons are clicked
     * @param event of the sort radio button being clicked
     */
    void sortRadioButtonsClicked(ActionEvent event) {
        sortOutput.clear();
        if (sort_PA.isSelected()) {
            pa_pressed();
        } else if (sort_PP.isSelected()) {
            pp_pressed();
        } else if (sort_PL.isSelected()) {
            pl_pressed();
        } else if (sort_PO.isSelected()) {
            po_pressed();
        } else if (sort_PI.isSelected()) {
            pi_pressed();
        }
    }
    /**
     * for when clear button is pressed on the first tab
     * @param event of t1 clear button being pressed
     */
    @FXML
    void t1_clearInputPRESSED(ActionEvent event) {
        providerOrImaging.getSelectionModel().clearSelection();
        apptDate.getEditor().clear();
        fname.clear();
        lname.clear();
        dob.getEditor().clear();
        timeslot.getSelectionModel().clearSelection();
    }
    /**
     * for when clear button is pressed on the second tab
     * @param event of t2 clear button being pressed
     */
    @FXML
    void t2_clearInputPRESSED(ActionEvent event) {
        t2_apptDate.getEditor().clear();
        t2_fname.clear();
        t2_lname.clear();
        t2_dob.getEditor().clear();
        t2_timeslot.getSelectionModel().clearSelection();
        t2_newTimeslot.getSelectionModel().clearSelection();
    }
    /**
     * for when an appt is canceled, added, or rescheduled, it updates tab3-5 to make sure the data is accurate
     */
    private void updateData() {
        // FOR TAB 3 (update))
        sortOutput.clear();
        if (sort_PA.isSelected()) {
            pa_pressed();
        } else if (sort_PP.isSelected()) {
            pp_pressed();
        } else if (sort_PL.isSelected()) {
            pl_pressed();
        } else if (sort_PO.isSelected()) {
            po_pressed();
        } else if (sort_PI.isSelected()) {
            pi_pressed();
        }
        // FOR TAB 4 (update)
        t4_table.setItems(FXCollections.observableArrayList()); // this clears the data
        populateTab4();
        // FOR TAB 5 (update)
        t5_table.setItems(FXCollections.observableArrayList()); // this clears the data
        populateTab5();
    }
    /**
     * for updating data on tab 4
     */
    private void populateTab4() {
        ObservableList<ObservableList<Object>> data = FXCollections.observableArrayList();
        // make sure providers is sorted by name
        Sort.providers(allProviders);
        for (Provider p : allProviders) {
            ObservableList<Object> row = FXCollections.observableArrayList();
            // add providers full name and dob into first two cols
            row.add(p.getProfile().getFname() + " " + p.getProfile().getLname());
            row.add(p.getProfile().getDOB().toString());
            // calculate credit for this provider and add to row
            double totalCredit = 0;
            for (Appointment appt : allAppts) {
                if (appt.getProvider().equals(p)) {
                    totalCredit += ((Provider) appt.getProvider()).rate();
                }
            }
            row.add("$" + formatMoneyString(totalCredit));
            // add our new row to the data table observable list we made
            data.add(row);
        }
        // now set it as t4s table
        t4_table.setItems(data);
    }
    /**
     * populate tab5 patients and their billing statements
     */
    private void populateTab5() {
        ObservableList<ObservableList<Object>> data = FXCollections.observableArrayList();
        // make sure patients is sorted by name
        Sort.patients(allPatients);
        for (Patient p : allPatients) {
            ObservableList<Object> row = FXCollections.observableArrayList();
            // add patients full name and dob into first two cols
            row.add(p.getProfile().getFname() + " " + p.getProfile().getLname());
            row.add(p.getProfile().getDOB().toString());
            // calculate billing for this patient and add to row
            int totalCharge = 0;
            Visit ptr = p.getVisits();
            while (ptr != null) {
                totalCharge += ptr.getCharge();
                ptr = ptr.getNext();
            }
            row.add("$" + formatMoneyString(totalCharge));
            // add our new row to the data table observable list we made
            data.add(row);
        }
        // now set it as t5s table
        t5_table.setItems(data);
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
    private void loadProvidersList(File file) {
        try {
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
                    //System.out.println("Invalid provider type.");
                    return;
                }
            }
            //System.out.println("Providers loaded to the list.");
            scanner.close();
            // load provider list and add providers that are DOCTORS to choice drop down
            for (Provider prov : allProviders) {
                if (prov instanceof Doctor) {
                    this.providerOrImaging.getItems().add(prov.getProfile().getFname() + " " + prov.getProfile().getLname());
                }
            }
            // populate last 2 tabs now that we have our provider text chosen
            populateTab4();
            populateTab5();
            // hide file chooser button for providers and disable
            chooseProviderFileButton.setDisable(true);
            chooseProviderFileButton.setOpacity(0);
            // let user know successful file choose
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Successful File Upload");
            alert.setHeaderText("Provider File has been loaded in successfully.");
            alert.showAndWait();

        } catch (Exception e) {
            //System.out.println("An error occurred when trying to load in that file.");
            // let user know successful file choose
            providerOrImaging.getItems().clear();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("File Is Incompatible");
            alert.setHeaderText("File given is not a compatible list of providers.");
            alert.showAndWait();
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
    /**
     * helper method for print commands to check if schedule calendar is empty
     * @return true if the schedule calendar is empty, false otherwise
     */
    private boolean isScheduleCalendarEmpty() {
        if (this.allAppts.size() == 0) {
            sortOutput.setText("Schedule calendar is empty.");
            return true;
        }
        return false;
    }
    /**
     * Formats a number into the proper format including commas and periods "1,300.00"
     * @param amount int type to convert to format with two decimal places
     * @return String containing the amount in the proper format
     */
    private String formatMoneyString(double amount) {
        return String.format("%,.2f", amount);
    }
    /**
     * Helper method for printing appointments to view how they are currently sorted by key
     * @param key the ApptPrintTypes enum key of what to print
     */
    private void printAppts(ApptPrintTypes key) {
        // Print the sorted appointments using the custom iterator
        Iterator<Appointment> iterator = this.allAppts.iterator();
        while (iterator.hasNext()) {
            Appointment appt = iterator.next();
            if (appt != null) {
                if(key == ApptPrintTypes.ALL) {
                    sortOutput.setText(sortOutput.getText() + appt + "\n");
                } else if (key == ApptPrintTypes.IMAGING) {
                    if (appt instanceof Imaging) {
                        sortOutput.setText(sortOutput.getText() + appt + "\n");
                    }
                } else if (key == ApptPrintTypes.OFFICE) {
                    if (!(appt instanceof Imaging)) { // if not imaging type its office type
                        sortOutput.setText(sortOutput.getText() + appt + "\n");
                    }
                } else {
                    //System.out.println("!!! Error in key input for printing appointments in ClinicManager, method printAppts");
                }
            }
        }
    }
    /**
     * PA Prints the appointments in the List object ordered by date/timeslot, provider name
     */
    private void pa_pressed() {
        if(isScheduleCalendarEmpty()) {return;}
        List<SortKey> sortBy = new List<SortKey>();
        sortBy.add(SortKey.DATE);
        sortBy.add(SortKey.TIMESLOT);
        sortBy.add(SortKey.PROVIDER);
        sortOutput.setText("** List of appointments, ordered by date/time/provider.\n");
        Sort.appointment(this.allAppts, sortBy);
        printAppts(ApptPrintTypes.ALL);
        sortOutput.setText(sortOutput.getText() + "** end of list **");
    }
    /**
     * pp command prints the appointments ordered by patient
     * (by last name, first name, date of birth, then
     * appointment date and time).
     */
    private void pp_pressed() {
        if(isScheduleCalendarEmpty()) {return;}
        List<SortKey> sortBy = new List<SortKey>();
        sortBy.add(SortKey.PATIENT);
        sortBy.add(SortKey.DATE);
        sortBy.add(SortKey.TIMESLOT);
        sortOutput.setText("** Appointments ordered by patient/date/time **\n");
        Sort.appointment(this.allAppts, sortBy);
        printAppts(ApptPrintTypes.ALL);
        sortOutput.setText(sortOutput.getText() + "** end of list **");
    }
    /**
     * pl command prints the appointments ordered by county name, appt date and time
     */
    private void pl_pressed() {
        if(isScheduleCalendarEmpty()) {return;}
        List<SortKey> sortBy = new List<SortKey>();
        sortBy.add(SortKey.COUNTY);
        sortBy.add(SortKey.DATE);
        sortBy.add(SortKey.TIMESLOT);
        sortOutput.setText("** List of appointments, ordered by county/date/time.\n");
        Sort.appointment(this.allAppts, sortBy);
        printAppts(ApptPrintTypes.ALL);
        sortOutput.setText(sortOutput.getText() + "** end of list **");
    }
    /**
     * po command to display the list of office appointments,
     * sorted by the county name, then date and time.
     */
    private void po_pressed() {
        if(isScheduleCalendarEmpty()) {return;}
        List<SortKey> sortBy = new List<SortKey>();
        sortBy.add(SortKey.COUNTY);
        sortBy.add(SortKey.DATE);
        sortBy.add(SortKey.TIMESLOT);
        sortBy.add(SortKey.PROVIDER_FNAME); // if those all equal, then just sort by providers FIRSt name, didnt specify in directions, but shows in output
        sortOutput.setText("** List of office appointments ordered by county/date/time.\n");
        Sort.appointment(this.allAppts, sortBy);
        printAppts(ApptPrintTypes.OFFICE);
        sortOutput.setText(sortOutput.getText() + "** end of list **");
    }
    /**
     * pi command to display the list of imaging appointments,
     * sorted by the county name, then date and time.
     */
    private void pi_pressed() {
        if(isScheduleCalendarEmpty()) {return;}
        List<SortKey> sortBy = new List<SortKey>();
        sortBy.add(SortKey.COUNTY);
        sortBy.add(SortKey.DATE);
        sortBy.add(SortKey.TIMESLOT);
        sortBy.add(SortKey.PROVIDER_FNAME); // if those all equal, then just sort by providers FIRSt name, didnt specify in directions, but shows in output
        sortOutput.setText("** List of radiology appointments ordered by county/date/time.\n");
        Sort.appointment(this.allAppts, sortBy);
        printAppts(ApptPrintTypes.IMAGING);
        sortOutput.setText(sortOutput.getText() + "** end of list **");
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