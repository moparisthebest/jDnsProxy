package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;
import com.moparisthebest.dns.dto.Question;

import java.util.concurrent.*;

import static com.moparisthebest.dns.Util.supplyAsyncOnTimeOut;

public class CacheResolver implements Resolver, AutoCloseable {

    private final int minTtl, staleResponseTtl;
    private final long staleResponseTimeout;

    private final BlockingQueue<RequestResponse> queue;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduledExecutorService;

    private final ConcurrentMap<Object, CachedPacket> cache = new ConcurrentHashMap<>();

    public CacheResolver(final int minTtl, final int staleResponseTtl, final long staleResponseTimeout, final int packetQueueLength, final ExecutorService executor, final ScheduledExecutorService scheduledExecutorService) {
        this.minTtl = minTtl;
        this.staleResponseTtl = staleResponseTtl;
        this.staleResponseTimeout = staleResponseTimeout;
        this.queue = packetQueueLength < 1 ? new LinkedBlockingQueue<>() : new ArrayBlockingQueue<>(packetQueueLength);
        this.executor = executor;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public CacheResolver startQueueProcessingResolvers(final Iterable<QueueProcessingResolver> queueProcessingResolvers) {
        for(final QueueProcessingResolver queueProcessingResolver : queueProcessingResolvers)
            queueProcessingResolver.start(this.executor, this.queue);
        return this;
    }

    @Override
    public void close() {

    }

    private class CachedPacket {
        final Packet response;
        final long receivedSeconds, expiredSeconds;

        public CachedPacket(final Packet response, final long receivedSeconds, final long expiredSeconds) {
            this.response = response;
            this.receivedSeconds = receivedSeconds;
            this.expiredSeconds = expiredSeconds;
        }

        boolean isExpired(final long currentSeconds) {
            return currentSeconds > expiredSeconds;
        }

        Packet getStaleResponse() {
            return response.copy().modTtls((ttl) -> staleResponseTtl);
        }

        Packet getResponse(final long currentSeconds) {
            final int timePassed = (int)(currentSeconds - receivedSeconds);
            return response.copy().modTtls((ttl) -> ttl - timePassed);
        }

        @Override
        public String toString() {
            return "CachedPacket{" +
                    "response=" + response +
                    ", receivedSeconds=" + receivedSeconds +
                    ", expiredSeconds=" + expiredSeconds +
                    '}';
        }
    }

    private static long currentTimeSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    private String calcRequestPacketKey(final Packet request) {
        //debugPacket(request.getBuf().array());
        //System.out.println("request: " + request);
        final StringBuilder sb = new StringBuilder();
        // concat all query domains and types (todo: order maybe shouldn't matter meh)
        for (final Question q : request.getQuestions())
            sb.append(q.getName().getDomain()).append('|').append(q.getQType()).append('|');
        // also append whether dnssec is ok or not
        sb.append(request.isDnssecOk() ? 'Y' : 'N');
        return sb.toString();
    }

    @Override
    public <E extends RequestResponse> CompletableFuture<E> resolveAsync(final E requestResponse) {
        final String key = calcRequestPacketKey(requestResponse.getRequest());
        //System.out.println("requestPacketKey: " + key);
        final CachedPacket response = cache.get(key);
        //System.out.println("cachedPacket: " + response);
        if (response != null) {
            final long currentTime = currentTimeSeconds();
            if (response.isExpired(currentTime)) {
                //System.out.println("cachedPacket isExpired!");
                requestResponse.setRequestPacketKey(key);
                final CompletableFuture<E> request = requestAndCache(requestResponse);
                final CompletableFuture<E> stale = supplyAsyncOnTimeOut(scheduledExecutorService, staleResponseTimeout, TimeUnit.MILLISECONDS, () -> {
                    requestResponse.setResponse(response.getStaleResponse().setId(requestResponse.getRequest().getId()));
                    return requestResponse;
                });
                return request.applyToEitherAsync(stale, s -> s);
            } else {
                //System.out.println("cachedPacket returning from cache!");
                requestResponse.setResponse(response.getResponse(currentTime).setId(requestResponse.getRequest().getId()));
                return CompletableFuture.completedFuture(requestResponse);
            }
        }
        //System.out.println("no cachedPacket, querying upstream!");
        requestResponse.setRequestPacketKey(key);
        return requestAndCache(requestResponse);
        /*
        // todo: should not have to do this, some upstreams seem to eat stuff though, figure that out, I think readTimeout fixed this
        final CompletableFuture<E> request = requestAndCache(requestResponse);
        final CompletableFuture<E> abort = supplyAsyncOnTimeOut(scheduledExecutorService, 15000, TimeUnit.MILLISECONDS, () -> {
            throw new RuntimeException("timed out cause upstream ate us");
        });
        return request.applyToEitherAsync(abort, s -> s);
        */
    }

    //boolean first = true;

    private <E extends RequestResponse> CompletableFuture<E> requestAndCache(final E requestResponse) {
        CompletableFuture<E> request = new CompletableFuture<>();
        requestResponse.setCompletableFuture(request);
        //if(first) {
            //first = false;
            queue.add(requestResponse);
        //}
        if(minTtl > 0) {
            request = request.thenApply(s -> {
                s.getResponse().modTtls((ttl) -> Math.max(ttl, minTtl));
                //s.getResponse().modTtls((ttl) -> 30);
                return s;
            });
        }
        request.thenAcceptAsync(s -> {
            final Packet response = s.getResponse().copy(); // todo: do we need to copy?
            final long currentTime = currentTimeSeconds();
            cache.put(s.getRequestPacketKey(), new CachedPacket(response, currentTime, currentTime + response.getLowestTtl()));
        }, executor);
        return request;
    }

    @Override
    public Packet resolve(final Packet request) throws Exception {
        return resolveAsync(new BaseRequestResponse(request)).get().getResponse();
    }
}
