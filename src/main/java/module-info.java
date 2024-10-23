module com.example.clinic {
    requires javafx.controls;
    requires javafx.fxml;


    opens ruclinic to javafx.fxml;
    exports ruclinic;
}