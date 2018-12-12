package org.getback4j.getback.api;

import org.getback4j.getback.configuration.InvalidConfigurationException;
import org.getback4j.getback.configuration.file.FileConfiguration;
import org.getback4j.getback.configuration.file.YamlConfiguration;
import org.getback4j.getback.main.GetBack;
import org.getback4j.getback.runnable.Logger;
import org.getback4j.getback.runnable.Utilities;
import org.getback4j.getback.http.HttpMirror;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Bryce
 */
public abstract class WebsitePlugin {

    /**
     * Contains the name, description, and jar location of the current instance
     * of {@Link WebsitePlugin}.
     */
    @Getter
    @Setter
    private String name, description, jarPath;

    /**
     * Contains the APILoader for the of the current instance of
     * {@Link WebsitePlugin}.
     */
    @Getter
    private APILoader apiLoader = null;

    /**
     * Contains the FileConfiguration instance for the of the current instance
     * of {@Link WebsitePlugin}.
     */
    @Getter
    private FileConfiguration config;

    /**
     * Contains a map of short classnames to full classpaths only this
     * {@Link WebsitePlugin} will be able to use.
     */
    @Getter
    private final Map<String, String> apiNameMap = new LinkedHashMap<>();

    /**
     * Contains the Logger instance for the of the current instance of
     * {@Link WebsitePlugin}.
     */
    @Getter
    private final Logger logger = GetBack.getInstance().getLogger();

    /**
     * Contains the URLClassLoader instance for the of the current instance of
     * {@Link WebsitePlugin}.
     */
    @Getter
    private URLClassLoader classLoader;

    /**
     * Contains the HttpMirror instance for the of the current instance of
     * {@Link WebsitePlugin}.
     */
    @Getter
    private final HttpMirror mirror;

    /**
     * Contains true/false if the index file is forced.
     */
    @Getter
    private boolean isIndexForced = false;

    /**
     * Contains the index path for the of the current instance of
     * {@Link WebsitePlugin}.
     */
    @Getter
    @Setter
    private String indexFile;

    /**
     * Contains the PluginStatus for the of the current instance of
     * {@Link WebsitePlugin}.
     */
    @Getter
    @Setter
    private PluginStatus pluginStatus = PluginStatus.DISABLED;

    public WebsitePlugin() {
        this.apiLoader = new APILoader(this);
        mirror = new HttpMirror(this);
    }

