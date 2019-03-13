package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;
import com.moparisthebest.dns.net.ParsedUrl;

import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;

public interface Resolver {
    default <E extends RequestResponse> CompletableFuture<E> resolveAsync(E requestResponse) {
        return null;
        /*
        return CompletableFuture.supplyAsync(() -> {
            requestResponse.setResponse(resolve(requestResponse.getRequest()));
            return requestResponse;
        }, executor);
        */
    }

    Packet resolve(Packet request) throws Exception;

    ServiceLoader<Services> services = ServiceLoader.load(Services.class);

    static Resolver of(final ParsedUrl parsedUrl) {
        for (final Services s : services) {
            final Resolver ret = s.getResolver(parsedUrl);
            if (ret != null)
                return ret;
        }
        throw new IllegalArgumentException("unhandled resolver format: " + parsedUrl.getUrlStr());
    }
}
