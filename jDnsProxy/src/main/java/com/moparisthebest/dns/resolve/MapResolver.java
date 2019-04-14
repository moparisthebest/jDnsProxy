package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class MapResolver extends WrappingResolver {

    private final Function<? super Packet, ? extends Packet> mapper;

    public static Resolver minTtl(final int minTtl, final Resolver delegate) {
        // anything less than 1 just don't wrap
        return minTtl < 1 ? delegate : new MapResolver(delegate, (s) -> s.modTtls((ttl) -> Math.max(ttl, minTtl)));
    }

    private MapResolver(final Resolver delegate, final Function<? super Packet, ? extends Packet> mapper) {
        super(delegate);
        this.mapper = mapper;
    }

    @Override
    public CompletableFuture<Packet> resolveAsync(final Packet request, final Executor executor) {
        return delegate.resolveAsync(request, executor).thenApply(mapper);
    }

    @Override
    public Packet resolve(final Packet request) throws Exception {
        return mapper.apply(delegate.resolve(request));
    }
}
