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
import java.net.HttpURLConnection;
import java.net.URL;
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
        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        final ExecutorService threadPool = Executors.newFixedThreadPool(20);
                        final String url = tfUrl.getText();
                        final Integer thread = Integer.parseInt(url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")));
                        final String resUrl = url.substring(0, url.indexOf("/res"));
                        final String board = resUrl.substring(resUrl.lastIndexOf("/") + 1);
                        final String[] splitUrl = url.split("/");
                        final String domain = splitUrl[0] + "//" + splitUrl[2];
                        final String fullUrlBuild = domain + "/makaba/mobile.fcgi?task=get_thread&board=" + board + "&thread=" + thread + "&num=" + thread;

                        final URL uri = new URL(fullUrlBuild);
                        HttpURLConnection con = (HttpURLConnection) uri.openConnection();
                        con.setRequestMethod("GET");
                        con.setRequestProperty("Content-Type", "application/json");
                        con.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36");

                        final TwochThread[] readedThread = new ObjectMapper().readValue(con.getInputStream(), TwochThread[].class);
                        images = new ArrayList<>();
                        for (final TwochThread twochThread : readedThread) {
                            twochThread.getFiles().forEach(file -> {
                                images.add(domain + file.getPath());
                                final String finalImageUrl = new String(domain + file.getPath());
                                threadPool.submit(() -> {
                                    try {
                                        downloadImage(board + thread, finalImageUrl);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        showErrorDialog(e);
                                    }
                                });
                            });
                        }

                        Platform.runLater(() -> lblLog.setText("Images count: " + images.size()));
                        threadPool.shutdown();

                    } catch (Exception e) {
                        e.printStackTrace();
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
            final URL imageUrl = new URL(url);
            HttpURLConnection con = (HttpURLConnection) imageUrl.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36");
            final Path path = Paths.get(folder + url.substring(url.lastIndexOf("/")));
            if (!path.toFile().exists())
                Files.copy(con.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
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
        final DownloadService downloadService = new DownloadService();
        btnStart.disableProperty().bind(downloadService.runningProperty());
        downloadService.reset();
        downloadService.start();
    }

    private void showErrorDialog(Throwable throwable) {
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
