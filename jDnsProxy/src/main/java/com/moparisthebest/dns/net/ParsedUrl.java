package com.moparisthebest.dns.net;

import eu.geekplace.javapinning.java7.Java7Pinning;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ParsedUrl {

    private final SocketAddress addr;
    private final URI uri; // minus #
    private final Map<String, String> props; // after #, split by ;
    private final Proxy proxy;
    private final SSLSocketFactory sslSocketFactory;

    public ParsedUrl(final SocketAddress addr, final URI uri, final Map<String, String> props, final Proxy proxy, final SSLSocketFactory sslSocketFactory) {
        this.addr = addr;
        this.uri = uri;
        this.props = props;
        this.proxy = proxy;
        this.sslSocketFactory = sslSocketFactory;
    }

    public static ParsedUrl of(final String urlStr) {
        return of(urlStr, null);
    }

    public static ParsedUrl of(final String urlStr, final Map<String, String> upperLevelProps) {
        try {
            final URI url = new URI(urlStr.trim());
            final SocketAddress addr = new InetSocketAddress(url.getHost(), getPort(url));
            final Map<String, String> props = upperLevelProps == null ? new HashMap<>() : new HashMap<>(upperLevelProps);
            if(url.getFragment() != null)
                Arrays.stream(url.getFragment().split(";"))
                    .map(p -> p.split("=", 2)).forEach(p -> props.put(p[0], p.length > 1 ? p[1] : null));
            Proxy proxy = null;
            final String proxyStr = props.get("proxy");
            if (proxyStr != null) {
                final URI proxyUrl = new URI(proxyStr);
                if(proxyUrl.getPort() == -1)
                    throw new RuntimeException("proxy url must define port");
                Proxy.Type type = null;
                if (proxyUrl.getScheme().toLowerCase().startsWith("socks"))
                    type = Proxy.Type.SOCKS;
                else if (proxyUrl.getScheme().toLowerCase().startsWith("http"))
                    type = Proxy.Type.HTTP;
                else
                    throw new RuntimeException("proxy url must be socks or http");
                proxy = new Proxy(type, new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort()));
            }
            SSLSocketFactory sslSocketFactory = null;
            final String pubKeyPinsSha256 = props.get("pubKeyPinsSha256");
            if (pubKeyPinsSha256 != null) {
                final String[] pins = pubKeyPinsSha256.split(",");
                // todo: ugh java-pinning only supports hex not base64 *and* hashes the cert one time per pin, fix this
                for (int x = 0; x < pins.length; ++x) {
                    pins[x] = "SHA256:" + bytesToHex(Base64.getDecoder().decode(pins[x]));
                }
                final SSLContext sslContext;
                try {
                    sslContext = Java7Pinning.forPins(pins);
                } catch (KeyManagementException | NoSuchAlgorithmException e) {
                    throw new RuntimeException("invalid pins", e);
                }
                sslSocketFactory = sslContext.getSocketFactory();
            }
            if(sslSocketFactory == null && url.getScheme().equals("tls"))
                sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            return new ParsedUrl(addr, url, props, proxy, sslSocketFactory);
        } catch (Exception e) {
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            throw new RuntimeException("invalid URL format: '" + urlStr + "'", e);
        }
    }

    private static int getPort(final URI uri) {
        final int port = uri.getPort();
        if(port != -1)
            return port;
        switch (uri.getScheme()) {
            case "tcp":
            case "udp":
                return 53;
            case "tls":
                return 853;
            case "http":
                return 80;
            case "https":
                return 443;
        }
        return port;
    }

    public String getProtocol() {
        return uri.getScheme().toLowerCase();
    }

    public URL getUrlWithoutFragment() {
        return toUrlRemoveRef(uri);
    }

    public SocketAddress getAddr() {
        return addr;
    }

    public URI getUri() {
        return uri;
    }

    public Map<String, String> getProps() {
        return props;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    private static URL toUrlRemoveRef(final URI uri) {
        // mostly copied from java.net.URLStreamHandler.toExternalForm
        try {
            final URL u = uri.toURL();

            // pre-compute length of StringBuffer
            int len = u.getProtocol().length() + 1;
            if (u.getAuthority() != null && u.getAuthority().length() > 0)
                len += 2 + u.getAuthority().length();
            if (u.getPath() != null) {
                len += u.getPath().length();
            }
            if (u.getQuery() != null) {
                len += 1 + u.getQuery().length();
            }
        /*
        if (u.getRef() != null)
            len += 1 + u.getRef().length();
        */

            final StringBuilder result = new StringBuilder(len);
            result.append(u.getProtocol());
            result.append(":");
            if (u.getAuthority() != null && u.getAuthority().length() > 0) {
                result.append("//");
                result.append(u.getAuthority());
            }
            if (u.getPath() != null) {
                result.append(u.getPath());
            }
            if (u.getQuery() != null) {
                result.append('?');
                result.append(u.getQuery());
            }
        /*
        if (u.getRef() != null) {
            result.append("#");
            result.append(u.getRef());
        }
        */
            return new URL(result.toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException("invalid URL format: " + uri.toString());
        }
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
