package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class MapResolver implements Resolver {

    private final Resolver delegate;
    private final Function<? super Packet, ? extends Packet> mapper;

    public static Resolver minTtl(final int minTtl, final Resolver delegate) {
        // anything less than 1 just don't wrap
        return minTtl < 1 ? delegate : new MapResolver(delegate, (s) -> s.modTtls((ttl) -> Math.max(ttl, minTtl)));
    }

    private MapResolver(final Resolver delegate, final Function<? super Packet, ? extends Packet> mapper) {
        this.delegate = delegate;
        this.mapper = mapper;
    }

    @Override
    public <E extends RequestResponse> CompletableFuture<E> resolveAsync(final E requestResponse, final Executor executor) {
        return delegate.resolveAsync(requestResponse, executor).thenApply(s -> {
                s.setResponse(mapper.apply(s.getResponse()));
                return s;
            });
    }

    @Override
    public Packet resolve(final Packet request) throws Exception {
        return mapper.apply(delegate.resolve(request));
    }
}
