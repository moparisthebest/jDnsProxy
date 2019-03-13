package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.net.ParsedUrl;

public class XmppServices implements Services {
    @Override
    public Resolver getResolver(ParsedUrl parsedUrl) {
        if ("xmpp".equals(parsedUrl.getProtocol())) {
            return new XmppResolver(parsedUrl);
        }
        return null;
    }
}
