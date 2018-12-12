package org.getback4j.getback.api;

import java.util.LinkedHashMap;

import java.util.Map;

/**
 *
 * @author Bryce
 */
public class APILoader {

    /**
     * Contains the active classes ready to be used by this
     * {@link WebsitePlugin}'s {@link HttpMirror} instance.
     *
     */
    private final Map<String, Class<?>> availableClasses;

    /**
     * Stores the instance of the plugin using this APILoader.
     */
    private final WebsitePlugin plugin;

    /**
     * Instantiates available class map required to function, and stores this
     * {@link WebsitePlugin}'s instance.
     *
     * @param plugin The instance of the plugin utilizing this APILoader.
     */
    public APILoader(WebsitePlugin plugin) {
        availableClasses = new LinkedHashMap<>();
        this.plugin = plugin;
    }

    /**
     * Instantiates available class map required to function, and stores this
     * {@link WebsitePlugin}'s instance.
     *
     * @param name The name of the class the api is looking to target
     * @return Class<?> The class itself.
     */
    public Class<?> getAvailableClass(String name) {
        return availableClasses.get(name);
    }

    /**
     * True/False if the specified class is available for loading.
     *
     * @param className The name of the class the api is checking exists.
     * @return Boolean If the class is available for load.
     */
    public Boolean hasClass(String className) {
        return availableClasses.containsKey(className);
    }

    /**
     * Registers a single class by its formal name to become available for
     * loading.
     *
     * @param input The class to become available.
     */
    public void registerAPIClass(Class<?> input) {
        int p = input.getName().lastIndexOf(".");
        String shortName = input.getName().substring(p + 1);

        availableClasses.put(input.getName(), input);
        plugin.addAPILoader(shortName, input.getName());
    }

    /**
     * Registers multiple classes by their formal names to become available for
     * loading.
     *
     * @param input The class to become available.
     */
    public void registerAPIClasses(Class<?>[] input) {
        for (Class<?> clas : input) {
            availableClasses.put(clas.getName(), clas);
        }
    }

    /**
     * De-Registers a single class by its formal name to no longer be available
     * for loading.
     *
     * @param input The class to become available.
     */
    public void deRegisterAPIClass(Class<?> input) {
        availableClasses.remove(input.getName());
    }

}
