package com.timgroup.ahcleak.client;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.providers.netty.NettyAsyncHttpProvider;
import com.timgroup.ahcleak.Config;

public class RunClient {

    public static void main(String[] args) throws InterruptedException {
        int threadCount = 100;
        int requestCount = 1000;
        boolean haltOnLeak = false;
        boolean ignoreErrorLeak = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-t")) {
                threadCount = Integer.parseInt(args[++i]);
            } else if (arg.equals("-r")) {
                requestCount = Integer.parseInt(args[++i]);
            } else if (arg.equals("-h")) {
                haltOnLeak = true;
                ignoreErrorLeak = false;
            } else if (arg.equals("-H")) {
                haltOnLeak = true;
                ignoreErrorLeak = true;
            } else {
                throw new IllegalArgumentException("unrecognised argument: " + arg);
            }
        }

        main(threadCount, requestCount, haltOnLeak, ignoreErrorLeak);
    }

    public static void main(int threadCount, int requestCount, boolean haltOnLeak, boolean ignoreErrorLeak) throws InterruptedException {
        silence(LoggerFactory.getLogger(NettyAsyncHttpProvider.class));

        int timeoutSeconds = 1;

        SimpleClient client = new SimpleClient(true, threadCount * 2, timeoutSeconds);

        ReferenceQueue<ResponseHandler> queue = new ReferenceQueue<ResponseHandler>();
        Set<Exception> exceptions = Collections.synchronizedSet(new HashSet<Exception>());

        List<Requester> threads = new ArrayList<Requester>();
        for (int i = 0; i < threadCount; ++i) {
            threads.add(new Requester(client, Config.SERVER_URI.toString(), requestCount, queue, exceptions));
        }

        long startTime = System.currentTimeMillis();

        for (Requester thread : threads) {
            thread.start();
        }

        for (Requester thread : threads) {
            thread.join();
            if (!exceptions.isEmpty()) {
                interruptAll(threads);
            }
        }

        long endTime = System.currentTimeMillis();

        Thread.sleep((timeoutSeconds * 1000));
        System.gc();
        Thread.sleep(1000);

        int requestsMade = 0;
        int requestsCompleted = 0;
        int created = 0;
        for (Requester thread : threads) {
            requestsMade += thread.requestsMade;
            requestsCompleted += thread.requestsCompleted;
            created += thread.handlers.size();
        }

        int live = created - collect(queue).size();

        report(startTime, endTime, requestsMade, requestsCompleted, live, exceptions.size());

        if (haltOnLeak && live > 0 && (!ignoreErrorLeak || exceptions.isEmpty())) {
            System.out.println("halted");
            Thread.sleep(Long.MAX_VALUE);
        } else {
            client.stop();
        }
    }

    private static void silence(Logger logger) {
        if (!logger.getClass().getName().equals("org.slf4j.impl.SimpleLogger")) {
            throw new AssertionError(logger.getClass());
        }
        try {
            Field currentLogLevel = logger.getClass().getDeclaredField("currentLogLevel");
            currentLogLevel.setAccessible(true);
            currentLogLevel.set(logger, Integer.MAX_VALUE);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void interruptAll(List<Requester> threads) {
        for (Requester thread : threads) {
            thread.interrupt();
        }
    }

    private static <E> Set<Reference<? extends E>> collect(ReferenceQueue<E> queue) throws InterruptedException {
        Set<Reference<? extends E>> collected = new HashSet<Reference<? extends E>>();
        Reference<? extends E> c;
        while ((c = queue.poll()) != null) {
            collected.add(c);
        }
        return collected;
    }

    private static void report(long startTime, long endTime, int requestsMade, int requestsCompleted, int live, int numExceptions) {
        System.out.print("elapsed = " + (endTime - startTime) + ", ");
        System.out.print("made = " + requestsMade + ", ");
        System.out.print("completed = " + requestsCompleted + ", ");
        System.out.print("failed  = " + (requestsMade - requestsCompleted) + ", ");
        System.out.print("live = " + live + ", ");
        System.out.print("exceptions = " + numExceptions);
        System.out.println();
    }

}
