package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;
import com.moparisthebest.dns.net.ParsedUrl;

import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Implementations need to implement at least 1 of the resolve functions, either resolveAsync or resolve, the default
 * implementations simply call the other one ending in a stack overflow.
 *
 * Ideally, implementations provide optimized versions of both the in-line and async call.
 */
public interface Resolver {

    /**
     * This must return immediately and resolve the DNS query in the background, using the given executor
     * @param requestResponse
     * @param executor
     * @param <E>
     * @return
     */
    default <E extends RequestResponse> CompletableFuture<E> resolveAsync(final E requestResponse, final Executor executor) {
        final CompletableFuture<E> ret = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                requestResponse.setResponse(resolve(requestResponse.getRequest()));
                ret.complete(requestResponse);
            } catch (Throwable e) {
                ret.completeExceptionally(e);
            }
        });
        return ret;
    }

    /**
     * This must block on resolving the given query
     * @param request
     * @return
     * @throws Exception
     */
    default Packet resolve(final Packet request) throws Exception {
        return resolveAsync(new BaseRequestResponse(request), Runnable::run).get().getResponse();
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
