package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class RetryResolver extends WrappingResolver {

    protected final int maxRetries;

    public static Resolver of(final int maxRetries, final Resolver delegate) {
        // anything less than 1 just don't wrap
        return maxRetries < 1 ? delegate : new RetryResolver(delegate, maxRetries);
    }

    private RetryResolver(final Resolver delegate, final int maxRetries) {
        super(delegate);
        this.maxRetries = maxRetries;
    }

    @Override
    public CompletableFuture<Packet> resolveAsync(final Packet request, final Executor executor) {
        // todo: better async way to do this?
        final CompletableFuture<Packet> ret = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                ret.complete(resolve(request));
            } catch (Throwable e) {
                ret.completeExceptionally(e);
            }
        });
        return ret;
    }

    @Override
    public Packet resolve(final Packet request) throws Exception {
        for(int x = 0; x < maxRetries; ++x) {
            try {
                final Packet response = super.resolve(request);
                if(response != null)
                    return response;
            } catch (Exception e) {
                e.printStackTrace();
                //System.err.println("FAILURE: " + name + ": " + e.getMessage());
            }
        }
        throw new Exception("SRVFAIL");
    }
}
