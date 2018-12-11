/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httpUtil;

import getback.GetBack;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import json.JSONObjects;

/**
 *
 * @author Bryce
 */
public class Invoker {

    public Invoker() {

    }

    public String Invoke(String className, String method, String[] args) {
        try {
            Class<?> c = Class.forName(className);
            Method meth = c.getDeclaredMethod(method, String[].class);
            // FIX: We need to cast args to Object to fix the String[] class argument error.
            Object object = meth.invoke(null, (Object) args);
            return (String) object;
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
            if (GetBack.getInstance().getConfig().getVerbose()) {
                GetBack.getInstance().getLogger().log("Error while invoking " + method + ": " + e.toString());
            }
            return JSONObjects.getStatusError("Error while invoking: " + method + ": " + e.toString());
        }
    }

}
