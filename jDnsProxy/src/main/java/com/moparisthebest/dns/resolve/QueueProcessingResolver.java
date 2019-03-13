package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.net.ParsedUrl;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
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
        return new DelegatingQueueProcessingResolver(maxRetries, name, Resolver.of(parsedUrl));
    }
}
