package edu.escuelaing.arem.ASE.app;

import java.awt.*;
import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpServer {

    private static String currentQuery = null;
    private static HttpServer _instance = new HttpServer();
    private static String route = "";
    private static Map<String, WebService> services = new HashMap<String, WebService>();


    private HttpServer(){}

    public static HttpServer getInstance(){
        return _instance;
    }

    public void runServer(String[] args) throws IOException, URISyntaxException {

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(35000);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 35000.");
            System.exit(1);
        }

        boolean running = true;
        while (running) {
            Socket clientSocket = null;
            try {
                System.out.println("Listo para recibir ...");
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            clientSocket.getInputStream()));
            String inputLine, outputLine;
            outputLine = null;

            boolean firstLine = true;
            String uriStr = "";

            while ((inputLine = in.readLine()) != null) {
                if(firstLine){
                    uriStr = inputLine.split(" ")[1];
                    firstLine = false;
                }
                System.out.println("Received: " + inputLine);
                if (!in.ready()) {
                    break;
                }
            }

            URI fileUri = new URI(uriStr);

            String path = fileUri.getPath();
            if (path.startsWith("/action")){
                String webUri = path.replace("/action", "");
                if (services.containsKey(webUri)) {
                // QUitar query
                    outputLine = services.get(webUri).handle();
                }
            } else if (uriStr.startsWith("/movie?t=")){
                // If client is asking for a movie
                currentQuery = fileUri.getQuery();
                outputLine = httpRequestTextFiles("/movieInfo.html");
            } else if (uriStr.startsWith("/movieData")) {
                // When client asks for the movie´s information to complete the table
                outputLine = ExternalRestApiConnection.movieDataService(currentQuery);

                if (outputLine == null) {
                    outputLine = httpErrorNotFound();
                }

            } else if (uriStr.startsWith("/jpeg")){
                // When client asks for an image
                OutputStream outputForImage = clientSocket.getOutputStream();
                httpRequestImage(fileUri.getPath(), outputForImage);
                outputLine = null;

            } else {
                // When client asks for a text file
                outputLine = httpErrorNotFound();

                try{
                    outputLine = httpRequestTextFiles(fileUri.getPath());
                } catch(Exception e){
                    e.printStackTrace();
                }
            }

            out.println(outputLine);
            out.close();
            in.close();
            clientSocket.close();
        }
        serverSocket.close();
    }

    /**
     * When a file asked is not found
     * @return outputLine to send
     * @throws IOException
     */
    private static String httpErrorNotFound() throws IOException {
        String outputLine = "HTTP/1.1 404 Not Found\r\n"
                + "Content-Type:text/html\r\n"
                + "\r\n";
        Charset charset = Charset.forName("UTF-8");
        Path file = Paths.get("target/classes/public/notFound.html");
        BufferedReader reader = Files.newBufferedReader(file, charset);
        String line = null;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            outputLine = outputLine + line + "\r\n";;
        }
        return outputLine;

    }

    /**
     * Looks for an image file and returns it
     * @param requestedFile the image requested
     * @param outputStream  the stream where the image is going to be sent
     * @throws IOException
     */
    public static void httpRequestImage(String requestedFile, OutputStream outputStream) throws IOException {
        Path file = Paths.get("target/classes/public" + requestedFile);
        byte[] buffer = new byte[1024]; // Tamaño del buffer
        try (InputStream inputStream = Files.newInputStream(file)) {
            String header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type:image/jpeg\r\n" +
                    "Content-Length: " + Files.size(file) + "\r\n" +
                    "\r\n";
            outputStream.write(header.getBytes()); // Envía los encabezados
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead); // Escribir el buffer al OutputStream
            }
        }
    }

    /**
     * Looks for the text file requested
     * @param requestedFile the text file requested
     * @return outputLine the String that shows what the client requested
     * @throws IOException
     */
    public static String httpRequestTextFiles(String requestedFile) throws IOException {
        Charset charset = Charset.forName("UTF-8");
        Path file = Paths.get("target/classes/public" + requestedFile);
        BufferedReader reader = Files.newBufferedReader(file, charset);
        String line = null;
        String outputLine = "HTTP/1.1 200 OK\r\n";
        String extension = requestedFile.split("\\.")[1];

        if (extension.equals("html")) {
           outputLine = outputLine + "Content-Type:text/html; charset=utf-8\r\n";
        } else if (extension.equals("js")) {
            outputLine = outputLine + "Content-Type:application/javascript; charset=utf-8\r\n";
        } else if (extension.equals(("css"))){
            outputLine = outputLine + "Content-Type:text/css; charset=utf-8\r\n";
        }

        outputLine = outputLine + "\r\n";

        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            outputLine = outputLine + line + "\r\n";;
        }

        return outputLine;

    }

    public static void get(String r, WebService s) {
        services.put(r, s);
    }


}