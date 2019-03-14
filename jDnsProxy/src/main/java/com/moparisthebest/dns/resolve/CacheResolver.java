package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;
import com.moparisthebest.dns.dto.Question;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.*;

import static com.moparisthebest.dns.Util.supplyAsyncOnTimeOut;

public class CacheResolver implements Resolver, AutoCloseable {

    private final int minTtl, staleResponseTtl;
    private final long staleResponseTimeout;

    private final BlockingQueue<RequestResponse> queue;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduledExecutorService;

    private final ConcurrentMap<String, CachedPacket> cache = new ConcurrentHashMap<>();

    public CacheResolver(final int minTtl, final int staleResponseTtl, final long staleResponseTimeout, final int packetQueueLength, final ExecutorService executor, final ScheduledExecutorService scheduledExecutorService,
                         final String cacheFile, final long cacheDelayMinutes) throws IOException {
        this.minTtl = minTtl;
        this.staleResponseTtl = staleResponseTtl;
        this.staleResponseTimeout = staleResponseTimeout;
        this.queue = packetQueueLength < 1 ? new LinkedBlockingQueue<>() : new ArrayBlockingQueue<>(packetQueueLength);
        this.executor = executor;
        this.scheduledExecutorService = scheduledExecutorService;
        if(cacheFile != null && !cacheFile.isEmpty()) {
            final File cacheFileFile = new File(cacheFile);
            readCache(cacheFileFile, cache);
            scheduledExecutorService.scheduleWithFixedDelay(() -> {
                try {
                    persistCache(cacheFileFile, cache);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, cacheDelayMinutes, cacheDelayMinutes, TimeUnit.MINUTES);
        }
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

    public void persistCache(final File file, final Map<String, CachedPacket> cache) throws IOException {
        final File tmpFile = new File(file.getAbsolutePath() + ".tmp");
        try(FileOutputStream fos = new FileOutputStream(tmpFile);
            DataOutputStream dos = new DataOutputStream(fos)) {
            for(final Map.Entry<String, CachedPacket> entry : cache.entrySet()) {

                dos.writeUTF(entry.getKey());

                final CachedPacket cp = entry.getValue();

                final byte[] rawCopy = cp.response.copyRaw();
                dos.writeInt(rawCopy.length);
                dos.write(rawCopy);

                dos.writeLong(cp.receivedSeconds);
                dos.writeLong(cp.expiredSeconds);
            }
        }
        // after the file is fully written, move it into place, should be atomic
        Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public void readCache(final File file, final Map<String, CachedPacket> cache) throws IOException {
        if(file.exists())
        try(FileInputStream fis = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(fis)) {
            final String key = dis.readUTF();
            final byte[] packet = new byte[dis.readInt()];
            dis.readFully(packet);
            cache.put(key, new CachedPacket(
                    new Packet(ByteBuffer.wrap(packet), 0, packet.length),
                    dis.readLong(), dis.readLong()));
        } catch(EOFException e) {
            // ignore this, we just hit end of file
        }
    }
}
