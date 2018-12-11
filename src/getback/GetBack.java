/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getback;

import api.WebsitePluginLoader;
import commands.Command;
import commands.Disable;
import commands.Enable;
import commands.Help;
import commands.Plugins;
import commands.Quit;
import commands.Restart;
import data.SQLSaver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Bryce
 */
public class GetBack {

    Yaml yaml;
    Config config;
    private final Logger LOGGER = new Logger();

    private String BASE;

    private static GetBack INSTANCE;
    
    private Double version = 1.0;

    private static final Map<String, Command> COMMANDS = new LinkedHashMap<>();

    private final WebsitePluginLoader loader = new WebsitePluginLoader();

    public static GetBack getInstance() {
        return INSTANCE;
    }

    public void launch(String[] args) {
        getLogger().log("OS NAME: " + System.getProperty("os.name"));
        addCommand("help", new Help());
        addCommand("quit", new Quit());
        addCommand("restart", new Restart());
        addCommand("plugins", new Plugins());
        addCommand("disable", new Disable());
        addCommand("enable", new Enable());

        BASE = System.getProperty("user.dir");

        saveDefaultConfig();
        INSTANCE = this;
        loader.loadPlugins();

        Scanner scan = new Scanner(System.in);
        while (true) {
            String line = scan.nextLine();
            String line2 = line.toLowerCase();
            String command = line2.split(" ")[0];
            String[] arg = line.replace(command + " ", "").split(" ");
            runCommand(command, arg);
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        GetBack gb = new GetBack();
        gb.launch(args);
    }

    public void saveDefaultConfig() {
        try {
            File modules = new File(BASE + "/plugins");
            modules.mkdirs();

            File file = new File(BASE + "/config.yml");
            if (!file.exists()) {
                InputStream link = getClass().getClassLoader().getResourceAsStream("config.yml");
                Files.copy(link, file.getAbsoluteFile().toPath());

                getLogger().log("This is a first run. Extracting default files to: " + BASE);
            }
            FileInputStream inputStream = new FileInputStream(file);
            yaml = new Yaml(new org.yaml.snakeyaml.constructor.Constructor(Config.class));
            config = yaml.load(inputStream);
            getLogger().log("Configuration loaded");
            getLogger().log("Mounted base directory: " + BASE);
        } catch (IOException e) {
            getLogger().log("Error creating default files: " + e.getMessage());
        }
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public Config getConfig() {
        return config;
    }

    public String getBaseDir() {
        return BASE;
    }

    public void runCommand(String command, String[] args) {
        if (getCommands().containsKey(command)) {
            getCommands().get(command).execute(command, args);
        } else {
            getLogger().log("Command not found: " + command);
        }
    }

    public Map<String, Command> getCommands() {
        return COMMANDS;
    }

    public void addCommand(String label, Command command) {
        COMMANDS.put(label, command);
    }

    public WebsitePluginLoader getWebsitePluginLoader() {
        return loader;
    }
    
    public Double getVersion(){
        return version;
    }
}
