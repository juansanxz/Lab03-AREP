package edu.escuelaing.arem.ASE.app;

import java.io.IOException;
import java.net.URISyntaxException;

public class MyWebServices {
    public static void main(String[] args) throws IOException, URISyntaxException {
        HttpServer.get("/arep", (req) -> {
            String resp = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type:text/html\r\n" +
                    "\r\n" +
                    "Hello Arep.";
            return resp;
        });
        HttpServer.getInstance().runServer(args);
    }
}
