package com.moparisthebest.dns.net;

import java.nio.channels.CompletionHandler;

@FunctionalInterface
public interface FunctionalCompletionHandler<V,A> extends CompletionHandler<V,A> {
    @Override
    void completed(V result, A attachment);

    @Override
    default void failed(final Throwable e, final A attachment) {
        e.printStackTrace();
    }

    default FunctionalCompletionHandler<V,A> getThisFunctionalCompletionHandler() {
        return this;
    }
}
