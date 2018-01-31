package com.moparisthebest.dns.dto;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Opt extends AbstractBufferWindow {

    public static final int OPT_TYPE_CODE = 41;

    private static final byte[] OPT_HEADER = new byte[]{0, 0, 41};

    private static final int FLAG_DNSSEC_OK = 0b1000_0000_0000_0000;

    public static Opt tryOpt(final ByteBuffer buf) {
        int start = buf.position();
        for(final byte oh : OPT_HEADER)
            if(buf.get(start++) != oh)
                return null;
        return new Opt(buf);
    }

    private Opt(final ByteBuffer buf) {
        super(buf);
        ++this.start; // just one after position
        this.end = this.start + 10 + getRDataLen();
    }

    public int getType() {
        return readU16(0);
    }

    public int getUdpPayloadSize() {
        return readU16(2);
    }

    public byte getExtendedRcode() {
        return readI8(4);
    }

    public int getVersion() {
        return readU8(5);
    }

    public int getFlags() {
        return readU16(6);
    }

    public boolean isDnssecOk() {
        return (getFlags() & FLAG_DNSSEC_OK) != 0;
    }

    public int getRDataLen() {
        return readU16(8);
    }

    public List<OptData> getOptData() {
        final int rDataLen = getRDataLen();
        if (rDataLen == 0)
            return Collections.emptyList();
        buf.position(getStart() + 10);
        final List<OptData> ret = new ArrayList<>();
        //System.out.println("all questions buf.position(): " + buf.position());
        //DnsProxy.printPrettyHexBytes(buf);
        while(buf.position() < (getStart() + rDataLen)) {
            final OptData o = new OptData(buf);
            ret.add(o);
            buf.position(o.getEnd());
        }
        //System.out.println("after questions buf.position(): " + buf.position());
        //DnsProxy.printPrettyHexBytes(buf);
        return ret;
    }

    @Override
    public String toString() {
        return "Opt{" +
                "type=" + getType() +
                ", udpPayloadSize=" + getUdpPayloadSize() +
                ", extendedRcode=" + getExtendedRcode() +
                ", version=" + getVersion() +
                ", flags=" + getFlags() +
                ", dnssecOk=" + isDnssecOk() +
                ", rDataLen=" + getRDataLen() +
                ", optData=" + getOptData() +
                '}';
    }
}
