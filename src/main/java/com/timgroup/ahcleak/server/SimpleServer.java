package com.timgroup.ahcleak.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class SimpleServer {

    private final HttpServer server;

    public SimpleServer(int port, String path, String contentType, Charset charset, String responseBody) throws IOException {
        HttpHandler handler = new ConstantHandler(charset, contentType, responseBody);
        server = createServer(port, path, handler);
    }

    private HttpServer createServer(int port, String path, HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(path, handler);
        server.setExecutor(Executors.newCachedThreadPool());
        return server;
    }

    public void start() throws InterruptedException {
        server.start();
    }

    @Override
    public String toString() {
        return server.getAddress().toString();
    }

}
