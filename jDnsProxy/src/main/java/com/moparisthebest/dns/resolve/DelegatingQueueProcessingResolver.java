package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DelegatingQueueProcessingResolver extends AbstractQueueProcessingResolver {

    private final Resolver delegate;

    public DelegatingQueueProcessingResolver(final int maxRetries, final String name, final Resolver delegate) {
        super(maxRetries, name);
        this.delegate = delegate;
    }

    @Override
    public <E extends RequestResponse> CompletableFuture<E> resolveAsync(final E requestResponse, final Executor executor) {
        return delegate.resolveAsync(requestResponse, executor);
    }

    @Override
    public Packet resolve(final Packet request) throws Exception {
        return delegate.resolve(request);
    }

    @Override
    public void close() {
        super.close();
        delegate.close();
    }
}
