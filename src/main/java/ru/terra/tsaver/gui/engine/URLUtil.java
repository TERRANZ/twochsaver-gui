package ru.terra.tsaver.gui.engine;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class URLUtil {
    public static HttpURLConnection buildConnection(final String fullUrlBuild) throws IOException {
        final URL uri = new URL(fullUrlBuild);
        final HttpURLConnection con = (HttpURLConnection) uri.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36");

        return con;
    }
}
