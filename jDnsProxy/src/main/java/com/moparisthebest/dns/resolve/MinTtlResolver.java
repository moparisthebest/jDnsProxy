package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MinTtlResolver implements Resolver {

    private final int minTtl;

    private final Resolver delegate;

    public static Resolver of(final int minTtl, final Resolver delegate) {
        // anything less than 1 just don't wrap
        return minTtl < 1 ? delegate : new MinTtlResolver(minTtl, delegate);
    }

    private MinTtlResolver(final int minTtl, final Resolver delegate) {
        this.minTtl = minTtl;
        this.delegate = delegate;
    }

    @Override
    public <E extends RequestResponse> CompletableFuture<E> resolveAsync(final E requestResponse, final Executor executor) {
        return delegate.resolveAsync(requestResponse, executor).thenApply(s -> {
                s.getResponse().modTtls((ttl) -> Math.max(ttl, minTtl));
                //s.getResponse().modTtls((ttl) -> 30);
                return s;
            });
    }

    @Override
    public Packet resolve(final Packet request) throws Exception {
        return delegate.resolve(request).modTtls((ttl) -> Math.max(ttl, minTtl));
    }
}
