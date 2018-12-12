/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.getback4j.getback.commands;

import org.getback4j.getback.api.WebsitePlugin;
import org.getback4j.getback.main.GetBack;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Bryce
 */
public class Enable extends Command {

    public Enable() {
        super("enable", "Enables a website plugin.");
    }

    @Override
    public void execute(String command, String[] args) {
        if (args.length == 0) {
            System.out.println("Incorrect usage: enable {name/id}");
        }
        
        if (StringUtils.isNumeric(args[0])) {
            int stop = Integer.parseInt(args[0]);
            int index = 0;
            for (WebsitePlugin plugin : GetBack.getInstance().getWebsitePluginLoader().getWebsitePlugins().values()){
                index += 1;
                if (index == stop){
                    plugin.startPlugin();
                }
            }
        } else {
            WebsitePlugin plugin = GetBack.getInstance().getWebsitePluginLoader().getWebsitePlugins().get(args[0]);
            plugin.startPlugin();
        }
    }

}
