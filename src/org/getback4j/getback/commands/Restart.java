/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.getback4j.getback.commands;

import org.getback4j.getback.main.GetBack;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 *
 * @author Bryce
 */
public class Restart extends Command {
    
    public Restart() {
        super("restart", "Gracefully resarts the WebServer after saving.");
    }
    
    @Override
    public void execute(String command, String[] args) {
        restartApplication();
    }
    
    public void restartApplication() {
        try {
            final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            final File currentJar = new File(GetBack.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            /* is it a jar file? */
            if (!currentJar.getName().endsWith(".jar")) {
                return;
            }

            /* Build command: java -jar application.jar */
            final ArrayList<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("-jar");
            command.add(currentJar.getPath());
            
            final ProcessBuilder builder = new ProcessBuilder(command);
            builder.start();
            System.exit(0);
        } catch (IOException | URISyntaxException e) {
            GetBack.getInstance().getLogger().log("Error restarting program: " + e.getMessage());
        }
    }
    
}
