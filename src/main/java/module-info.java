module com.example.clinic {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens ruclinic to javafx.fxml;
    exports ruclinic;
}