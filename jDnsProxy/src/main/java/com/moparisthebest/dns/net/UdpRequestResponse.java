package com.moparisthebest.dns.net;

import com.moparisthebest.dns.dto.Packet;
import com.moparisthebest.dns.resolve.BaseRequestResponse;

import java.net.SocketAddress;

public class UdpRequestResponse extends BaseRequestResponse {

    private final SocketAddress requester;

    public UdpRequestResponse(final SocketAddress requester, final Packet request) {
        super(request);
        this.requester = requester;
    }

    public SocketAddress getRequester() {
        return requester;
    }

    @Override
    public String toString() {
        return "UdpRequestResponse{" +
                "requester=" + requester +
                "} " + super.toString();
    }
}
