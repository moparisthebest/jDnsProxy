package com.moparisthebest.dns.dto;

import java.nio.ByteBuffer;

public abstract class AbstractBufferWindow implements BufferWindow {
    protected final ByteBuffer buf;
    protected int start, end;

    public AbstractBufferWindow(final ByteBuffer buf, final int start) {
        this.buf = buf;
        this.start = start;
    }

    public AbstractBufferWindow(final ByteBuffer buf) {
        this(buf, buf.position());
    }

    @Override
    public ByteBuffer getBuf() {
        return buf;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getEnd() {
        return end;
    }
}
