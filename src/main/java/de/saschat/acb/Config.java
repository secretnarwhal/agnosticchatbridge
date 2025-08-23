package de.saschat.acb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class Config {
    @Expose
    public String token;
    @Expose
    public String channel;
    /**
     * May be set. Will be set automatically if token is allowed to create webhook for channel.
     */
    @Expose
    public String webhook;

    /**
     * Commandline to start server.
     */
    @Expose
    public String cmdline;

    @Expose
    public String startPattern = "Done";
    @Expose
    public String messagePattern = ": <(?<name>[^ ]{3,16})> (?<msg>.*)$";
    @Expose
    public String receiveTemplate = "tellraw @a [\"\",{text:\"[DC] \",color:\"dark_blue\"},{text:\"<%name%> %message%\",click_event:{action:\"suggest_command\",command:\"<@%id%> \"}}]";
    @Expose
    public String joinedPattern = ": (?<name>[^ ]{3,16}) joined the game$";
    @Expose
    public String leavePattern = ": (?<name>[^ ]{3,16}) left the game$";
    @Expose
    public String deathPattern = ": (?<name>[^ ]{3,16}) (?<action>[^:\\[]*)$";
    @Expose
    public String advancementPattern = ": (?<name>[^ ]{3,16}) has made the advancement \\[(?<advancement>.*)\\]$";
    @Expose
    public String mePattern = ": (?<text>\\* .*)$";

    private String path;

    private Config() {}

    public static Config load(String path) {
        try(FileInputStream str = new FileInputStream(Objects.requireNonNullElse(path, "config.json"))) {
            return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(new InputStreamReader(str), Config.class);
        } catch (Throwable ex) {
            Config config = new Config();
            config.path = path;
            config.dirty();
            return config;
        }
    }

    public void dirty() {
        try {
            FileOutputStream str = new FileOutputStream(Objects.requireNonNullElse(path, "config.json"));
            str.write(new GsonBuilder().excludeFieldsWithoutExposeAnnotation().serializeNulls().setPrettyPrinting().create().toJson(this).getBytes());
            str.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
