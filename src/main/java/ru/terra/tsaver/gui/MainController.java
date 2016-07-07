package ru.terra.tsaver.gui;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.terra.tsaver.gui.dto.TwochThread;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private class DownloadService extends Service<Void> {
        private Logger logger = LoggerFactory.getLogger(this.getClass());

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        ExecutorService threadPool = Executors.newFixedThreadPool(20);
                        String url = tfUrl.getText();
                        Integer thread = Integer.parseInt(url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")));
                        String resUrl = url.substring(0, url.indexOf("/res"));
                        String board = resUrl.substring(resUrl.lastIndexOf("/") + 1);
                        URLConnection conn = new URL("https://2ch.hk/makaba/mobile.fcgi?task=get_thread&board=" + board + "&thread=" + thread + "&num=" + thread).openConnection();
                        conn.setConnectTimeout(10000);
                        ObjectMapper mapper = new ObjectMapper();
                        TwochThread[] readedThread = mapper.readValue(conn.getInputStream(), TwochThread[].class);
                        images = new ArrayList<>();
                        final String finalResUrl = resUrl + "/";
                        for (TwochThread twochThread : readedThread) {
                            twochThread.getFiles().forEach(file -> {
                                images.add(finalResUrl + file.getPath());
                                final String finalImageUrl = new String(finalResUrl + file.getPath());
                                threadPool.submit(() -> {
                                    try {
                                        downloadImage(board + thread, finalImageUrl);
                                    } catch (Exception e) {
                                        showErrorDialog(e);
                                    }
                                });
                            });
                        }

                        Platform.runLater(() -> lblLog.setText("Images count: " + images.size()));
                        threadPool.shutdown();

                    } catch (Exception e) {
                        showErrorDialog(e);
                    }
                    return null;
                }
            };
        }
    }

    private void downloadImage(String folder, String url) throws IOException {
        new File(folder).mkdirs();
        for (int i = 0; i <= 2; i++) {
            URL imageUrl = new URL(url);
            Path path = Paths.get(folder + url.substring(url.lastIndexOf("/")));
            if (!path.toFile().exists())
                Files.copy(imageUrl.openStream(), path, StandardCopyOption.REPLACE_EXISTING);
            break;
        }
        Platform.runLater(() -> {
            try {
                if (!lvLog.getItems().contains(url))
                    lvLog.getItems().add(url);
                synchronized (images) {
                    images.remove(url);
                    lblLog.setText("Images count: " + images.size());
                }
            } catch (Exception e) {
                showErrorDialog(e);
            }
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    public void start(ActionEvent actionEvent) {
        download();
    }

    private void download() {
        DownloadService downloadService = new DownloadService();
        btnStart.disableProperty().bind(downloadService.runningProperty());
        downloadService.reset();
        downloadService.start();
    }

    private void showErrorDialog(Throwable throwable) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exception Dialog");
        alert.setHeaderText("Exception");
        alert.setContentText(throwable.getMessage());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }
}
