package com.moparisthebest.dns.net;

import com.moparisthebest.dns.dto.Packet;
import com.moparisthebest.dns.resolve.BaseRequestResponse;
import com.moparisthebest.dns.resolve.RequestResponse;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;

public class BufChan extends BaseRequestResponse {
    public final ByteBuffer tcpHead;
    public final AsynchronousSocketChannel sock;

    public ByteBuffer buf;

    public BufChan(final ByteBuffer buf, final ByteBuffer tcpHead, final AsynchronousSocketChannel sock) {
        this.buf = buf;
        this.tcpHead = tcpHead;
        this.sock = sock;
    }

    public static BufChan forTcp(final AsynchronousSocketChannel sock) {
        final ByteBuffer buf = ByteBuffer.allocateDirect(2);
        return new BufChan(buf, buf, sock);
    }

    public final void read(final CompletionHandler<Integer,? super BufChan> handler)
    {
        this.sock.read(this.buf, this, handler);
    }

    public final void flipWrite(final CompletionHandler<Integer,? super BufChan> handler)
    {
        this.buf.flip();
        this.write(handler);
    }

    public final void write(final CompletionHandler<Integer,? super BufChan> handler)
    {
        this.sock.write(this.buf, this, handler);
    }
}
