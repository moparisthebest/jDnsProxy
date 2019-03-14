package com.moparisthebest.dns.listen;

import com.moparisthebest.dns.resolve.Resolver;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class HttpListener implements Listener {

    private final InetSocketAddress isa;

    private ConfigurableApplicationContext ctx = null;

    public HttpListener(final InetSocketAddress isa, final Resolver resolver, final ExecutorService executor) {
        this.isa = isa;
        DohController.resolver = resolver;
        DohController.executor = executor;
    }

    @Override
    public void run() {
        final Map<String, Object> props = new HashMap<>();
        props.put("server.address", isa.getAddress().getHostAddress());
        props.put("server.port", isa.getPort());
        ctx = new SpringApplicationBuilder()
                .sources(HttpServices.class)
                .properties(props)
                .run();
    }

    @Override
    public void close() {
        ctx.stop();
    }
}
