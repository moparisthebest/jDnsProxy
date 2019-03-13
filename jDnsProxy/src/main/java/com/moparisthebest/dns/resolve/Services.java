package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.net.ParsedUrl;

public interface Services {
    Resolver getResolver(ParsedUrl parsedUrl);
}
