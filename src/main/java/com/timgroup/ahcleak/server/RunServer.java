package com.timgroup.ahcleak.server;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;

import com.timgroup.ahcleak.Config;

public class RunServer {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        SimpleServer server = new SimpleServer(Config.SERVER_URI.getPort(), Config.SERVER_URI.getPath(), "text/plain", UTF8, "hello Ning");
        server.start();
        System.out.println("Server started: " + server);
    }

}
