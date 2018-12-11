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
import getback.Logger;
import getback.Utilities;
import httpUtil.HttpMirror;
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
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author Bryce
 */
public abstract class WebsitePlugin {

    private Yaml yaml;

    private String name, description;

    private APILoader apiLoader = null;

    private FileConfiguration config;

    private File configFile;

    private final Map<String, String> apiLoaders = new LinkedHashMap<>();

    private String jarPath;

    private final Logger logger = GetBack.getInstance().getLogger();

    private URLClassLoader classLoader;

    private final HttpMirror mirror;
    
    private boolean forceIndex = false;
    
    private String indexFile;

    private PluginStatus status = PluginStatus.DISABLED;

    public WebsitePlugin() {
        this.apiLoader = new APILoader(this);
        mirror = new HttpMirror(this);
    }

    public void startWebServer() {
        Thread thread1 = new Thread(() -> {
            mirror.run();
        });
        thread1.start();
    }

    public void init(String name, String path, Boolean forceIndex, String indexFile) {
        this.name = name;
        this.jarPath = path;
        this.forceIndex = forceIndex;
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

    public APILoader getAPILoader() {
        return this.apiLoader;
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public void saveDefaultConfig(Class clazz) {
        this.configFile = new File(getDataFolder(), "config.yml");
        if (!this.configFile.exists()) {
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
            return null;
        }
    }

    public String getDataFolder() {
        return GetBack.getInstance().getBaseDir() + "/plugins/" + getName();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String input) {
        this.name = input;
    }

    public abstract void onEnable();

    public void onDisable() {
        setPluginStatus(PluginStatus.DISABLED);
    }

    public URLClassLoader getClassLoader() {
        return classLoader;
    }

    public void setJarPath(String input) {
        jarPath = input;
    }

    public String getJarPath() {
        return jarPath;
    }

    public Logger getLogger() {
        return logger;
    }

    public Map<String, String> getAvailableAPILoaders() {
        return apiLoaders;
    }

    public void addAPILoader(String shortName, String fullName) {
        apiLoaders.put(shortName, fullName);
    }

    public void log(String message) {
        getLogger().log(this, message);
    }

    public void setPluginStatus(PluginStatus input) {
        status = input;
    }

    public PluginStatus getPluginStatus() {
        return status;
    }

    public String getPrettyPluginStatus() {
        String stat = Utilities.capitailizeWord(status.toString().toLowerCase());

        switch (status) {
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

    public HttpMirror getMirror() {
        return mirror;
    }

    public void disablePlugin() {
        if (status == PluginStatus.RUNNING) {
            onDisable();
            mirror.stop();
            setPluginStatus(PluginStatus.DISABLED);
        } else if (status == PluginStatus.DISABLED) {
            getLogger().log("Plugin already running");
        }
    }

    public void startPlugin() {
        if (status == PluginStatus.DISABLED) {
            onEnable();
            mirror.run();
            setPluginStatus(PluginStatus.RUNNING);
        } else if (status == PluginStatus.RUNNING) {
            getLogger().log("Plugin already running");
        }

    }
    
    public boolean isIndexForced(){
        return forceIndex;
    }
    
    public String getIndexFile(){
        return indexFile;
    }
    
    public void setIndexFile(String input){
        indexFile = input;
    }
}
