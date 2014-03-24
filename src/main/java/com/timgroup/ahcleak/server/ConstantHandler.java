package com.timgroup.ahcleak.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ConstantHandler implements HttpHandler {
    private static final int STATUS_OK = 200;

    private final String contentTypeHeader;
    private final byte[] bytes;

    public ConstantHandler(Charset charset, String contentType, String body) {
        contentTypeHeader = contentType + ";charset=" + charset.name();
        bytes = body.getBytes(charset);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentTypeHeader);
        exchange.sendResponseHeaders(ConstantHandler.STATUS_OK, 0);
        OutputStream responseBody = exchange.getResponseBody();
        try {
            responseBody.write(bytes);
        } finally {
            responseBody.close();
        }
    }

}
