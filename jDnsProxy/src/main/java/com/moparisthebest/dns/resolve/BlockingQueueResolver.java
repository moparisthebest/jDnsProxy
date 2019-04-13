package com.moparisthebest.dns.resolve;

import java.util.concurrent.*;

public class BlockingQueueResolver implements Resolver {

    private final BlockingQueue<RequestResponse> queue;

    public BlockingQueueResolver(final BlockingQueue<RequestResponse> queue) {
        this.queue = queue;
    }

    public BlockingQueueResolver(final int packetQueueLength) {
        this(packetQueueLength < 1 ? new LinkedBlockingQueue<>() : new ArrayBlockingQueue<>(packetQueueLength));
    }

    public Resolver startQueueProcessingResolvers(final ExecutorService executor, final Iterable<QueueProcessingResolver> queueProcessingResolvers) {
        for(final QueueProcessingResolver queueProcessingResolver : queueProcessingResolvers)
            queueProcessingResolver.start(executor, this.queue);
        return this;
    }

    @Override
    public <E extends RequestResponse> CompletableFuture<E> resolveAsync(final E requestResponse, final Executor executor) {
        final CompletableFuture<E> request = new CompletableFuture<>();
        requestResponse.setCompletableFuture(request);
        queue.add(requestResponse);
        return request;
    }
}
