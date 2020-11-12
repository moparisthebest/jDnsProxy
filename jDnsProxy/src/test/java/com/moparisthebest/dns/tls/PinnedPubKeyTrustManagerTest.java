package com.moparisthebest.dns.tls;

import org.junit.Test;

import javax.net.ssl.SSLSocketFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class PinnedPubKeyTrustManagerTest {

    @Test
    public void pinSha256SSLContext() {
        // todo: make these not rely on remote server, stand up local TLS server with hard-coded certs?
        assertEquals("pinnedpubkey authentication failed, server pin (SHA-256): fYQPv1BOUFcIGTz3W+FwhhI6x+S3VD3aMxMl1JyO4ow=",
                testPin("moparisthebest.com", 443, "bla").getCause().getMessage());
        assertEquals("pinnedpubkey authentication failed, server pin (SHA-256): fYQPv1BOUFcIGTz3W+FwhhI6x+S3VD3aMxMl1JyO4ow=",
                testPin("moparisthebest.com", 443, "eEHQC9au2QRAP1FnvcYEsmvXT7511EXQ2gw8ppBfseM=").getCause().getMessage());
        assertEquals("read: 1024",
                testPin("moparisthebest.com", 443, "fYQPv1BOUFcIGTz3W+FwhhI6x+S3VD3aMxMl1JyO4ow=").getMessage());
    }

    private static Exception testPin(final String hostname, final int port, final String... sha256Pins) {
        final SSLSocketFactory sf = PinnedPubKeyTrustManager.pinSha256SSLContext(sha256Pins).getSocketFactory();
        try(Socket s = sf.createSocket()) {
            s.connect(new InetSocketAddress(hostname, port), 5000);
            try (InputStream is = s.getInputStream();
                 OutputStream os = s.getOutputStream()) {
                os.write("GET /\r\n".getBytes(UTF_8));
                os.flush();
                final byte[] resp = new byte[1024];
                final int read = is.read(resp);
                return new Exception("read: " + read);
            }
        } catch(Exception e) {
            return e;
        }
    }
}