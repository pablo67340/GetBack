/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getback;

import api.WebsitePlugin;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Bryce
 */
public class Logger {
    
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public Logger(){
        
    }

    public void log(Socket connection, String msg) {
        System.err.println("[" + sdf.format(new Date()) + "] [" + connection.getInetAddress().getHostAddress()
                + ":" + connection.getPort() + "] " + msg);
    }

    public void log(String msg) {
        
        System.err.println("[" + sdf.format(new Date()) + "] " + msg);
    }
     
    
    public void log(WebsitePlugin plugin, String msg){
        System.err.println("[" + sdf.format(new Date()) + "] " + "["+plugin.getName()+"] "+msg);
    }
}
