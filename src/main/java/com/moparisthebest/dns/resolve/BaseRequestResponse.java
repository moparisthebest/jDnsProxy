package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;

public class BaseRequestResponse implements RequestResponse {

    private Packet request, response;

    private CompletableFuture<? extends RequestResponse> completableFuture;
    private Object requestPacketKey;
    private int failureCount;

    public BaseRequestResponse() {
    }

    public BaseRequestResponse(final Packet request) {
        this.request = request;
    }

    @Override
    public Packet getRequest() {
        return request;
    }

    public void setRequest(final Packet request) {
        this.request = request;
    }

    @Override
    public Packet getResponse() {
        return response;
    }

    @Override
    public void setResponse(final Packet response) {
        this.response = response;
    }

    @Override
    public CompletableFuture<? extends RequestResponse> getCompletableFuture() {
        return completableFuture;
    }

    @Override
    public void setCompletableFuture(final CompletableFuture<? extends RequestResponse> completableFuture) {
        this.completableFuture = completableFuture;
    }

    @Override
    public Object getRequestPacketKey() {
        return requestPacketKey;
    }

    @Override
    public void setRequestPacketKey(final Object requestPacketKey) {
        this.requestPacketKey = requestPacketKey;
    }

    @Override
    public final int getAndIncrementFailureCount() {
        return ++failureCount;
    }

    @Override
    public String toString() {
        return "BaseRequestResponse{" +
                "request=" + request +
                ", response=" + response +
                ", completableFuture=" + completableFuture +
                ", requestPacketKey=" + requestPacketKey +
                ", failureCount=" + failureCount +
                '}';
    }
}
