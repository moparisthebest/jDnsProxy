package com.moparisthebest.dns;

import com.moparisthebest.dns.listen.Listener;
import com.moparisthebest.dns.net.ParsedUrl;
import com.moparisthebest.dns.resolve.CacheResolver;
import com.moparisthebest.dns.resolve.QueueProcessingResolver;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.moparisthebest.dns.Util.tryClose;

public class DnsProxy {

    public static void main(String[] args) throws Throwable {

        final Map<String, String> config;
        final File propsFile = new File(args.length > 0 ? args[0] : "jdnsproxy.properties");
        if(propsFile.canRead()) {
            try (FileInputStream fis = new FileInputStream(propsFile)) {
                final Properties props = new Properties();
                props.load(fis);
                @SuppressWarnings("unchecked") final Map<String, String> configUnchecked = (Map<String, String>) (Object) props;
                config = configUnchecked;
            }
        } else {
            if(args.length > 0) {
                System.err.printf("Error: config file '%s' does not exist or can't be read%n", args[0]);
                return;
            }
            config = Collections.emptyMap();
        }
        System.out.println("config:" + config);

        final int minTtl = Integer.parseInt(config.getOrDefault("minTtl", "600"));
        final int staleResponseTtl = Integer.parseInt(config.getOrDefault("staleResponseTtl", "10"));
        final long staleResponseTimeout = Long.parseLong(config.getOrDefault("staleResponseTimeout", "1000"));
        final int packetQueueLength = Integer.parseInt(config.getOrDefault("packetQueueLength", "100"));

        final String[] resolvers = config.getOrDefault("resolvers", "https://dns.google.com/experimental?ct#name=dns.google.com").split("\\s+");
        if (!config.containsKey("maxRetries"))
            config.put("maxRetries", String.valueOf(resolvers.length * 2));

        //System.out.println("resolvers: " + Arrays.toString(resolvers));

        final List<QueueProcessingResolver> queueProcessingResolvers = Arrays.stream(resolvers).map(s -> ParsedUrl.of(s, config)).map(QueueProcessingResolver::of).collect(Collectors.toList());
        //final List<QueueProcessingResolver> queueProcessingResolvers = new ArrayList<>();
        //queueProcessingResolvers.add(new SocketResolver(5, "socket1", SocketFactory.getDefault(), new InetSocketAddress("8.8.4.4", 53)));
        //queueProcessingResolvers.add(new HttpResolver(5, "http1", "https://dns.google.com/experimental?ct"));

        final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(40);
        final ExecutorService executor = scheduledExecutorService;//ForkJoinPool.commonPool();

        final CacheResolver resolver = new CacheResolver(minTtl, staleResponseTtl, staleResponseTimeout, packetQueueLength, executor, scheduledExecutorService)
                .startQueueProcessingResolvers(queueProcessingResolvers);

        final List<Listener> listeners = Arrays.stream(config.getOrDefault("listeners", "tcp://127.0.0.1:5353 udp://127.0.0.1:5353").split("\\s+"))
                .map(url -> Listener.ofAndStart(url, resolver, executor)).collect(Collectors.toList());
        //final List<Listener> listeners = new ArrayList<>();
        //listeners.add(Listener.ofAndStart("tcp://127.0.0.1:5556", resolver, executor));
        //listeners.add(Listener.ofAndStart("udp://127.0.0.1:5556", resolver, executor));

        System.out.println("startup complete");

        final Thread cleanKill = new Thread(() -> {
            System.out.println("shutdown requested");
            //if(true) return;
            executor.shutdown();
            scheduledExecutorService.shutdown();
            queueProcessingResolvers.forEach(Util::tryClose);
            listeners.forEach(Util::tryClose);
            tryClose(resolver);
            System.out.println("shutdown complete");
        });

        Runtime.getRuntime().addShutdownHook(cleanKill);
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }
}
