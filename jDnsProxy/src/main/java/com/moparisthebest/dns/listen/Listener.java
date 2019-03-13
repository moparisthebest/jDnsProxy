package com.moparisthebest.dns.listen;

import com.moparisthebest.dns.net.ParsedUrl;
import com.moparisthebest.dns.resolve.Resolver;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;

public interface Listener extends Runnable, AutoCloseable {

    ServiceLoader<Services> services = ServiceLoader.load(Services.class);

    static Listener of(final String listener, final Resolver resolver, final ExecutorService executor) {
        final ParsedUrl parsedUrl = ParsedUrl.of(listener);
        for (final Services s : services) {
            final Listener ret = s.getListener(parsedUrl, resolver, executor);
            if (ret != null)
                return ret;
        }
        throw new IllegalArgumentException("unhandled listener format: " + listener);
    }

    public static Listener ofAndStart(final String listener, final Resolver resolver, final ExecutorService executor) {
        final Listener ret = of(listener, resolver, executor);
        executor.execute(ret);
        return ret;
    }
}
