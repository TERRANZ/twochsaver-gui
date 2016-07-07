package ru.terra.tsaver.gui;

import com.beust.jcommander.JCommander;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {
    public static void main(String... args) throws Exception {
        BasicConfigurator.configure();
        final StartupParameters parameters = StartupParameters.getInstance();
        new JCommander(parameters, args);
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        String fxmlFile = "/fxml/w_main.fxml";
        URL location = this.getClass().getResource(fxmlFile);
        Parent root = null;
        FXMLLoader fxmlLoader = new FXMLLoader();
        try {
            root = fxmlLoader.load(location.openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        stage.setScene(new Scene(root));
        stage.setTitle("Twoch downloader");
        stage.show();
    }
}
