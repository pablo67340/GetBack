/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.getback4j.getback.commands;

import org.getback4j.getback.main.GetBack;

/**
 *
 * @author Bryce
 */
public class Help extends Command{
    
    public Help(){
        super("help", "Displays a list of available commands.");
    }

    @Override
    public void execute(String command, String[] args) {
        GetBack.getInstance().getCommands().entrySet().forEach((entry) -> {
            GetBack.getInstance().getLogger().log(entry.getKey()+": "+entry.getValue().getDescription());
        });
    }
    
}
