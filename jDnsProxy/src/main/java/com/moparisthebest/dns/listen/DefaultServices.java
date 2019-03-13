package com.moparisthebest.dns.listen;

import com.moparisthebest.dns.net.ParsedUrl;
import com.moparisthebest.dns.resolve.Resolver;

import java.util.concurrent.ExecutorService;

public class DefaultServices implements Services {
    @Override
    public Listener getListener(ParsedUrl parsedUrl, final Resolver resolver, final ExecutorService executor) {
        switch(parsedUrl.getProtocol()) {
            case "tcp":
                return new TcpAsync(parsedUrl.getAddr(), resolver, executor);
            case "udp":
                return new UdpSync(parsedUrl.getAddr(), resolver, executor);
        }
        return null;
    }
}
