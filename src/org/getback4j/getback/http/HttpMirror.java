package org.getback4j.getback.http;

/**
 *
 * @author Bryce
 */
import org.getback4j.getback.api.WebsitePlugin;
import org.getback4j.getback.main.GetBack;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.getback4j.getback.json.JSONObjects;

/**
 *
 */
public class HttpMirror {

    /**
     * Contains true/false if this mirror is running
     *
     */
    @Getter
    @Setter
    private boolean isRunning = false;

    /**
     * Contains the instance of the parent plugin
     *
     */
    @Getter
    private final WebsitePlugin parentPlugin;

    /**
     * Contains the socket instance we will be connecting too
     *
     */
    @Getter
    private ServerSocket socket;

    /**
     * Instantiates this mirror from a plugin, on a per plugin basis.
     *
     * @param plugin The instance of the plugin launching this HttpMirror
     */
    public HttpMirror(WebsitePlugin plugin) {
        parentPlugin = plugin;
    }

    /**
     * Runs the "HTTP Interpreter.
     *
     * This method will read the HTTP/1.1 request coming in, digest the target
     * directory OR digest the POST payload and either send a file, or
     * JSONResponse.
     *
     * Note: PHP Emulation has been lightly tested here, by manually executing
     * php-cgi followed with a php script. This KIND of worked with a bunch of
     * bugs for things like not being able to use requireOnce() or require() as
     * the working directory (or something along those lines) with other
     * variables not being set, that something like Apache would normally set
     * for you. I would like to come back to this as it would be hugely
     * beneficial to be able to run your old beloved PHP based sites!
     *
     * Note: To add support for AngularJS and ReactJS, a ForceIndex variable had
     * to be introduced as the Javascript file itself is in charge of routing
     * the client. We can add support by simply force sending the index.html or
     * index.whatever to the client first, which (should) contain the Javascript
     * file nnecessary for routing the client.
     *
     * Note: This is basically just a huge string shredder, so we need to find
     * the most efficient ways to splice/edit those stringy bois.
     *
     * Note: During an instance of 404, the default goto file is 404.html inside
     * your web directory. If you are using Angular or React, this can be easily
     * overriden to use a custom page/url.
     *
     * Note: TODO: Without something like Angular/React, I will be adding a
     * variable setting to the plugin.yml setting within the next few commits,
     * that will allow you to point to a 404 file of your own, or any error for
     * that matter.
     */
    public void run() {
        isRunning = true;

        // open server socket
        try {
            InetAddress addr = InetAddress.getByName(getParentPlugin().getConfig().getString("host"));
            Integer port = getParentPlugin().getConfig().getInt("port");
            socket = new ServerSocket(port, 0, addr);
            getParentPlugin().log("Accepting connections on " + addr.getHostAddress() + ":" + port);

            // request handler loop
            while (isRunning) {
                Socket connection;

                // wait for request
                connection = socket.accept();

                Thread thread = new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                        PrintStream pout = new PrintStream(out);

                        // read first line of request (ignore the rest)
                        String request = in.readLine();
                        if (request != null) {
                            if (GetBack.getInstance().getConfig().getVerbose()) {
                                getParentPlugin().getLogger().log(connection, request);
                            }
                            while (true) {
                                String misc = in.readLine();
                                if (misc == null || misc.length() == 0) {
                                    break;
                                }
                            }

                            if (request.startsWith("GET")) {
                                String req = request.substring(4, request.length() - 9).trim();
                                if (req.contains("..") || req.contains("/.ht") || req.endsWith("~")) {
                                    // Hecker tryna get into the parent of wwwhome or beyond
                                    errorReport(pout, connection, "403", "Forbidden", "You don't have permission to access the requested URL.");
                                } else {

                                    req = req.substring(1);
                                    if (req.contains("?c") || req.contains("?v")) {
                                        req = StringUtils.substringBefore(req, "?c");
                                        req = StringUtils.substringBefore(req, "?v=");
                                    }

                                    // To handle angular routes, we need to serve the html file before everything else!
                                    if ((!req.contains(".") && getParentPlugin().isIndexForced()) || req.equalsIgnoreCase("/")) {
                                        req = getParentPlugin().getIndexFile();
                                    }

                                    if (getParentPlugin().getClassLoader().findResource(req) == null) {
                                        req = "404.html";

                                        File file2 = new File(getParentPlugin().getClassLoader().findResource(req).getFile());
                                        InputStream file = new FileInputStream(file2);

                                        pout.println("HTTP/1.1 404 File Not Found");
                                        pout.println("Server: GetBack " + GetBack.getInstance().getVersion());
                                        pout.println("Date: " + new Date());
                                        pout.println("Content-type: " + guessContentType(req));
                                        pout.println("Content-length: " + (int) file2.length());
                                        // Chrome will always stay loading unless we add this.
                                        pout.println("Connection: Closed");
                                        pout.println(); // blank line between headers and content, very important !
                                        pout.flush(); // flush character output stream buffer
                                        sendFile(file, out);
                                    } else {
                                        File f = new File(getParentPlugin().getClassLoader().findResource(req).getFile());

                                        if (f.isDirectory() && !req.endsWith("/")) {
                                            // redirect browser if referring to directory without final '/'
                                            pout.println("HTTP/1.1 301 Moved Permanently");
                                            pout.println("Location: http://" + connection.getLocalAddress().getHostAddress() + ":" + connection.getLocalPort() + "/" + req + "/");
                                            pout.println();
                                            pout.flush();
                                            if (GetBack.getInstance().getConfig().getVerbose()) {
                                                getParentPlugin().getLogger().log(connection, "301 Moved Permanently");
                                            }
                                        } else {
                                            if (req.contains(".php")) {
                                                try {
                                                    req = "C:/" + req;

                                                    ProcessBuilder pb = new ProcessBuilder(new String[]{"cmd", "/c", "php-cgi " + req});
                                                    File phpmyadmin = new File("C:/phpmyadmin");

                                                    pb.directory(phpmyadmin);
                                                    Process p = pb.start();

                                                    String line;
                                                    String renderred;
                                                    try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                                                        renderred = "";
                                                        while ((line = input.readLine()) != null) {
                                                            System.out.println(line);
                                                            renderred += line;

                                                        }
                                                    }

                                                    pout.println("HTTP/1.1 200 OK");
                                                    pout.println("Content-Type: " + guessContentType(req));
                                                    pout.println("Date: " + new Date());
                                                    pout.println("Content-Type: text/html");
                                                    pout.println("Server: GetBack " + GetBack.getInstance().getVersion());
                                                    pout.println();
                                                    pout.flush();
                                                    pout.print(renderred);

                                                } catch (IOException e) {
                                                    System.out.println("Error: " + e.getMessage());
                                                }
                                            } else {
                                                // send file
                                                InputStream file = getParentPlugin().getResource(getParentPlugin().getClass(), req);
                                                pout.println("HTTP/1.1 200 OK");
                                                pout.println("Content-Type: " + guessContentType(req));
                                                pout.println("Date: " + new Date());
                                                pout.println("Server: GetBack " + GetBack.getInstance().getVersion());
                                                pout.println();
                                                pout.flush();
                                                sendFile(file, out); // send raw file 
                                                if (GetBack.getInstance().getConfig().getVerbose()) {
                                                    getParentPlugin().getLogger().log(connection, "200 OK");
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (request.startsWith("POST")) {

                                //code to read the post payload data
                                StringBuilder payloadBuilder = new StringBuilder();
                                while (in.ready()) {
                                    payloadBuilder.append((char) in.read());
                                }
                                String payload = payloadBuilder.toString();
                                // TODO: TODO

                                // Notice the payload below cuts out commas and switches for &, we need to only replace
                                // the commas on the data keys, not the data values.
                                // Handle the payload as POST parameters are sent on the next line after a blank space. Destroy JSON and force into
                                // paramfiy.
                                payload = "?" + payload.replace("\"", "").replace(":", "=").replace("{", "").replace("}", "").replace(",", "&");
                                if (GetBack.getInstance().getConfig().getVerbose()) {
                                    getParentPlugin().getLogger().log("POST Payload data is: " + payload);
                                }
                                String req = request.substring(4, request.length() - 9).trim();
                                if (req.contains("..") || req.contains("/.ht") || req.endsWith("~")) {
                                    // Hecker tryna get into the parent of wwwhome or beyond
                                    errorReport(pout, connection, "403", "Forbidden", "You don't have permission to access the requested URL.");
                                } else {
                                    if (req.contains("/api/")) {
                                        pout.println("HTTP/1.1 200 OK");
                                        pout.println("Content-Type: " + guessContentType(".json"));
                                        pout.println("Date: " + new Date());
                                        pout.println("Server: GetBack " + GetBack.getInstance().getVersion());
                                        pout.println();
                                        pout.flush();
                                        String data, className, methodName;
                                        String[] params;

                                        data = req.replace("/api/", "");

                                        className = StringUtils.substringBefore(data, "/");
                                        methodName = StringUtils.substringAfter(data, "/").replace("/", "_");
                                        methodName = methodName.substring(0, methodName.length() - 1);

                                        String preParams = StringUtils.substringAfter(payload, "?");
                                        methodName = StringUtils.substringBefore(methodName, "?");
                                        params = preParams.split("&");
                                        if (GetBack.getInstance().getConfig().getVerbose()) {
                                            getParentPlugin().getLogger().log("Targetting Class: " + className + " Method: " + methodName);
                                        }
                                        String fullClass = getParentPlugin().getApiNameMap().get(className);

                                        pout.print(invoke(fullClass, methodName, params));
                                    }
                                }
                            }
                            out.flush();
                            connection.close();
                        }
                    } catch (IOException e) {
                        getParentPlugin().log(e.getMessage());

                    }
                });
                // Give the JVM the permission to halt all threads on command, no waiting allowed!
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            if (!e.getMessage().contains("socket closed")) {
                System.err.println(e.getMessage());
            } else {
                getParentPlugin().log("Socket Closed");
            }
        }
    }

    /**
     * Runs when an error occurs during logic flow, this will force send an
     * error page to the client, with the error and information. This will also
     * log to GetBack.
     *
     * @param pout The stream used to communicate with the client
     * @param connection The connection associated with the stream
     * @param code The error code of the error encountered
     * @param title The title of the error stack
     * @param msg The message of the full stack
     */
    private void errorReport(PrintStream pout, Socket connection, String code, String title, String msg) {
        pout.println("HTTP/1.1 " + code + " " + title);
        pout.println();
        pout.flush();
        pout.println("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
        pout.println("<TITLE>" + code + " " + title + "</TITLE>");
        pout.println("</HEAD><BODY>");
        pout.println("<H1>" + title + "</H1>\r\n" + msg + "<P>");
        pout.println("<HR><ADDRESS>GetBack " + GetBack.getInstance().getVersion() + " at " + connection.getLocalAddress().getHostName() + " Port " + connection.getLocalPort() + "</ADDRESS>");
        pout.println("</BODY></HTML>");
        pout.println();
        pout.flush();
        if (GetBack.getInstance().getConfig().getVerbose()) {
            getParentPlugin().getLogger().log(connection, code + " " + title);
        }
    }

    /**
     * Determines the content type we are about to send to the client This is
     * required to declare the MIME type of the content being sent.
     *
     * @param path The file name of the file e.g logo.png
     */
    private static String guessContentType(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm")) {
            return "text/html";
        } else if (path.endsWith(".txt") || path.endsWith(".java") || path.endsWith(".json")) {
            return "text/plain";
        } else if (path.endsWith(".gif")) {
            return "image/gif";
        } else if (path.endsWith(".class")) {
            return "application/octet-stream";
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (path.endsWith(".css")) {
            return "text/css";
        } else if (path.endsWith(".png")) {
            return "image/png";
        } else if (path.endsWith(".woff")) {
            return "font/woff";
        } else if (path.endsWith(".woff2")) {
            return "font/woff2";
        } else {
            return "text/plain";
        }
    }

    /**
     * Reads the file into a buffer which is then sent to the client using the
     * ouput stream.
     *
     * @param file The absolute file URL to be sent to the client
     * @param out The output stream to be used to stream the file
     */
    private static void sendFile(InputStream file, OutputStream out) {
        try {
            byte[] buffer = new byte[1000];
            while (file.available() > 0) {
                out.write(buffer, 0, file.read(buffer));
            }
        } catch (IOException e) {
            System.err.println("IO EXCEPTION: " + e);
        }
    }

    /**
     * Force sets mirror to disabled, and kills socket communication
     *
     */
    public void stop() {
        isRunning = false;
        try {
            socket.close();
        } catch (IOException ex) {
            getParentPlugin().log("Error closing socket: " + ex.getMessage());
        }
    }

    /**
     * Starts the mirror and opens the Socket connection
     *
     */
    public void start() {
        run();
    }

    /**
     * Invokes a method inside of an API class declared in each plugin
     *
     * @param className The short class name to be invoked
     * @param method The method name inside the class to be targetted
     * @param args The arguments to be sent into the targetted method.
     *
     * @return JSON The response generated by the API method
     *
     */
    public String invoke(String className, String method, String[] args) {
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
