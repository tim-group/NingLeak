package com.timgroup.ahcleak.client;
import java.io.IOException;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.PerRequestConfig;

public class SimpleClient {

    private final AsyncHttpClient client;
    private final PerRequestConfig perRequestConfig;

    public SimpleClient(boolean allowPooling, int maximumConnectionsPerHost, int timeoutSeconds) {
        Builder config = new AsyncHttpClientConfig.Builder().setAllowPoolingConnection(allowPooling).setMaximumConnectionsPerHost(
                maximumConnectionsPerHost);
        client = new AsyncHttpClient(config.build());
        perRequestConfig = new PerRequestConfig(null, timeoutSeconds * 1000);
    }

    public <T> ListenableFuture<T> get(String url, AsyncHandler<T> handler) throws IOException {
        BoundRequestBuilder requestBuilder = client.prepareGet(url).setPerRequestConfig(perRequestConfig);

        return requestBuilder.execute(handler);
    }

    public void stop() {
        client.close();
    }

}
