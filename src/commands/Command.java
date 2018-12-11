/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package commands;

/**
 *
 * @author Bryce
 */
public abstract class Command {
    
    private String label, description;
    
    public Command(String label, String description){
        this.label = label;
        this.description = description;
    }
    
    public abstract void execute(String command, String[] args);
    
    public void setDescription(String input){
        description = input;
    }
    
    public void setLabel(String input){
        label = input;
    }
    
    public String getDescription(){
        return description;
    }
    
    public String getLabel(){
        return label;
    }
    
}
