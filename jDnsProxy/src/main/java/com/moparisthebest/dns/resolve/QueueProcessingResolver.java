package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;
import com.moparisthebest.dns.net.ParsedUrl;

import javax.net.SocketFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public interface QueueProcessingResolver extends Resolver, Runnable, AutoCloseable {
    void start(final ExecutorService executor, final BlockingQueue<RequestResponse> queue);

    public static QueueProcessingResolver of(final String resolver, final Map<String, String> upperLevelProps) {
        return of(ParsedUrl.of(resolver, upperLevelProps));
    }

    public static QueueProcessingResolver of(final String resolver) {
        return of(ParsedUrl.of(resolver));
    }

    public static QueueProcessingResolver of(final ParsedUrl parsedUrl) {
        final int maxRetries = Integer.parseInt(parsedUrl.getProps().getOrDefault("maxRetries", "5"));
        String name = parsedUrl.getProps().get("name");
        if(name == null)
            name = parsedUrl.getUri().toString();
        final int connectTimeout = Integer.parseInt(parsedUrl.getProps().getOrDefault("connectTimeout", "500"));
        switch(parsedUrl.getProtocol()) {
            case "tcp":
            case "tls":
                return new SocketResolver(maxRetries, name, parsedUrl.getAddr(), connectTimeout, parsedUrl.getProxy(), parsedUrl.getSslSocketFactory());
            case "http":
            case "https":
                return new HttpResolver(maxRetries, name, parsedUrl.getUrlWithoutFragment(), connectTimeout, parsedUrl.getProxy(), parsedUrl.getSslSocketFactory());
        }
        throw new IllegalArgumentException("invalid listener format");
    }
}
