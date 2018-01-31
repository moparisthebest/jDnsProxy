package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.util.concurrent.CompletableFuture;

public interface RequestResponse {
    Packet getRequest();
    Packet getResponse();
    void setResponse(Packet response);

    /**
     * These should only be used by resolvers, may be null
     CompletableFuture<RequestResponse> getCompletableFuture();
     void setCompletableFuture(CompletableFuture<RequestResponse> completableFuture);
     <E extends RequestResponse> CompletableFuture<E> getCompletableFuture();
     <E extends RequestResponse> void setCompletableFuture(CompletableFuture<E> completableFuture);
     */
    CompletableFuture<? extends RequestResponse> getCompletableFuture();
    void setCompletableFuture(CompletableFuture<? extends RequestResponse> completableFuture);
    Object getRequestPacketKey();
    void setRequestPacketKey(Object key);
    int getAndIncrementFailureCount();
}
