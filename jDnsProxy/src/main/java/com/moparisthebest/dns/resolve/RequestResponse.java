package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.util.concurrent.CompletableFuture;

public interface RequestResponse {
    Packet getRequest();
    Packet getResponse();
    void setResponse(Packet response);
}