    // GETTERS //
    /**
     * Returns the specified resource from an external file using a class anchor
     * point.
     *
     * @param clazz The {@link WebsitePlugin}'s main class (usually)
     * @param filename The file to be returned in a stream
     * @return InputStream The stream of the file
     */
    public InputStream getResource(Class clazz, String filename) {
        if (filename == null) {
            GetBack.getInstance().getLogger().log("Error getting resource: Filename cannot be null.");
        }

        try {
            URL url = classLoader.findResource(filename);

            if (url == null) {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            log("Error getting resource: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Returns prettified string displaying the current plugin instance's
     * PluginStatus
     *
     * @return String The pretty, colored string you all love <3
     */
    public String getPrettyPluginStatus() {
        String stat = Utilities.capitailizeWord(getPluginStatus().toString().toLowerCase());

        switch (getPluginStatus()) {
            case DISABLED:
                return Utilities.ANSI_RED + stat + Utilities.ANSI_RESET;
            case RUNNING:
                return Utilities.ANSI_GREEN + stat + Utilities.ANSI_RESET;
            case INITIALIZING:
                return Utilities.ANSI_YELLOW + stat + Utilities.ANSI_RESET;
            default:
                return stat;
        }

    }

    /**
     * Returns the folder of the plugin containing the config.yml
     *
     * @return String The path to your plugin's config directory.
     */
    public String getDataFolder() {
        return GetBack.getInstance().getBaseDir() + "/plugins/" + getName();
    }
    // END GETTERS //

    // VOIDS //
    /**
     * Runs the (hopefully) instantiated HttpMirror
     *
     */
    public void startWebServer() {
        Thread thread1 = new Thread(() -> {
            mirror.run();
        });
        thread1.start();
    }

    /**
     * Initializes the plugin with its information loaded from various spots.
     *
     * @param name The name of the plugin
     * @param path The path of the plugin's jar
     * @param forceIndex If the index should be sent first
     * @param indexFile The path to the index file
     */
    public void init(String name, String path, Boolean forceIndex, String indexFile) {
        this.name = name;
        this.jarPath = path;
        this.isIndexForced = forceIndex;
        this.indexFile = indexFile;

        URL[] urls = new URL[1];
        try {
            File jar = new File(jarPath);
            urls[0] = jar.toURI().toURL();
        } catch (MalformedURLException e) {
            GetBack.getInstance().getLogger().log("Couldnt find JAR URL: " + e.getMessage());
        }
        classLoader = new URLClassLoader(urls);

    }

    /**
     * Saves the default config of the plugin
     *
     * @param clazz The anchor point class to find resources from
     */
    public void saveDefaultConfig(Class clazz) {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            saveResource(clazz, "config.yml", false);
        }

        this.config = new YamlConfiguration();
        try {
            this.config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            GetBack.getInstance().getLogger().log("Error loading default config.yml. Error: " + e.getMessage());
        }
    }

    /**
     * Saves a resource from a specified class anchor point to a directory
     *
     * @param clazz The anchor point class to find resources from
     * @param resourcePath The path of the resource about to be saved
     * @param replace Replace the file if already exists
     */
    public void saveResource(Class clazz, String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            GetBack.getInstance().getLogger().log("ResourcePath cannot be null or empty");
        } else {
            resourcePath = resourcePath.replace('\\', '/');
            InputStream in = getResource(clazz, resourcePath);
            if (in == null) {
                GetBack.getInstance().getLogger().log("The embedded resource '" + resourcePath + "' cannot be found in " + name);
            } else {
                File outFile = new File(getDataFolder(), resourcePath);
                int lastIndex = resourcePath.lastIndexOf('/');
                File outDir = new File(getDataFolder(), resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));

                if (!outDir.exists()) {
                    outDir.mkdirs();
                }

                try {
                    if (!outFile.exists() || replace) {
                        try (OutputStream out = new FileOutputStream(outFile)) {
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                        }
                        in.close();
                    } else {
                        GetBack.getInstance().getLogger().log("Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
                    }
                } catch (IOException ex) {
                    GetBack.getInstance().getLogger().log("Could not save " + outFile.getName() + " to " + outFile);
                }
            }
        }
    }

    /**
     * Adds a full classpath to a class shortname
     *
     * @param shortName The short name of the API class
     * @param fullName The full classpath of the API class.
     */
    public void addAPILoader(String shortName, String fullName) {
        getApiNameMap().put(shortName, fullName);
    }

    /**
     * Logs to the GetBack output on behalf of the plugin
     *
     */
    public void log(String message) {
        getLogger().log(this, message);
    }

    /**
     * Disables the plugin, stops the HttpMirror Socket.
     *
     */
    public void disablePlugin() {
        if (getPluginStatus() == PluginStatus.RUNNING) {
            onDisable();
            mirror.stop();
            setPluginStatus(PluginStatus.DISABLED);
        } else if (getPluginStatus() == PluginStatus.DISABLED) {
            getLogger().log("Plugin already running");
        }
    }

    /**
     * Starts the plugin, launches a new Socket inside HttpMirror
     *
     */
    public void startPlugin() {
        if (getPluginStatus() == PluginStatus.DISABLED) {
            onEnable();
            mirror.run();
            setPluginStatus(PluginStatus.RUNNING);
        } else if (getPluginStatus() == PluginStatus.RUNNING) {
            getLogger().log("Plugin already running");
        }

    }

    /**
     * This method will run when the plugin is enabled and ready to run.
     *
     */
    public abstract void onEnable();

    /**
     * This method will run when the plugin is disabled and no longer running
     */
    public void onDisable() {
        setPluginStatus(PluginStatus.DISABLED);
    }

}
