package com.moparisthebest.dns.resolve;

import java.util.concurrent.CompletableFuture;

public class RequestResponseCompletableFuture<E extends RequestResponse> extends CompletableFuture<E> {

    private final E requestResponse;

    public RequestResponseCompletableFuture(final E requestResponse) {
        this.requestResponse = requestResponse;
    }

    public E getRequestResponse() {
        return requestResponse;
    }
}
