/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api;

import configuration.InvalidConfigurationException;
import configuration.file.FileConfiguration;
import configuration.file.YamlConfiguration;
import getback.GetBack;

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

    public Map<String, WebsitePlugin> activePlugins, inactivePlugins;

    public WebsitePluginLoader() {
        activePlugins = inactivePlugins = new HashMap<>();
    }

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

    public Map<String, WebsitePlugin> getWebsitePlugins() {
        return activePlugins;
    }

    public void disablePlugin(String name, WebsitePlugin plugin) {
        plugin.onDisable();
    }
}
