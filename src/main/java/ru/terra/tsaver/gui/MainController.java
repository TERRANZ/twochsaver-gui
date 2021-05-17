package ru.terra.tsaver.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import ru.terra.tsaver.gui.engine.DownloadService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Properties;
import java.util.ResourceBundle;

import static ru.terra.tsaver.gui.engine.URLUtil.buildConnection;

public class MainController implements Initializable {
    @FXML
    public Label lblLog;
    @FXML
    public ListView<String> lvLog;
    @FXML
    public TextField tfUrl;
    @FXML
    public Button btnStart;
    @FXML
    public CheckBox cbAutoUpdate;
    private ArrayList<String> images;
    private Properties defaultProps;
    private Properties applicationProps;
    private String folder = "";

    public ListView<String> getLvLog() {
        return lvLog;
    }

    public Label getLblLog() {
        return lblLog;
    }

    public ArrayList<String> getImages() {
        return images;
    }

    public void setImages(ArrayList<String> images) {
        this.images = images;
    }

    private void loadProps() throws IOException {
        Properties defaultProps = new Properties();
        FileInputStream in = new FileInputStream("settings");
        defaultProps.load(in);
        in.close();

        applicationProps = new Properties(defaultProps);

        in = new FileInputStream("appProperties");
        applicationProps.load(in);
        in.close();
    }

    private void saveProps() throws IOException {
        FileOutputStream out = new FileOutputStream("appProperties");
        applicationProps.store(out, "---No Comment---");
        out.close();
    }

    public void openSettings(ActionEvent actionEvent) {

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    public void start(ActionEvent actionEvent) {
        download();
    }

    private void download() {
        final DownloadService downloadService = new DownloadService(this);
        btnStart.disableProperty().bind(downloadService.runningProperty());
        downloadService.reset();
        downloadService.start();
    }

    public void showErrorDialog(Throwable throwable) {
        final Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exception Dialog");
        alert.setHeaderText("Exception");
        alert.setContentText(throwable.getMessage());

        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String exceptionText = sw.toString();

        final Label label = new Label("The exception stacktrace was:");

        final TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        final GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }
}
