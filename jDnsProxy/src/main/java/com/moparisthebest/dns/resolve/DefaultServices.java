package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.net.ParsedUrl;

public class DefaultServices implements Services {
    @Override
    public Resolver getResolver(ParsedUrl parsedUrl) {
        final int connectTimeout = Integer.parseInt(parsedUrl.getProps().getOrDefault("connectTimeout", "500"));
        switch(parsedUrl.getProtocol()) {
            case "tcp":
            case "tls":
                return new SocketResolver(parsedUrl.getAddr(), connectTimeout, parsedUrl.getProxy(), parsedUrl.getSslSocketFactory());
            case "http":
            case "https":
                return new HttpResolver(parsedUrl.getUrlWithoutFragment(), connectTimeout, parsedUrl.getProxy(), parsedUrl.getSslSocketFactory());
        }
        return null;
    }
}
