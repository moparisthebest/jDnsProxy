package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.util.concurrent.CompletableFuture;

public interface RequestResponse {
    Packet getRequest();
    Packet getResponse();
    void setResponse(Packet response);

    /**
     * These should only be used by resolvers, may be null
     */
    String getRequestPacketKey();
    void setRequestPacketKey(String key);
}
