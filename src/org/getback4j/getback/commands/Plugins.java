/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.getback4j.getback.commands;

import org.getback4j.getback.api.WebsitePlugin;
import org.getback4j.getback.main.GetBack;
import org.getback4j.getback.runnable.Utilities;
import java.util.Map.Entry;

/**
 *
 * @author Bryce
 */
public class Plugins extends Command {

    public Plugins() {
        super("plugins", "Lists the currently running plugins on the server.");
    }

    @Override
    public void execute(String command, String[] args) {
        System.out.println("       All plugins       ");
        System.out.println("-------------------------");
        Integer index = 0;

        for (Entry<String, WebsitePlugin> entry : GetBack.getInstance().getWebsitePluginLoader().getWebsitePlugins().entrySet()) {
            index += 1;
            System.out.println(index + ". " + entry.getKey() + " (" + entry.getValue().getPrettyPluginStatus() + ")");
        }
    }
}
