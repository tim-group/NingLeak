package com.timgroup.ahcleak.client;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ning.http.client.ListenableFuture;

public class Requester extends Thread {

    private final SimpleClient client;
    private final String url;
    private final int requestCount;
    private final ReferenceQueue<ResponseHandler> queue;
    private final Set<Exception> exceptions;
    public int requestsMade;
    public int requestsCompleted;
    public final List<WeakReference<ResponseHandler>> handlers = new LinkedList<WeakReference<ResponseHandler>>();

    public Requester(SimpleClient client, String url, int requestCount, ReferenceQueue<ResponseHandler> queue, Set<Exception> exceptions) {
        this.client = client;
        this.url = url;
        this.requestCount = requestCount;
        this.queue = queue;
        this.exceptions = exceptions;
    }

    @Override
    public void run() {
        for (int i = 0; i < requestCount; ++i) {
            try {
                ResponseHandler handler = new ResponseHandler();
                handlers.add(new WeakReference<ResponseHandler>(handler, queue));
                ListenableFuture<String> future = client.get(url, handler);
                ++requestsMade;
                future.get();
                ++requestsCompleted;
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                exceptions.add(e);
                break;
            }
        }
    }

}
