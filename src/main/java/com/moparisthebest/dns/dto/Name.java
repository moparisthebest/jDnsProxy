package com.moparisthebest.dns.dto;

import com.moparisthebest.dns.DnsProxy;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Name extends AbstractBufferWindow {

    private static final int POINTER_MASK = 0b1100_0000;
    private static final int POINTER_UNMASK = 0b0011_1111_1111_1111;

    private final int[] partIndices;
    private final int domainLength;

    public Name(final ByteBuffer buf) {
        super(buf);
        /*
        System.out.println("before name");
        System.out.println("index: " + buf.position());
        System.out.println("start: " + start);
        DnsProxy.printPrettyHexBytes(buf);
        new Exception().printStackTrace(System.out);
        */

        final List<Integer> partIndices = new ArrayList<>();
        int domainLength = -1, i = start;
        do {
            final int b = Byte.toUnsignedInt(getBuf().get(i));
            /*
            System.out.printf("i: %d, b: %d\n", i, b);
            System.out.println("binaryString: " + Integer.toBinaryString(b));
             */
            if (b == 0) {

                // if end was not set by pointer below, set it now
                if (this.end == 0)
                    this.end = i + 1;
                break;
            } else if ((b & POINTER_MASK) == POINTER_MASK) {
                // if end was not set by previous, set it now
                if (this.end == 0)
                    this.end = i + 2;
                i = Short.toUnsignedInt(getBuf().getShort(i)) & POINTER_UNMASK;
                //System.out.println("found pointer, new index: " + i);
                // if we have already recursed to this index, some naughty DNS packet is trying to get us stuck in an infinite loop, we'll show them
                if(partIndices.contains(i))
                    throw new RuntimeException("NAME pointer would cause infinite recursion");
            } else if ((b & POINTER_MASK) == 0) {
                //System.out.printf("i: %d, partLength: %d\n", i, b);
                partIndices.add(i);
                domainLength += b + 1;
                i += b + 1;
            } else {
                throw new RuntimeException("10 and 01 combinations are reserved for future use (invalid NAME)");
            }
        }while(true);
        this.partIndices = new int[partIndices.size()];
        for(int x = 0; x < this.partIndices.length; ++x)
            this.partIndices[x] = partIndices.get(x);
        this.domainLength = domainLength == -1 ? 0 : domainLength;
        /*
        System.out.println("partIndices: "+partIndices);
        System.out.println("partIndices: "+ Arrays.toString(this.partIndices));
        System.out.println("this.domainLength: "+this.domainLength);
        */
    }

    public int getDomainLength() {
        return domainLength;
    }

    public String getDomain() {
        if(domainLength == 0)
            return "";
        final char[] ret = new char[domainLength];
        int y = -1;
        for (final int i : partIndices) {
            final int partLength = Byte.toUnsignedInt(buf.get(i));
            //System.out.printf("i: %d, partLength: %d\n", i, partLength);
            if (i != partIndices[0])
                ret[++y] = '.';
            for (int x = 1; x <= partLength; ++x)
                ret[++y] = (char) buf.get(i + x);
        }
        return new String(ret);
    }

    @Override
    public String toString() {
        return "Domain{" +
                "domainLength=" + getDomainLength() +
                ", domain=" + getDomain() +
                '}';
    }
}
