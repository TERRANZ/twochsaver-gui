package ru.terra.tsaver.gui.engine;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.codehaus.jackson.map.ObjectMapper;
import ru.terra.tsaver.gui.MainController;
import ru.terra.tsaver.gui.dto.TwochThread;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.terra.tsaver.gui.engine.URLUtil.buildConnection;

public class DownloadService extends Service<Void> {
    private final MainController mainController;

    public DownloadService(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    final ExecutorService threadPool = Executors.newFixedThreadPool(20);
                    final String url = mainController.tfUrl.getText();
                    final int thread = Integer.parseInt(url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")));
                    final String resUrl = url.substring(0, url.indexOf("/res"));
                    final String board = resUrl.substring(resUrl.lastIndexOf("/") + 1);
                    final String[] splitUrl = url.split("/");
                    final String domain = splitUrl[0] + "//" + splitUrl[2];
                    final String fullUrlBuild = domain + "/makaba/mobile.fcgi?task=get_thread&board=" + board + "&thread=" + thread + "&num=" + thread;

                    final HttpURLConnection con = buildConnection(fullUrlBuild);

                    final TwochThread[] readedThread = new ObjectMapper().readValue(con.getInputStream(), TwochThread[].class);
                    mainController.setImages(new ArrayList<>());
                    for (final TwochThread twochThread : readedThread) {
                        twochThread.getFiles().forEach(file -> {
                            mainController.getImages().add(domain + file.getPath());
                            final String finalImageUrl = domain + file.getPath();
                            threadPool.submit(() -> {
                                try {
                                    downloadImage(board + thread, finalImageUrl);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    mainController.showErrorDialog(e);
                                }
                            });
                        });
                    }

                    Platform.runLater(() -> mainController.lblLog.setText("Images count: " + mainController.getImages().size()));
                    threadPool.shutdown();

                } catch (Exception e) {
                    e.printStackTrace();
                    mainController.showErrorDialog(e);
                }
                return null;
            }
        };
    }

    public void downloadImage(String folder, String url) throws IOException {
        new File(folder).mkdirs();
        for (int i = 0; i <= 2; i++) {
            HttpURLConnection con = buildConnection(url);
            final Path path = Paths.get(folder + url.substring(url.lastIndexOf("/")));
            if (!path.toFile().exists())
                Files.copy(con.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            break;
        }
        Platform.runLater(() -> {
            try {
                if (!mainController.getLvLog().getItems().contains(url))
                    mainController.getLvLog().getItems().add(url);
                synchronized (mainController.getImages()) {
                    mainController.getImages().remove(url);
                    mainController.getLblLog().setText("Images count: " + mainController.getImages().size());
                }
            } catch (Exception e) {
                mainController.showErrorDialog(e);
            }
        });
    }
}
