package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

public class QueueProcessingResolver extends WrappingResolver implements Runnable {

    private final ExecutorService executor;
    private final BlockingQueue<RequestCompletableFuture> queue;

    private boolean running = false;
    private Thread thisThread = null;

    public QueueProcessingResolver(final Resolver delegate, final ExecutorService executor, final BlockingQueue<RequestCompletableFuture> queue) {
        super(delegate);
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
                final RequestCompletableFuture cf = queue.take();
                //System.err.println(name + " got from queue");
                Packet response = null;
                Throwable resolveException = null;
                try {
                    response = resolve(cf.getRequest());
                } catch (Throwable e) {
                    //e.printStackTrace();
                    //System.err.println("FAILURE: " + name + ": " + e.getMessage());
                    resolveException = e;
                }
                //resolveAsync(requestResponse, executor).get();

                if(response == null) {
                    // failed
                    cf.completeExceptionally(resolveException == null ? new Exception("SRVFAIL") : resolveException);
                } else {
                    //System.err.println(name + " got response: " + response);
                    //System.err.println(name + " completed: " + cf.complete(response));
                    cf.complete(response);
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
