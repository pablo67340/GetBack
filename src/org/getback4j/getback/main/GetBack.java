package org.getback4j.getback.main;

import org.getback4j.getback.runnable.Logger;
import org.getback4j.getback.api.WebsitePluginLoader;
import org.getback4j.getback.commands.Command;
import org.getback4j.getback.commands.Disable;
import org.getback4j.getback.commands.Enable;
import org.getback4j.getback.commands.Help;
import org.getback4j.getback.commands.Plugins;
import org.getback4j.getback.commands.Quit;
import org.getback4j.getback.commands.Restart;

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

    /**
     * Contains the logger instance for GetBack
     */
    private final Logger LOGGER = new Logger();

    /**
     * Contains the base directory path
     */
    private String BASE;

    /**
     * Contains the config instance for GetBack
     */
    private Config config;

    /**
     * Contains the object instance of GetBack
     */
    private static GetBack INSTANCE;

    /**
     * Contains the versionfor GetBack
     */
    private final Double version = 1.1;

    /**
     * Contains the active commands for GetBack
     */
    private static final Map<String, Command> COMMANDS = new LinkedHashMap<>();

    /**
     * Contains the website plugin loader instance for GetBack
     */
    private final WebsitePluginLoader loader = new WebsitePluginLoader();

    /**
     * Gets the instance of the GetBack server.
     *
     * @return GetBack The instance of the running server.
     */
    public static GetBack getInstance() {
        return INSTANCE;
    }

    /**
     * A sub method of main that mounts the commands, saves defaults, and
     * launches the server
     *
     * @param args The arguments passed from main(String[] args).
     */
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
     * Constructs GetBack into an Object, runs with arguments
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        GetBack gb = new GetBack();
        gb.launch(args);
    }

    /**
     * Saves the default config.yml from inside the jar.
     *
     */
    public void saveDefaultConfig() {
        try {
            Yaml yaml;

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

    /**
     * Gets the logger for this instance of GetBack
     *
     * @return Logger The logger object
     */
    public Logger getLogger() {
        return LOGGER;
    }

    /**
     * Gets the config for this instance of GetBack
     *
     * @return Config The config object
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Gets the working directory for this instance of GetBack
     *
     * @return String The base path
     */
    public String getBaseDir() {
        return BASE;
    }

    /**
     * Runs a command executed by console
     *
     * @param command The command to be run
     * @param args The args included with the command
     */
    public void runCommand(String command, String[] args) {
        if (getCommands().containsKey(command)) {
            getCommands().get(command).execute(command, args);
        } else {
            getLogger().log("Command not found: " + command);
        }
    }

    /**
     * The the list of pre-made commands
     *
     * @return Map<String, Command> The list of available commands
     */
    public Map<String, Command> getCommands() {
        return COMMANDS;
    }

    /**
     * Adds a command into the registered GetBack commands
     *
     * @param label The command label
     * @param command The command Object
     */
    public void addCommand(String label, Command command) {
        COMMANDS.put(label, command);
    }

    /**
     * Gets the plugin loader for this instance of GetBack
     *
     * @return WebsitePluginLoader The loader object
     */
    public WebsitePluginLoader getWebsitePluginLoader() {
        return loader;
    }

    /**
     * Gets the current version of GetBack
     *
     * @return Double The version of GetBack
     */
    public Double getVersion() {
        return version;
    }
}
