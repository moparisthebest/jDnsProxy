package com.moparisthebest.dns;

import com.moparisthebest.dns.dto.Packet;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Util {

    private Util() throws Exception {
        throw new Exception("no instance for you");
    }

    public static Packet readTcpPacket(final DataInputStream dis) throws IOException {
        final int length = dis.readUnsignedShort();
        return readPacket(length, dis);
    }

    public static Packet readPacket(final int length, final DataInputStream dis) throws IOException {
        //System.out.println("length: " + length);

        final ByteBuffer bb = ByteBuffer.allocate(length);
        final byte[] request = bb.array();

        dis.readFully(request);
        //System.out.println("request:");
        //debugPacket(request);
        return new Packet(bb);
    }

    public static void writeTcpPacket(final Packet p, final DataOutputStream dos) throws IOException {
        dos.writeShort(p.getBuf().capacity());
        dos.write(p.getBuf().array());
    }

    public static void tryClose(final AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Throwable e) {
            // ignore
        }
    }

    public static <U> CompletableFuture<U> supplyAsyncOnTimeOut(final ScheduledExecutorService e, final long timeout, final TimeUnit unit, final Supplier<U> f) {
        if (e == null || f == null) throw new NullPointerException();
        final CompletableFuture<U> d = new CompletableFuture<>();
        e.schedule(() -> {
            if (!d.isDone()) {
                try {
                    d.complete(f.get());
                } catch (Throwable ex) {
                    d.completeExceptionally(ex);
                }
            }
        }, timeout, unit);
        return d;
    }

    /*
    // temp debug code

    public static void debugPacket(final byte[] packet) {
        System.out.println(Base64.getUrlEncoder().encodeToString(packet));
        //System.out.println(new Packet(ByteBuffer.wrap(packet, 2, packet.length - 2).slice()));
        System.out.println(new Packet(ByteBuffer.wrap(packet).slice()));
        printPrettyHexBytes(ByteBuffer.wrap(packet));
        printPrettyChars(packet);
        printPrettyDecimalUnsignedBytes(ByteBuffer.wrap(packet));
    }

    public static void printPrettyHexBytes(ByteBuffer bytes) {
        bytes = bytes.slice();
        System.out.println("-----------------------------");
        int count = 0;
        for (int x = 0; x < bytes.limit(); ++x) {
            System.out.printf("%02X ", bytes.get(x));
            if (++count == 8) {
                System.out.println();
                count = 0;
            } else if (count % 2 == 0) {
                System.out.print("| ");
            }
        }
        System.out.println();
        System.out.println("+++++++++++++++++++++++++++++");
    }

    public static void printPrettyChars(final byte[] bytes) {
        System.out.println("-----------------------------");
        int count = 0;
        for (final byte b : bytes) {
            System.out.printf("%02X(%c) ", b, (char) b);
            if (++count == 8) {
                System.out.println();
                count = 0;
            } else if (count % 2 == 0) {
                System.out.print("| ");
            }
        }
        System.out.println();
        System.out.println("+++++++++++++++++++++++++++++");
    }

    public static void printPrettyDecimalUnsignedBytes(ByteBuffer bytes) {
        bytes = bytes.slice();
        System.out.println("-----------------------------");
        int count = 0;
        for (int x = 0; x < bytes.limit(); ++x) {
            System.out.printf("%02X(%d) ", bytes.get(x), Byte.toUnsignedInt(bytes.get(x)));
            if (++count == 8) {
                System.out.println();
                count = 0;
            } else if (count % 2 == 0) {
                System.out.print("| ");
            }
        }
        System.out.println();
        System.out.println("+++++++++++++++++++++++++++++");
    }

    public static Packet getResponse(final Packet request) throws Exception {
        final SocketFactory socketFactory = SocketFactory.getDefault();
        final Socket upstream = socketFactory.createSocket("8.8.4.4", 53);

        //final SSLContext sslContext = Java7Pinning.forPin("SHA256:c22904de360003a8d0997613affadb1de10f982efc750c196d0a7a59daec784b");
        //final SocketFactory socketFactory = sslContext.getSocketFactory();
        //final Socket upstream = socketFactory.createSocket("89.233.43.71", 853);

        //final Socket ssl = new SSL("8.8.4.4", 53);

        final Packet response;

        try (InputStream upIs = upstream.getInputStream();
             DataInputStream upDis = new DataInputStream(upIs);
             OutputStream upOs = upstream.getOutputStream();
             DataOutputStream upDos = new DataOutputStream(upOs)) {

            writeTcpPacket(request, upDos);
            upDos.flush();

            response = readTcpPacket(upDis);
        }
        return response;
    }

    public static Packet getHttpResponse(final Packet request) throws Exception {
        final URL url = new URL("https://dns.google.com/experimental?ct");
        final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        //final SSLContext sslContext = Java7Pinning.forPin("SHA256:c96d45b587a9230a50185ddd25ff36fd23bee886c91a5133d8bd82d9c4f0b676");
        //final SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        //conn.setSSLSocketFactory(socketFactory);

        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/dns-udpwireformat");
        conn.setRequestProperty("Accept", "application/dns-udpwireformat");

        try (OutputStream os = conn.getOutputStream()) {

            os.write(request.getBuf().array());
            os.flush();

            try (InputStream is = conn.getInputStream();
                 DataInputStream dis = new DataInputStream(is);) {
                final int responseCode = conn.getResponseCode();
                System.out.println("responseCode: " + responseCode);

                final int contentLength = conn.getContentLength();
                System.out.println("contentLength: " + contentLength);

                return readPacket(contentLength, dis);
            }
        }
    }

    public static byte[] getBytesFromInputStream(InputStream is) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();) {
            final byte[] buffer = new byte[2048];

            for (int len; (len = is.read(buffer)) != -1; )
                os.write(buffer, 0, len);

            os.flush();

            return os.toByteArray();
        }
    }

    public static void maine(String[] args) throws Throwable {
        // sync udp
        final DatagramSocket ss = new DatagramSocket(5555);
        final DatagramPacket request = new DatagramPacket(new byte[512], 512);
        final InetAddress upSrv = InetAddress.getByName("8.8.4.4");
        final DatagramSocket upstream = ss;//new DatagramSocket();
        //upstream.connect(upSrv, 53);
        while (!ss.isClosed()) {
            ss.receive(request);
            System.out.println("got packet");
            System.out.println(new Packet(ByteBuffer.wrap(request.getData(), request.getOffset(), request.getLength()).slice()));
            debugPacket(request.getData());
            final InetAddress old = request.getAddress();
            final int oldPort = request.getPort();
            request.setAddress(upSrv);
            request.setPort(53);
            upstream.send(request);
            System.out.println("sent packet");
            upstream.receive(request);
            System.out.println("got packet 2");
            request.setAddress(old);
            request.setPort(oldPort);
            ss.send(request);
            System.out.println("sent packet");
        }

        // sync tcp with debug
        if (false)
            try (FileInputStream request = new FileInputStream("test.request");
                 FileInputStream response = new FileInputStream("test.resolve");
            ) {
                System.out.println("request: ");
                debugPacket(getBytesFromInputStream(request));
                System.out.println("resolve: ");
                debugPacket(getBytesFromInputStream(response));
                // just testing
                return;
            } catch (FileNotFoundException e) {
                // ignore, start normal
            }
        //if(true) return;
        final ServerSocket ss = new ServerSocket(5555);
        while (!ss.isClosed()) {
            final Socket s = ss.accept();
            System.out.println("got socket");
            try (InputStream is = s.getInputStream();
                 DataInputStream dis = new DataInputStream(is);
                 OutputStream os = s.getOutputStream();
                 DataOutputStream dos = new DataOutputStream(os)) {

                final Packet request = readTcpPacket(dis);
                final int requestId = request.getId();
                request.setId(0);
                try (FileOutputStream fos = new FileOutputStream("test.request")) {
                    fos.write(request.getBuf().array());
                }
                System.out.println("request: " + request);

                //final Packet resolve = getResponse(request);
                final Packet response = getHttpResponse(request);
                try (FileOutputStream fos = new FileOutputStream("test.resolve")) {
                    fos.write(response.getBuf().array());
                }
                System.out.println("resolve: " + response);
                //debugPacket(resolve);

                response.setId(requestId);
                writeTcpPacket(response, dos);
                dos.flush();

            }
        }
    }
    */
}
