package com.moparisthebest.dns.resolve;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

public interface MultiResolver extends Resolver {

    public static MultiResolver of(final int packetQueueLength, final ExecutorService executor, final Collection<Resolver> delegates) {
        return packetQueueLength < 0 ?
                new RandomUpstreamResolver(delegates) :
                new BlockingQueueResolver(packetQueueLength, executor, delegates);
    }
}
