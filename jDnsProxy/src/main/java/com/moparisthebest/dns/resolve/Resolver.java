package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;
import com.moparisthebest.dns.net.ParsedUrl;

import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;

public interface Resolver {
    default <E extends RequestResponse> CompletableFuture<E> resolveAsync(E requestResponse) {
        try {
            requestResponse.setResponse(resolve(requestResponse.getRequest()));
            return CompletableFuture.completedFuture(requestResponse);
        } catch (Exception e) {
            final CompletableFuture<E> ret = new CompletableFuture<>();
            ret.completeExceptionally(e);
            return ret;
        }
        /*
        return CompletableFuture.supplyAsync(() -> {
            requestResponse.setResponse(resolve(requestResponse.getRequest()));
            return requestResponse;
        }, executor);
        */
    }

    default Packet resolve(Packet request) throws Exception {
        return resolveAsync(new BaseRequestResponse(request)).get().getResponse();
    }

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
