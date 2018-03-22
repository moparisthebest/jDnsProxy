package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

import static com.moparisthebest.dns.Util.readTcpPacket;
import static com.moparisthebest.dns.Util.tryClose;
import static com.moparisthebest.dns.Util.writeTcpPacket;

public class SocketResolver extends AbstractQueueProcessingResolver {
    private final OpenSocket openConnection;
    private final int readTimeout = 4000;

    private Socket activeSocket;

    interface OpenSocket {
        Socket open() throws Exception;
    }

    public SocketResolver(final int maxRetries, final String name, final SocketAddress endpoint, final int connectTimeout, final Proxy proxy, final SSLSocketFactory sslSocketFactory) {
        super(maxRetries, name);
        if(proxy == null && sslSocketFactory == null) {
            openConnection = () -> {
                Socket s = null;
                try {
                    s = new Socket();
                    s.connect(endpoint, connectTimeout);
                    return s;
                } catch(Throwable e) {
                    tryClose(s);
                    throw e;
                }
            };
        } else if(proxy != null && sslSocketFactory == null) {
            openConnection = () -> {
                Socket s = null;
                try {
                    s = new Socket(proxy);
                    s.connect(endpoint, connectTimeout);
                    return s;
                } catch(Throwable e) {
                    tryClose(s);
                    throw e;
                }
            };
        } else if(proxy == null
            //&& sslSocketFactory != null
                ) {
            openConnection = () -> {
                Socket s = null;
                try {
                    s = sslSocketFactory.createSocket();
                    s.connect(endpoint, connectTimeout);
                    return s;
                } catch(Throwable e) {
                    tryClose(s);
                    throw e;
                }
            };
        } else //if(proxy != null && sslSocketFactory != null)
        {
            // https://stackoverflow.com/questions/5783832/socks5-proxy-using-sslsocket
            final String proxyHost = ((InetSocketAddress)proxy.address()).getHostString();
            final int proxyPort = ((InetSocketAddress)proxy.address()).getPort();
            openConnection = () -> {
                Socket s = null;
                try {
                    s = new Socket(proxy);
                    s.connect(endpoint, connectTimeout);
                    return (SSLSocket) sslSocketFactory.createSocket(
                            s,
                            proxyHost,
                            proxyPort,
                            true);
                } catch(Throwable e) {
                    tryClose(s);
                    throw e;
                }
            };
        }

        try {
            this.getSocket();
        } catch (Exception e) {
            System.err.println("Failed to open socket to " + name);
            e.printStackTrace();
        }
    }

    private Socket getSocket() throws Exception {
        if (this.activeSocket == null || this.activeSocket.isClosed()) {
            this.activeSocket = openConnection.open();
            this.activeSocket.setKeepAlive(true);
            this.activeSocket.setSoTimeout(readTimeout);
        }

        return this.activeSocket;
    }

    @Override
    public Packet resolve(final Packet request) throws Exception {
                /*
        final SocketFactory socketFactory = SocketFactory.getDefault();
                final SSLContext sslContext = Java7Pinning.forPin("SHA256:c22904de360003a8d0997613affadb1de10f982efc750c196d0a7a59daec784b");
                final SocketFactory socketFactory = sslContext.getSocketFactory();
                final Socket upstream = socketFactory.createSocket("89.233.43.71", 853);

        final Socket upstream = socketFactory.createSocket("8.8.4.4", 53);
        final SocketFactory socketFactory = SocketFactory.getDefault();

        try (Socket upstream = socketFactory.createSocket()) {
            upstream.connect(endpoint, 500);
                */

            Socket upstream = this.getSocket();

            try (InputStream upIs = upstream.getInputStream();
                 DataInputStream upDis = new DataInputStream(upIs);
                 OutputStream upOs = upstream.getOutputStream();
                 DataOutputStream upDos = new DataOutputStream(upOs)) {
                writeTcpPacket(request, upDos);
                upDos.flush();

                return readTcpPacket(upDis);
            }
    }
}
