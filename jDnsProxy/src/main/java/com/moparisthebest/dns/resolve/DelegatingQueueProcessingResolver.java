package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.util.concurrent.CompletableFuture;

public class DelegatingQueueProcessingResolver extends AbstractQueueProcessingResolver {

    private final Resolver delegate;

    public DelegatingQueueProcessingResolver(final int maxRetries, final String name, final Resolver delegate) {
        super(maxRetries, name);
        this.delegate = delegate;
    }

    @Override
    public <E extends RequestResponse> CompletableFuture<E> resolveAsync(final E requestResponse) {
        return delegate.resolveAsync(requestResponse);
    }

    @Override
    public Packet resolve(final Packet request) throws Exception {
        return delegate.resolve(request);
    }
}
