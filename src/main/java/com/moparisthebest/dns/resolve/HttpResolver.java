package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.*;

import static com.moparisthebest.dns.Util.readPacket;

public class HttpResolver extends AbstractQueueProcessingResolver {
    private final OpenConnection openConnection;
    private final int connectTimeout;
    private final int readTimeout = 4000;

    interface OpenConnection {
        HttpURLConnection open() throws Exception;
    }

    public HttpResolver(final int maxRetries, final String name, final URL url, final int connectTimeout, final Proxy proxy, final SSLSocketFactory sslSocketFactory) {
        super(maxRetries, name);
        this.connectTimeout = connectTimeout;
        if(proxy == null && sslSocketFactory == null) {
            openConnection = () -> (HttpURLConnection) url.openConnection();
        } else if(proxy != null && sslSocketFactory == null) {
            openConnection = () -> (HttpURLConnection) url.openConnection(proxy);
        } else if(proxy == null
                //&& sslSocketFactory != null
                ) {
            openConnection = () -> {
                final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setSSLSocketFactory(sslSocketFactory);
                return conn;
            };
        } else //if(proxy != null && sslSocketFactory != null)
        {
            openConnection = () -> {
                final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection(proxy);
                conn.setSSLSocketFactory(sslSocketFactory);
                return conn;
            };
        }
    }

    @Override
    public Packet resolve(final Packet request) throws Exception {
        final HttpURLConnection conn = openConnection.open();

        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/dns-udpwireformat");
        conn.setRequestProperty("Accept", "application/dns-udpwireformat");

        //conn.connect();

        try (OutputStream os = conn.getOutputStream()) {

            os.write(request.getBuf().array());
            os.flush();

            try (InputStream is = conn.getInputStream();
                 DataInputStream dis = new DataInputStream(is);) {
                final int responseCode = conn.getResponseCode();
                //System.out.println("responseCode: " + responseCode);

                final int contentLength = conn.getContentLength();
                //System.out.println("contentLength: " + contentLength);

                return readPacket(contentLength, dis);
            }
        }
    }
}
