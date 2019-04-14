package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.Util;
import com.moparisthebest.dns.dto.Packet;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class BlockingQueueResolver implements MultiResolver {

    private final BlockingQueue<RequestCompletableFuture> queue;

    private final List<QueueProcessingResolver> queueProcessingResolvers;

    public BlockingQueueResolver(final BlockingQueue<RequestCompletableFuture> queue, final ExecutorService executor, final Collection<Resolver> delegates) {
        this.queue = queue;
        if (delegates.isEmpty())
            throw new IllegalArgumentException("must supply at least 1 resolver");
        queueProcessingResolvers = delegates.stream().map(resolver -> new QueueProcessingResolver(resolver, executor, this.queue)).collect(Collectors.toList());
    }

    public BlockingQueueResolver(final int packetQueueLength, final ExecutorService executor, final Collection<Resolver> delegates) {
        this(packetQueueLength < 1 ? new LinkedBlockingQueue<>() : new ArrayBlockingQueue<>(packetQueueLength), executor, delegates);
    }

    @Override
    public CompletableFuture<Packet> resolveAsync(final Packet request, final Executor executor) {
        final RequestCompletableFuture ret = new RequestCompletableFuture(request);
        queue.add(ret);
        return ret;
    }

    @Override
    public void close() {
        queueProcessingResolvers.forEach(Util::tryClose);
    }
}
