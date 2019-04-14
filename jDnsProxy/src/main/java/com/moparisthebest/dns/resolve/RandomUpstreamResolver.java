package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

public class RandomUpstreamResolver implements MultiResolver {

    private final Resolver[] delegates;

    public RandomUpstreamResolver(final Resolver... delegates) {
        this.delegates = delegates;
    }

    public RandomUpstreamResolver(final Collection<Resolver> delegates) {
        this.delegates = delegates.toArray(new Resolver[0]);
    }

    public Resolver random() {
        return delegates[ThreadLocalRandom.current().nextInt(delegates.length)];
    }

    @Override
    public <E extends RequestResponse> CompletableFuture<E> resolveAsync(final E requestResponse, final Executor executor) {
        return random().resolveAsync(requestResponse, executor);
    }

    @Override
    public Packet resolve(final Packet request) throws Exception {
        return random().resolve(request);
    }

}
