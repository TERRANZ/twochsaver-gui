package ru.terra.tsaver.gui;

import com.beust.jcommander.Parameter;

public class StartupParameters {
    @Parameter(names = {"-u", "--url"}, description = "Url")
    private String url;
    @Parameter(names = {"-n", "--nogui"}, description = "No gui")
    private Boolean noGui = false;

    private static StartupParameters instance = new StartupParameters();

    private StartupParameters() {
    }

    public static StartupParameters getInstance() {
        return instance;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getNoGui() {
        return noGui;
    }

    public void setNoGui(Boolean noGui) {
        this.noGui = noGui;
    }
}