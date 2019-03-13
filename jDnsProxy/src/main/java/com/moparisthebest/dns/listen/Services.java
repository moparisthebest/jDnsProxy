package com.moparisthebest.dns.listen;

import com.moparisthebest.dns.net.ParsedUrl;
import com.moparisthebest.dns.resolve.Resolver;

import java.util.concurrent.ExecutorService;

public interface Services {
    Listener getListener(ParsedUrl parsedUrl, Resolver resolver, ExecutorService executor);
}
