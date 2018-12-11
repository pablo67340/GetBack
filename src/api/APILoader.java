/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api;

import getback.GetBack;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import java.util.Map;
import json.JSONObjects;

/**
 *
 * @author Bryce
 */
public class APILoader {

    private final Map<String, Class<?>> availableClasses;
    
    private final WebsitePlugin plugin;

    public APILoader(WebsitePlugin plugin) {
        availableClasses = new LinkedHashMap<>();
        this.plugin = plugin;
    }

    public Class<?> getAvailableClass(String name) {
        return availableClasses.get(name);
    }

    public Boolean hasClass(String className) {
        return availableClasses.containsKey(className);
    }

    public void registerAPIClass(Class<?> input) {
        int p = input.getName().lastIndexOf(".");
        String shortName = input.getName().substring(p + 1);

        availableClasses.put(input.getName(), input);
        plugin.addAPILoader(shortName, input.getName());
    }

    public void registerAPIClasses(Class<?>[] input) {
        for (Class<?> clas : input) {
            availableClasses.put(clas.getName(), clas);
        }
    }

    public void deRegisterAPIClass(Class<?> input) {
        availableClasses.remove(input.getName());
    }

    public String Invoke(String className, String method, String[] args) {
        try {
            if (hasClass(className)) {
                Class<?> c = getAvailableClass(className);
                String[] params = {};
                Method meth = c.getDeclaredMethod(method, params.getClass());
                // FIX: We need to cast args to Object to fix the String[] class argument error.
                Object object = meth.invoke(null, (Object) args);
                return (String) object;
            } else {
                GetBack.getInstance().getLogger().log("Error while invoking: " + className + ". Class not found. ");
                return JSONObjects.getStatusError("Error while invoking: " + className + ". Class not found. ");
            }
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
            GetBack.getInstance().getLogger().log("Error while invoking " + method + ": " + e.toString());
            return JSONObjects.getStatusError("Error while invoking: " + method + ": " + e.toString());
        }
    }

}
