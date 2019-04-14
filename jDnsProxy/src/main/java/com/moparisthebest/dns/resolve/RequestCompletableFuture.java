package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.util.concurrent.CompletableFuture;

public class RequestCompletableFuture extends CompletableFuture<Packet> {

    private final Packet request;

    public RequestCompletableFuture(final Packet request) {
        this.request = request;
    }

    public Packet getRequest() {
        return request;
    }
}
