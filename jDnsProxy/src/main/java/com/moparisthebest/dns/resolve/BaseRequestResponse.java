package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;

public class BaseRequestResponse implements RequestResponse {

    private Packet request, response;

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
    public String toString() {
        return "BaseRequestResponse{" +
                "request=" + request +
                ", response=" + response +
                '}';
    }
}
