package com.moparisthebest.dns.listen;

import com.moparisthebest.dns.net.ParsedUrl;
import com.moparisthebest.dns.resolve.Resolver;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

@SpringBootApplication
public class HttpServices implements Services {

    @Override
    public Listener getListener(final ParsedUrl parsedUrl, final Resolver resolver, final ExecutorService executor) {
        if (!"http".equals(parsedUrl.getProtocol())) {
            return null;
        }
        final InetSocketAddress isa = ((InetSocketAddress) parsedUrl.getAddr());
        return new HttpListener(isa, resolver, executor);
    }

}
