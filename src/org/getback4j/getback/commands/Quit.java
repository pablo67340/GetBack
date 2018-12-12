/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.getback4j.getback.commands;

/**
 *
 * @author Bryce
 */
public class Quit extends Command{
    
    public Quit(){
        super("quit", "Gracefully shuts down the WebServer after saving.");
    }
    
    @Override
    public void execute(String label, String[] args){
        System.out.println("Saving...");
        System.exit(1);
    }
    
}
