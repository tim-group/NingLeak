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
        int gracePeriod = 60 * 1000;
        boolean haltOnLeak = false;
        boolean ignoreErrorLeak = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-t")) {
                threadCount = Integer.parseInt(args[++i]);
            } else if (arg.equals("-r")) {
                requestCount = Integer.parseInt(args[++i]);
            } else if (arg.equals("-g")) {
                gracePeriod = Integer.parseInt(args[++i]);
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

        main(threadCount, requestCount, gracePeriod, haltOnLeak, ignoreErrorLeak);
    }

    public static void main(int threadCount, int requestCount, int gracePeriod, boolean haltOnLeak, boolean ignoreErrorLeak) throws InterruptedException {
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

        int requestsMade = 0;
        int requestsCompleted = 0;
        int created = 0;
        for (Requester thread : threads) {
            requestsMade += thread.requestsMade;
            requestsCompleted += thread.requestsCompleted;
            created += thread.handlers.size();
        }

        Set<Reference<? extends ResponseHandler>> collected = new HashSet<Reference<? extends ResponseHandler>>();

        Thread.sleep((timeoutSeconds * 1000) + 500);

        long endTime = System.currentTimeMillis();

        drainAndReport(queue, collected, exceptions, startTime, endTime, requestsMade, requestsCompleted, created);

        long now;
        while ((created - collected.size()) > 0 && (now = System.currentTimeMillis()) < endTime + gracePeriod) {
            Thread.sleep(10 * 1000);
            drainAndReport(queue, collected, exceptions, startTime, now, requestsMade, requestsCompleted, created);
        }

        if (created > collected.size()) {
            System.out.println("leak!");
            if (haltOnLeak && (!ignoreErrorLeak || exceptions.isEmpty())) {
                Thread.sleep(Integer.MAX_VALUE);
            }
        }
        client.stop();
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

    private static void drainAndReport(ReferenceQueue<ResponseHandler> queue, Set<Reference<? extends ResponseHandler>> collected, Set<Exception> exceptions, long startTime, long now, int requestsMade, int requestsCompleted, int created) throws InterruptedException {
        System.gc();
        Thread.sleep(1000);
        drain(queue, collected);
        System.out.println(String.format("elapsed = %d, made = %d, completed = %d, failed = %d, exceptions = %d, live = %d",
                                         now - startTime,
                                         requestsMade,
                                         requestsCompleted,
                                         requestsMade - requestsCompleted,
                                         exceptions.size(),
                                         created - collected.size()));
    }

    private static <E> Set<Reference<? extends E>> drain(ReferenceQueue<E> queue, Set<Reference<? extends E>> collected) {
        Reference<? extends E> c;
        while ((c = queue.poll()) != null) {
            collected.add(c);
        }
        return collected;
    }

}
