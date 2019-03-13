package com.moparisthebest.dns.dto;

import java.nio.ByteBuffer;

public class Question extends AbstractBufferWindow {

    private final Name name;

    public Question(final ByteBuffer buf) {
        super(buf);
        this.name = new Name(buf);
        this.start = this.name.end;
        this.end = this.start + 4;
    }

    public Name getName() {
        return name;
    }

    public int getQType() {
        return readU16(0);
    }

    public int getQClass() {
        return readU16(2);
    }

    @Override
    public String toString() {
        return "Question{" +
                "name=" + getName() +
                ", qType=" + getQType() +
                ", qClass=" + getQClass() +
                '}';
    }
}
