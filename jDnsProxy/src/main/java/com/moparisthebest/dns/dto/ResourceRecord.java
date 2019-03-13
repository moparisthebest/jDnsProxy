package com.moparisthebest.dns.dto;

import java.nio.ByteBuffer;

public class ResourceRecord extends AbstractBufferWindow {

    private final Name name;

    public ResourceRecord(final ByteBuffer buf) {
        super(buf);
        //System.out.println("ResourceRecord start: " + start);
        this.name = new Name(buf);
        this.start = this.name.end;
        this.end = this.start + 10 + getRDataLen();
    }

    public Name getName() {
        return name;
    }

    public int getType() {
        return readU16(0);
    }

    public int getClassCode() {
        return readU16(2);
    }

    public int getTtl() {
        return readI32(4);
    }

    public void setTtl(final int ttl) {
        writeI32(4, ttl);
    }

    public int getRDataLen() {
        return readU16(8);
    }

    @Override
    public String toString() {
        return "ResourceRecord{" +
                "name=" + getName() +
                ", type=" + getType() +
                ", classCode=" + getClassCode() +
                ", ttl=" + getTtl() +
                ", rDataLen=" + getRDataLen() +
                '}';
    }
}
