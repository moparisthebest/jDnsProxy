package com.moparisthebest.dns.listen;

import com.moparisthebest.dns.net.ParsedUrl;
import com.moparisthebest.dns.resolve.Resolver;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

public interface Listener extends Runnable, AutoCloseable {
    public static Listener of(final String listener, final Resolver resolver, final ExecutorService executor) {
        /*
        listener = listener.trim().toLowerCase();
        final String[] hostPort = listener.substring(6).split(":");
        //System.out.println("hostPort: " + Arrays.toString(hostPort));
        final SocketAddress socketAddress = new InetSocketAddress(hostPort[0], Integer.parseInt(hostPort[1]));
        //System.out.println("socketAddress: " + socketAddress);
        if(listener.startsWith("tcp://")) {
            return new TcpAsync(socketAddress, resolver, executor);
        } else if(listener.startsWith("udp://")) {
            return new UdpSync(socketAddress, resolver, executor);
        }
        */
        final ParsedUrl parsedUrl = ParsedUrl.of(listener);
        switch(parsedUrl.getProtocol()) {
            case "tcp":
                return new TcpAsync(parsedUrl.getAddr(), resolver, executor);
            case "udp":
                return new UdpSync(parsedUrl.getAddr(), resolver, executor);
        }
        throw new IllegalArgumentException("invalid listener format");
    }

    public static Listener ofAndStart(final String listener, final Resolver resolver, final ExecutorService executor) {
        final Listener ret = of(listener, resolver, executor);
        executor.execute(ret);
        return ret;
    }
}
