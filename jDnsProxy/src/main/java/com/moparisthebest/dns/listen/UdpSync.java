package com.moparisthebest.dns.listen;

import com.moparisthebest.dns.dto.Packet;
import com.moparisthebest.dns.net.*;
import com.moparisthebest.dns.resolve.Resolver;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

public class UdpSync implements Listener {

    private final int maxPacketLength = 2048;

    private final SocketAddress local;
    private final Resolver resolver;
    private final ExecutorService executor;

    private boolean running = true;
    private Thread thisThread = null;

    public UdpSync(final SocketAddress local, final Resolver resolver, final ExecutorService executor) {
        this.local = local;
        this.resolver = resolver;
        this.executor = executor;
    }

    @Override
    public void run() {
        try (final DatagramSocket ss = new DatagramSocket(local)) {

            final DatagramPacket request = new DatagramPacket(new byte[maxPacketLength], maxPacketLength);

            thisThread = Thread.currentThread();
            while (running) {
                ss.receive(request);

                //System.out.println("got packet");
                final SocketAddress requester = request.getSocketAddress();
                final Packet requestPacket = new Packet(ByteBuffer.wrap(request.getData(), request.getOffset(), request.getLength()).slice());
                //System.out.println(requestResponse);
                //debugPacket(requestResponse.getRequest().getBuf());

                resolver.resolveAsync(requestPacket, executor).whenCompleteAsync((resp, t) -> {
                    if(t != null) {
                        t.printStackTrace();
                        return;
                    }
                    //debugPacket(urr.getResponse().getBuf());

                    //System.out.println("got response");
                    final byte[] response = resp.getBuf().array();
                    final DatagramPacket responsePacket = new DatagramPacket(response, response.length); // todo: always exact length? meh

                    responsePacket.setSocketAddress(requester);

                    try {
                        ss.send(responsePacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //System.out.println("sent packet");
                }, executor);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        running = false;
        if (thisThread != null)
            thisThread.interrupt();
    }
}
