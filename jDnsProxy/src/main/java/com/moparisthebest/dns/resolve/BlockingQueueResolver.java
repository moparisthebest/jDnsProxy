package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.Util;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class BlockingQueueResolver implements MultiResolver {

    private final BlockingQueue<RequestResponseCompletableFuture<? extends RequestResponse>> queue;

    private final List<QueueProcessingResolver> queueProcessingResolvers;

    public BlockingQueueResolver(final BlockingQueue<RequestResponseCompletableFuture<? extends RequestResponse>> queue, final ExecutorService executor, final Collection<Resolver> delegates) {
        this.queue = queue;
        if (delegates.isEmpty())
            throw new IllegalArgumentException("must supply at least 1 resolver");
        queueProcessingResolvers = delegates.stream().map(resolver -> new QueueProcessingResolver(resolver, executor, this.queue)).collect(Collectors.toList());
    }

    public BlockingQueueResolver(final int packetQueueLength, final ExecutorService executor, final Collection<Resolver> delegates) {
        this(packetQueueLength < 1 ? new LinkedBlockingQueue<>() : new ArrayBlockingQueue<>(packetQueueLength), executor, delegates);
    }

    @Override
    public <E extends RequestResponse> CompletableFuture<E> resolveAsync(final E requestResponse, final Executor executor) {
        final RequestResponseCompletableFuture<E> request = new RequestResponseCompletableFuture<>(requestResponse);
        queue.add(request);
        return request;
    }

    @Override
    public void close() {
        queueProcessingResolvers.forEach(Util::tryClose);
    }
}
