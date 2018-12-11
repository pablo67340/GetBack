/*
 * To change this license header, choose License Headers in Project Properties.
 */
package httpUtil;

/**
 *
 * @author Bryce
 */
import api.WebsitePlugin;
import getback.GetBack;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import org.apache.commons.lang.StringUtils;

/**
 *
 */
public class HttpMirror {

    public boolean isRunning = false;

    private WebsitePlugin parentPlugin;

    private ServerSocket socket;

    public HttpMirror(WebsitePlugin plugin) {
        parentPlugin = plugin;
    }

    public HttpMirror() {

    }

    public void run() {
        isRunning = true;

        // open server socket
        try {
            InetAddress addr = InetAddress.getByName(getPlugin().getConfig().getString("host"));
            Integer port = getPlugin().getConfig().getInt("port");
            socket = new ServerSocket(port, 0, addr);
            getPlugin().log("Accepting connections on " + addr.getHostAddress() + ":" + port);

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
                                getPlugin().getLogger().log(connection, request);
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
                                    if ((!req.contains(".") && getPlugin().isIndexForced()) || req.equalsIgnoreCase("/")) {
                                        req = getPlugin().getIndexFile();
                                    }

                                    if (getPlugin().getClassLoader().findResource(req) == null) {
                                        req = "404.html";

                                        File file2 = new File(getPlugin().getClassLoader().findResource(req).getFile());
                                        InputStream file = new FileInputStream(file2);

                                        pout.println("HTTP/1.1 404 File Not Found");
                                        pout.println("Server: GetBack "+GetBack.getInstance().getVersion());
                                        pout.println("Date: " + new Date());
                                        pout.println("Content-type: " + guessContentType(req));
                                        pout.println("Content-length: " + (int) file2.length());
                                        // Chrome will always stay loading unless we add this.
                                        pout.println("Connection: Closed");
                                        pout.println(); // blank line between headers and content, very important !
                                        pout.flush(); // flush character output stream buffer
                                        sendFile(file, out);
                                    } else {
                                        File f = new File(getPlugin().getClassLoader().findResource(req).getFile());

                                        if (f.isDirectory() && !req.endsWith("/")) {
                                            // redirect browser if referring to directory without final '/'
                                            pout.println("HTTP/1.1 301 Moved Permanently");
                                            pout.println("Location: http://" + connection.getLocalAddress().getHostAddress() + ":" + connection.getLocalPort() + "/" + req + "/");
                                            pout.println();
                                            pout.flush();
                                            if (GetBack.getInstance().getConfig().getVerbose()) {
                                                getPlugin().getLogger().log(connection, "301 Moved Permanently");
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
                                                    pout.println("Server: GetBack "+GetBack.getInstance().getVersion());
                                                    pout.println();
                                                    pout.flush();
                                                    pout.print(renderred);

                                                } catch (IOException e) {
                                                    System.out.println("Error: " + e.getMessage());
                                                }
                                            } else {
                                                // send file
                                                InputStream file = getPlugin().getResource(getPlugin().getClass(), req);
                                                pout.println("HTTP/1.1 200 OK");
                                                pout.println("Content-Type: " + guessContentType(req));
                                                pout.println("Date: " + new Date());
                                                pout.println("Server: GetBack "+GetBack.getInstance().getVersion());
                                                pout.println();
                                                pout.flush();
                                                sendFile(file, out); // send raw file 
                                                if (GetBack.getInstance().getConfig().getVerbose()) {
                                                    getPlugin().getLogger().log(connection, "200 OK");
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
                                    getPlugin().getLogger().log("POST Payload data is: " + payload);
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
                                        pout.println("Server: GetBack "+GetBack.getInstance().getVersion());
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
                                            getPlugin().getLogger().log("Targetting Class: " + className + " Method: " + methodName);
                                        }
                                        String fullClass = getPlugin().getAvailableAPILoaders().get(className);
                                        Invoker invoker = new Invoker();

                                        pout.print(invoker.Invoke(fullClass, methodName, params));
                                    }
                                }
                            }
                            out.flush();
                            connection.close();
                        }
                    } catch (IOException e) {

                        getPlugin().log(e.getMessage());

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
                getPlugin().log("Socket Closed");
            }
        }
    }

    private void errorReport(PrintStream pout, Socket connection, String code, String title, String msg) {
        pout.println("HTTP/1.1 " + code + " " + title);
        pout.println();
        pout.flush();
        pout.println("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">");
        pout.println("<TITLE>" + code + " " + title + "</TITLE>");
        pout.println("</HEAD><BODY>");
        pout.println("<H1>" + title + "</H1>\r\n" + msg + "<P>");
        pout.println("<HR><ADDRESS>GetBack "+GetBack.getInstance().getVersion()+" at " + connection.getLocalAddress().getHostName() + " Port " + connection.getLocalPort() + "</ADDRESS>");
        pout.println("</BODY></HTML>");
        pout.println();
        pout.flush();
        if (GetBack.getInstance().getConfig().getVerbose()) {
            getPlugin().getLogger().log(connection, code + " " + title);
        }
    }

    // This list gunna get bigger and bigger >.<
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

    public Boolean isRunning() {
        return isRunning;
    }

    public void setRunning(Boolean input) {
        isRunning = input;
    }

    public void stop() {
        isRunning = false;
        try {
            socket.close();
        } catch (IOException ex) {
            // We already catch up above;
        }
    }

    public void start() {
        run();
    }

    public WebsitePlugin getPlugin() {
        return parentPlugin;
    }

}
