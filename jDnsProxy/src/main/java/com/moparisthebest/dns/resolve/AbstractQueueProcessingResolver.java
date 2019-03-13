package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public abstract class AbstractQueueProcessingResolver implements QueueProcessingResolver {

    protected final int maxRetries;
    protected final String name;

    protected ExecutorService executor;
    protected BlockingQueue<RequestResponse> queue;
    private boolean running = false;
    private Thread thisThread = null;

    public AbstractQueueProcessingResolver(final int maxRetries, final String name) {
        this.maxRetries = maxRetries;
        this.name = name;
    }

    @Override
    public void start(final ExecutorService executor, final BlockingQueue<RequestResponse> queue) {
        this.executor = executor;
        this.queue = queue;
        this.running = true;
        executor.execute(this);
    }

    @Override
    public void run() {
        thisThread = Thread.currentThread();
        if (running)
            try {
                //System.err.println(name + " getting from queue");
                final RequestResponse requestResponse = queue.take();
                //System.err.println(name + " got from queue");
                Packet response = null;
                try {
                    response = resolve(requestResponse.getRequest());
                } catch (Exception e) {
                    //e.printStackTrace();
                    System.err.println("FAILURE: " + name + ": " + e.getMessage());
                }

                if(response == null) {
                    // failed
                    if (requestResponse.getAndIncrementFailureCount() < maxRetries) {
                        //System.err.println(name + " putting in queue");
                        queue.put(requestResponse);
                        //System.err.println(name + " put in queue");
                    } else {
                        //System.err.println(name + " maxRetries reached SRVFAIL");
                        @SuppressWarnings("unchecked") final CompletableFuture<RequestResponse> cf = (CompletableFuture<RequestResponse>) requestResponse.getCompletableFuture();
                        cf.completeExceptionally(new Exception("SRVFAIL"));
                    }
                } else {
                    requestResponse.setResponse(response);
                    //System.err.println(name + " got response: " + requestResponse.getResponse());
                    @SuppressWarnings("unchecked") final CompletableFuture<RequestResponse> cf = (CompletableFuture<RequestResponse>) requestResponse.getCompletableFuture();
                    //System.err.println(name + " completed: " + cf.complete(requestResponse));
                    cf.complete(requestResponse);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("socketresolver take", e);
            } finally {
                if (running)
                    executor.execute(this);
            }
    }

    @Override
    public void close() {
        running = false;
        if (thisThread != null)
            thisThread.interrupt();
    }
}
