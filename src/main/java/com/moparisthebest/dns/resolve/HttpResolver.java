package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLSocketFactory;
import java.net.*;
import java.util.concurrent.TimeUnit;


public class HttpResolver extends AbstractQueueProcessingResolver {
    private final HttpClient client;
    private final HttpPost request;

    public HttpResolver(final int maxRetries, final String name, final URL url, final int connectTimeout, final Proxy proxy, final SSLSocketFactory sslSocketFactory) {
        super(maxRetries, name);

        HttpHost proxyHost;

        if (proxy != null && proxy.type().equals(Proxy.Type.HTTP)) {
            final InetSocketAddress socketAddress = (InetSocketAddress) proxy.address();
            final InetAddress inetAddress = socketAddress.getAddress();

            proxyHost = new HttpHost(inetAddress, socketAddress.getPort());
        } else if (proxy != null && proxy.type().equals(Proxy.Type.SOCKS)) {
            throw new IllegalArgumentException("Socks hosts aren't supported");
        } else {
            proxyHost = null;
        }

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(connectTimeout)
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(connectTimeout)
                .build();

        this.client = HttpClients.custom()
                // .setSSLSocketFactory(new SSLConnectionSocketFactory(sslSocketFactory, (s, sslSession) -> s.equalsIgnoreCase(name)))
                .setProxy(proxyHost)
                .setConnectionTimeToLive(365, TimeUnit.DAYS)
                .setDefaultRequestConfig(requestConfig)
                .build();

        try {
            this.request = new HttpPost(url.toURI());
            this.request.setHeader(HttpHeaders.CONTENT_TYPE, "application/dns-udpwireformat");
            this.request.setHeader(HttpHeaders.ACCEPT, "application/dns-udpwireformat");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to parse upstream DNS URL", e);
        }
    }

    @Override
    public Packet resolve(final Packet request) throws Exception {
        final HttpPost req = (HttpPost) this.request.clone();
        req.setEntity(new ByteArrayEntity(request.getBuf().array()));

        final HttpResponse response = this.client.execute(req);

        final int responseCode = response.getStatusLine().getStatusCode();
        // System.out.println("responseCode: " + responseCode);

        final HttpEntity responseEntity = response.getEntity();

        final long contentLength = responseEntity.getContentLength();
        // System.out.println("contentLength: " + contentLength);

        return new Packet(EntityUtils.toByteArray(responseEntity));
    }
}
