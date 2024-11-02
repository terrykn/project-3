package ruclinic;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Runs the application ClinicManagerController and initializes the GUI with start()
 * @author Terry Nguyen, Olivia Schroeder
 */
public class ClinicManagerMain extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClinicManagerMain.class.getResource("clinic-view.fxml"));
        //Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        Scene scene = new Scene(fxmlLoader.load(), 716, 544);
        stage.setTitle("RU CLINIC MANAGER");
        stage.setResizable(false); // dont want them to be able to resize window
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}