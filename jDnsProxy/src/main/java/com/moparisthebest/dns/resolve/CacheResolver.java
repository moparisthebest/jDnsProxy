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

public class CacheResolver extends WrappingResolver {

    private final int staleResponseTtl;
    private final long staleResponseTimeout;

    private final ScheduledExecutorService scheduledExecutorService;

    private final ConcurrentMap<String, CachedPacket> cache = new ConcurrentHashMap<>();

    public CacheResolver(final Resolver delegate, final int staleResponseTtl, final long staleResponseTimeout, final ScheduledExecutorService scheduledExecutorService,
                         final String cacheFile, final long cacheDelayMinutes) throws IOException {
        super(delegate);
        this.staleResponseTtl = staleResponseTtl;
        this.staleResponseTimeout = staleResponseTimeout;
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
    public CompletableFuture<Packet> resolveAsync(final Packet request, final Executor executor) {
        final String key = calcRequestPacketKey(request);
        //System.out.println("requestPacketKey: " + key);
        final CachedPacket response = cache.get(key);
        //System.out.println("cachedPacket: " + response);
        if (response != null) {
            final long currentTime = currentTimeSeconds();
            if (response.isExpired(currentTime)) {
                //System.out.println("cachedPacket isExpired!");
                final CompletableFuture<Packet> ret = requestAndCache(key, request, executor);
                final CompletableFuture<Packet> stale = supplyAsyncOnTimeOut(scheduledExecutorService, staleResponseTimeout, TimeUnit.MILLISECONDS,
                        () -> response.getStaleResponse().setId(request.getId()));
                return ret.applyToEitherAsync(stale, s -> s);
            } else {
                //System.out.println("cachedPacket returning from cache!");
                return CompletableFuture.completedFuture(response.getResponse(currentTime).setId(request.getId()));
            }
        }
        //System.out.println("no cachedPacket, querying upstream!");
        return requestAndCache(key, request, executor);
        /*
        // todo: should not have to do this, some upstreams seem to eat stuff though, figure that out, I think readTimeout fixed this
        final CompletableFuture<E> request = requestAndCache(requestResponse);
        final CompletableFuture<E> abort = supplyAsyncOnTimeOut(scheduledExecutorService, 15000, TimeUnit.MILLISECONDS, () -> {
            throw new RuntimeException("timed out cause upstream ate us");
        });
        return request.applyToEitherAsync(abort, s -> s);
        */
    }

    private CompletableFuture<Packet> requestAndCache(final String key, final Packet request, final Executor executor) {
        final CompletableFuture<Packet> ret = delegate.resolveAsync(request, executor);
        ret.thenAcceptAsync(s -> {
            final Packet response = s.copy(); // todo: do we need to copy?
            final long currentTime = currentTimeSeconds();
            cache.put(key, new CachedPacket(response, currentTime, currentTime + response.getLowestTtl()));
        }, executor);
        return ret;
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
