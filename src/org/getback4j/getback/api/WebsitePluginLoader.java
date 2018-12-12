package org.getback4j.getback.api;

import org.getback4j.getback.configuration.InvalidConfigurationException;
import org.getback4j.getback.configuration.file.FileConfiguration;
import org.getback4j.getback.configuration.file.YamlConfiguration;
import org.getback4j.getback.main.GetBack;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author Bryce
 */
public class WebsitePluginLoader {

    /**
     * Maps containing active and inactive plugins however inactivePlugins is
     * currently not being used this setup will be changed very soon.
     */
    private final Map<String, WebsitePlugin> activePlugins, inactivePlugins;

    /**
     * Instantiates the active & inactive plugin maps needed to function
     */
    public WebsitePluginLoader() {
        activePlugins = inactivePlugins = new HashMap<>();
    }

    /**
     * Starts the {@Link WebsitePlugin} loading process for the server. This
     * will iterate through the 'plugins' folder checking each jar file for a
     * 'plugin.yml', if present, and correctly formatted, the plugin will be
     * loaded using details in this file later on in loadFile(String jar).
     *
     * @see {@Link WebsitePlugin}
     */
    public void loadPlugins() {
        File plugins = new File(GetBack.getInstance().getBaseDir() + "/plugins");

        plugins.mkdirs();
        FilenameFilter filter = (File dir, String name) -> name.toLowerCase().endsWith(".jar");
        File[] listFiles = plugins.listFiles(filter);

        if (listFiles.length == 0) {
            GetBack.getInstance().getLogger().log("No Website plugins are installed. No websites will run off this server without plugins!");
            return;
        }

        for (File plugin : listFiles) {
            loadFile(plugin);
        }
    }

    /**
     * Loads individual jar files using a specified path found in the
     * loadPlugins() iteration, to have its plugin.yml read for further
     * information.
     *
     * @param jar An absolute URL giving the direct location to the jar
     */
    public void loadFile(File jar) {
        try {
            URLClassLoader loader = (URLClassLoader) GetBack.getInstance().getClass().getClassLoader();

            URL url = jar.toURI().toURL();

            if (new HashSet<>(Arrays.asList(loader.getURLs())).contains(url)) {
                return;
            }

            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);

            method.setAccessible(true);
            method.invoke(loader, url);

            InputStream in;
            URL inputURL;
            String inputFile = "jar:file:/" + jar.getPath() + "!/plugin.yml";
            if (inputFile.startsWith("jar:")) {

                inputURL = new URL(inputFile);
                JarURLConnection conn = (JarURLConnection) inputURL.openConnection();
                in = conn.getInputStream();
                FileConfiguration plugin;

                plugin = new YamlConfiguration();
                try {
                    plugin.load(in);
                } catch (IOException | InvalidConfigurationException e) {
                    GetBack.getInstance().getLogger().log("Error loading " + jar.getName() + "'s plugin.yml: " + e.getMessage());
                }
                String name = plugin.getString("name");
                String mainClass = plugin.getString("main");
                Boolean forceIndex = plugin.getBoolean("forceIndex");
                String indexFile = plugin.getString("indexFile");
                loadPlugin(mainClass, name, jar.getPath(), forceIndex, indexFile);
            }

        } catch (IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            GetBack.getInstance().getLogger().log("Error loading plugin: " + e.getMessage());
        }
    }

    /**
     * Directly loads the plugins main class and creates a new instance to which
     * we can set object specific details like name, path, index, etc. All
     * parameters must be specified in the plugin.yml
     *
     * @param mainClass An absolute URL of the main class location
     * (org.testplugin.main.Main)
     * @param name Name of the plugin.
     * @param path The direct URL path to the jar file.
     * @param forceIndex Force send index file before anything else (Angular,
     * React).
     * @param indexFile Index file for the later create {@link HttpMirror}.
     * @see {@link WebsitePlugin}
     */
    public void loadPlugin(String mainClass, String name, String path, Boolean forceIndex, String indexFile) {
        WebsitePlugin plugin = null;
        try {
            Class<?> clazz = Class.forName(mainClass);
            plugin = (WebsitePlugin) clazz.newInstance();
            plugin.setPluginStatus(PluginStatus.INITIALIZING);
            plugin.init(name, path, forceIndex, indexFile);
            GetBack.getInstance().getLogger().log(plugin, "Loading website...");
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            GetBack.getInstance().getLogger().log("Error loading plugin class: " + e.getMessage());
        }

        enablePlugin(name, plugin, mainClass);
    }

    /**
     * Sets the plugin's status to Running, and marks plugin enabled where
     * needed.
     *
     * @param name The name of the plugin.
     * @param plugin The instance of the plugin to be enabled/cached.
     * @param clazz The main class of the plugin.
     */
    public void enablePlugin(String name, WebsitePlugin plugin, String clazz) {
        if (plugin != null) {
            plugin.onEnable();
            activePlugins.put(name, plugin);
            GetBack.getInstance().getLogger().log(plugin, "Enabled");
            plugin.startWebServer();
            plugin.setPluginStatus(PluginStatus.RUNNING);
        } else {
            GetBack.getInstance().getLogger().log("Error loading plugin class: " + clazz + ". Class not found.");
        }

    }

    /**
     * Returns the list of currently active plugins.
     *
     * @return The {@link Map} containing the active plugins by name.
     * @see {@link WebsitePlugin}
     */
    public Map<String, WebsitePlugin> getWebsitePlugins() {
        return activePlugins;
    }

    /**
     * Force invokes onDisable inside the specified plugin.
     *
     * @param plugin The instance of the plugin to be disabled
     */
    public void disablePlugin(WebsitePlugin plugin) {
        plugin.onDisable();
    }
}
