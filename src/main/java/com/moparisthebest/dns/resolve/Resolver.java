package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;
import com.moparisthebest.dns.listen.Listener;
import com.moparisthebest.dns.listen.TcpAsync;
import com.moparisthebest.dns.listen.UdpSync;

import javax.net.SocketFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public interface Resolver {
    <E extends RequestResponse> CompletableFuture<E> resolveAsync(E requestResponse);
    Packet resolve(Packet request) throws Exception;
}
