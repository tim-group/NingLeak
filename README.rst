What is it?
===========

This project demonstrates an apparent memory leak in the Ning AsyncHttpClient library.

How do i build it?
==================

With Gradle (http://www.gradle.org/). We are using version 1.7. To build, simply do::

    gradle clean install

How do i use it?
================

There are two executable components, a simple server and a test client.

To start the server, use Gradle::

    gradle runServer

To start the client (in another shell), either use Gradle::

    gradle run

Or run a standalone script::

    build/install/NingLeak/bin/run

If you run via the standalone script, you can give some flags:

-t THREAD-COUNT
    Use the given number of client threads (default 100)
-r REQUEST-COUNT
    Make the given number of requests in each client thread (default 1000)
-g GRACE-PERIOD
	The time to wait before declaring objects to be leaked (default 1 minute)
-h
    Halt if any objects are leaked
-H
    Halt if any objects are leaked, and there are no failed requests

Where is the leak?
==================

We believe there is a race between completion of an HTTP request and the enqueuing of a reaper for the request. If a request completes before the reaper is enqueued, the reaper will never terminate. The reaper has a reference to the request's ``AsyncHandler``, so this leads to a leak of user-supplied ``AsyncHandler`` implementations, as well as various objects internal to AsyncHttpClient. 

In AsyncHttpClient 1.6.3, the racing code is in ``com.ning.http.client.providers.netty.NettyAsyncHttpProvider``, between the passage concerning reapers starting on line 445, and the asynchronous consequences of the passage before it.

How can i see the leak?
=======================

Run the client until it reports a persistent leak. We have observed that under load (100+ client threads, or fewer threads on a loaded machine), a significant number of requests fail, and a significant number of objects will be leaked. Under less load, all requests are successful, and while leaks are less common and smaller, they nonetheless occur. 

If you use the -h/-H flag, the client will halt if there is a leak to allow you to investigate further. 

* Connect with a JDWP debugger on port 5005, follow stack frames down to ``RunClient::main(int, int, boolean, boolean)``, then and chase pointers from the ``client`` variable to the client's reaper queue. Despite the fact that all requests are finished, there are entries in this queue.

* Alternatively, connect with JVisualVM, take a heap dump, and search for instances of ``com.timgroup.ahcleak.client.ResponseHandler``, which is our ``AsyncHandler`` implementation. These are ultimately held on to by the reaper queue.
