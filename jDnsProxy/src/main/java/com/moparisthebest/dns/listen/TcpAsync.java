package com.moparisthebest.dns.listen;

import com.moparisthebest.dns.dto.Packet;
import com.moparisthebest.dns.net.BufChan;
import com.moparisthebest.dns.net.FullReadCompletionHandler;
import com.moparisthebest.dns.net.FullWriteCompletionHandler;
import com.moparisthebest.dns.net.FunctionalCompletionHandler;
import com.moparisthebest.dns.resolve.Resolver;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;

public class TcpAsync implements Listener {
    private final FunctionalCompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> newConnection = new FunctionalCompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>() {
        @Override
        public void completed(final AsynchronousSocketChannel sock, final AsynchronousServerSocketChannel listener) {
            listener.accept(listener, this); // get ready for next connection

            // callback 2
            BufChan.forTcp(sock).read(dnsSizeRead);
        }
    };

    private final FullReadCompletionHandler dnsRequestRead, dnsSizeRead;

    private final SocketAddress local;
    private boolean running = true;
    private Thread thisThread = null;

    public TcpAsync(final SocketAddress local, final Resolver resolver, final ExecutorService executor) {
        this.local = local;
        dnsRequestRead = new FullReadCompletionHandler() {
            @Override
            public void completed(final BufChan bufChan) {

                try {
                    bufChan.buf.flip();
                    bufChan.setRequest(new Packet(bufChan.buf));
                    //debugPacket(bufChan.getRequest().getBuf());

                    resolver.resolveAsync(bufChan, executor).whenCompleteAsync((bc, t) -> {
                        //System.out.println("got completed!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        if(t != null) {
                            t.printStackTrace();
                            return;
                        }
                        //debugPacket(bc.getResponse().getBuf());

                        bc.tcpHead.clear();
                        bc.tcpHead.putShort((short) bc.getResponse().getBuf().capacity());
                        bc.tcpHead.rewind();
                        bc.buf = bc.tcpHead;

                        bc.write((FullWriteCompletionHandler) (bc2) -> {
                            //System.out.println("header write complete");
                            bc2.buf = bc2.getResponse().getBuf();
                            bc2.buf.rewind();
                            bc2.write((FullWriteCompletionHandler) (unused) -> {
                                //System.out.println("body write complete");
                            });
                        });
                    }, executor);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                BufChan.forTcp(bufChan.sock).read(dnsSizeRead);
            }
        };
        dnsSizeRead = bc -> {
            final int dnsPacketSize = Short.toUnsignedInt(bc.tcpHead.getShort(0));
            //System.out.println("dnsPacketSize: " + dnsPacketSize);
            bc.buf = ByteBuffer.allocate(dnsPacketSize);
            // read the actual packet
            bc.read(dnsRequestRead);
        };
    }

    @Override
    public void run() {
        try (final AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open()) {

            listener.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            listener.bind(local);

            listener.accept(listener, newConnection);
            thisThread = Thread.currentThread();
            while (running) Thread.sleep(Long.MAX_VALUE);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            // just stop
        }
    }

    @Override
    public void close() {
        running = false;
        if (thisThread != null)
            thisThread.interrupt();
    }
}
