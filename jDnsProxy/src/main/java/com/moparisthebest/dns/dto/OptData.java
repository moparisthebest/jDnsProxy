package com.moparisthebest.dns.dto;

import java.nio.ByteBuffer;

public class OptData extends AbstractBufferWindow {

    public OptData(final ByteBuffer buf) {
        super(buf);
        this.end = this.start + 4 + getDataLen();
    }

    public int getCode() {
        return readU16(0);
    }

    public int getDataLen() {
        return readU16(2);
    }

    @Override
    public String toString() {
        return "Opt{" +
                "code=" + getCode() +
                ", dataLen=" + getDataLen() +
                '}';
    }
}
