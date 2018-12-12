package org.getback4j.getback.runnable;

import org.getback4j.getback.api.WebsitePlugin;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Bryce
 */
public class Logger {

    /**
     * Contains the format we want our console to use.
     *
     * TODO: Configurable?
     */
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Logger() {

    }

    /**
     * Logs to console on behalf of the socket connection
     *
     * @param connection The socket that will be logging
     * @param msg The message coming from the socket
     */
    public void log(Socket connection, String msg) {
        System.err.println("[" + sdf.format(new Date()) + "] [" + connection.getInetAddress().getHostAddress()
                + ":" + connection.getPort() + "] " + msg);
    }

    /**
     * Generically logs to console
     *
     * @param msg The message to be logged
     */
    public void log(String msg) {

        System.err.println("[" + sdf.format(new Date()) + "] " + msg);
    }

    /**
     * Logs to console on behalf of the plugin
     *
     * @param plugin The website plugin that will be logging
     * @param msg The message coming from the plugin
     */
    public void log(WebsitePlugin plugin, String msg) {
        System.err.println("[" + sdf.format(new Date()) + "] " + "[" + plugin.getName() + "] " + msg);
    }
}
