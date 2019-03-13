package com.moparisthebest.dns.listen;

import com.moparisthebest.dns.net.ParsedUrl;
import com.moparisthebest.dns.resolve.Resolver;

import java.util.concurrent.ExecutorService;

public class XmppServices implements Services {
    @Override
    public Listener getListener(ParsedUrl parsedUrl, final Resolver resolver, final ExecutorService executor) {
        if ("xmpp".equals(parsedUrl.getProtocol())) {
            return new XmppListener(parsedUrl, resolver, executor);
        }
        return null;
    }
}
