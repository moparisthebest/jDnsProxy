package com.moparisthebest.dns.dto;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class Packet extends AbstractBufferWindow {

    private static final int FLAG_QUERY = 0b1000_0000_0000_0000;
    private static final int FLAG_OPCODE_MASK = 0b0111_1000_0000_0000;
    private static final int FLAG_AUTHORITATIVE = 0b0000_0100_0000_0000;
    private static final int FLAG_TRUNCATED = 0b0000_0010_0000_0000;
    private static final int FLAG_RECURSION_DESIRED = 0b0000_0001_0000_0000;
    private static final int FLAG_RECURSION_AVAILABLE = 0b0000_0000_1000_0000;
    private static final int FLAG_AUTHENTICATED_DATA = 0b0000_0000_0010_0000;
    private static final int FLAG_CHECKING_DISABLED = 0b0000_0000_0001_0000;
    private static final int FLAG_RESERVED_MASK = 0b0000_0000_0100_0000;
    private static final int FLAG_RESPONSE_CODE_MASK = 0b0000_0000_0000_1111;

    public Packet(final ByteBuffer buf) {
        super(buf);
        if (this.buf.limit() < 12)
            throw new RuntimeException("header too short");
    }

    public Packet(final ByteBuffer buf, final int start, final int end) {
        super(buf, start);
        this.end = end;
    }

    public Packet(final byte[] buf) {
        this(ByteBuffer.wrap(buf));
    }

    public int getId() {
        return readU16(0);
    }

    public Packet setId(final int id) {
        writeU16(0, id);
        return this;
    }

    public int getFlags() {
        return readU16(2);
        //return readI16(2);
    }

    public boolean getQuery() {
        return (getFlags() & FLAG_QUERY) == 0;
    }

    public boolean getOpcode() {
        // todo: implement opcode: ((flags & flag::OPCODE_MASK) >> flag::OPCODE_MASK.trailing_zeros()).into(),
        return (getFlags() & FLAG_OPCODE_MASK) == 0;
    }

    public boolean getAuthoritative() {
        return (getFlags() & FLAG_AUTHORITATIVE) == 0;
    }

    public boolean getTruncated() {
        return (getFlags() & FLAG_TRUNCATED) == 0;
    }

    public boolean getRecursionDesired() {
        return (getFlags() & FLAG_RECURSION_DESIRED) == 0;
    }

    public boolean getRecursionAvailable() {
        return (getFlags() & FLAG_RECURSION_AVAILABLE) == 0;
    }

    public boolean getAuthenticatedData() {
        return (getFlags() & FLAG_AUTHENTICATED_DATA) == 0;
    }

    public boolean getCheckingDisabled() {
        return (getFlags() & FLAG_CHECKING_DISABLED) == 0;
    }

    public byte getResponseCode() {
        return (byte) (getFlags() & FLAG_RESPONSE_CODE_MASK);
    }

    public int getNumQuestions() {
        return readU16(4);
    }

    public int getNumAnswers() {
        return readU16(6);
    }

    public int getNumNameservers() {
        return readU16(8);
    }

    public int getNumAdditional() {
        return readU16(10);
    }

    public List<Question> getQuestions() {
        buf.position(12);
        final int questions = getNumQuestions();
        if (questions == 0)
            return Collections.emptyList();
        final Question[] ret = new Question[questions];
        //System.out.println("all questions buf.position(): " + buf.position());
        //DnsProxy.printPrettyHexBytes(buf);
        for (int x = 0; x < ret.length; ++x) {
            final Question q = new Question(buf);
            ret[x] = q;
            buf.position(q.getEnd());
        }
        //System.out.println("after questions buf.position(): " + buf.position());
        //DnsProxy.printPrettyHexBytes(buf);
        return Arrays.asList(ret);
    }

    private List<ResourceRecord> getResourceRecords(final int num) {
        if (num == 0)
            return Collections.emptyList();
        final ResourceRecord[] ret = new ResourceRecord[num];
        //System.out.println("all answers buf.position(): " + buf.position());
        //DnsProxy.printPrettyHexBytes(buf);
        for (int x = 0; x < ret.length; ++x) {
            final ResourceRecord q = new ResourceRecord(buf);
            ret[x] = q;
            //System.out.println("rr.getEnd()" + q.getEnd());
            buf.position(q.getEnd());
        }
        //System.out.println("after answers buf.position(): " + buf.position());
        //DnsProxy.printPrettyHexBytes(buf);
        return Arrays.asList(ret);
    }

    public List<ResourceRecord> getAnswers() {
        getQuestions(); // move position to correct place
        return getResourceRecords(getNumAnswers());
    }

    public List<ResourceRecord> getNameservers() {
        getAnswers(); // move position to correct place
        return getResourceRecords(getNumNameservers());
    }

    public List<ResourceRecord> getAdditional() {
        getNameservers(); // move position to correct place
        return getResourceRecords(getNumAdditional());
    }

    public Opt getOpt() {
        getNameservers(); // move position to correct place
        //System.out.println("before getOpt buf.position(): " + buf.position());
        final int num = getNumAdditional();
        if (num == 0)
            return null;
        for (int x = 0; x < num; ++x) {
            final Opt opt = Opt.tryOpt(buf);
            if(opt != null)
                return opt;
            //System.out.println("rr.getEnd()" + q.getEnd());
            buf.position(new ResourceRecord(buf).getEnd());
        }
        return null;
    }

    public boolean isDnssecOk() {
        if(getNumAdditional() < 1)
            return false;
        final Opt opt = getOpt();
        return opt != null && opt.isDnssecOk();
    }

    @Override
    public int getEnd() {
        if(end == 0) {
            // not cached yet
            getAdditional(); //moves pointer to end
            end = buf.position();
            buf.limit(end);
        }
        return end;
    }

    public int getLength() {
        return getEnd() - start;
    }

    public int getLowestTtl() {
        // todo: can make this a bit quicker since we know what positions we are in
        int lowestTtl = Integer.MAX_VALUE;
        for (final ResourceRecord rr : this.getAnswers()) {
            final int rrTtl = rr.getTtl();
            if (rrTtl < lowestTtl)
                lowestTtl = rrTtl;
        }
        for (final ResourceRecord rr : this.getNameservers()) {
            final int rrTtl = rr.getTtl();
            if (rrTtl < lowestTtl)
                lowestTtl = rrTtl;
        }
        for (final ResourceRecord rr : this.getAdditional()) {
            if (rr.getType() != Opt.OPT_TYPE_CODE) {
                final int rrTtl = rr.getTtl();
                if (rrTtl < lowestTtl)
                    lowestTtl = rrTtl;
            }
        }
        if(lowestTtl == Integer.MAX_VALUE)
            lowestTtl = 5; // todo: what is proper when no TTLs are available?
        //System.out.println("lowestTtl: " + lowestTtl);
        return lowestTtl;
    }

    public Packet modTtls(final Function<Integer, Integer> mod) {
        for (final ResourceRecord rr : this.getAnswers()) {
            rr.setTtl(mod.apply(rr.getTtl()));
        }
        for (final ResourceRecord rr : this.getNameservers()) {
            rr.setTtl(mod.apply(rr.getTtl()));
        }
        for (final ResourceRecord rr : this.getAdditional()) {
            if (rr.getType() != Opt.OPT_TYPE_CODE) {
                rr.setTtl(mod.apply(rr.getTtl()));
            }
        }
        return this;
    }

    public Packet copy() {
        final ByteBuffer copy = ByteBuffer.allocate(getEnd() - start);
        final ByteBuffer buf = this.buf.duplicate();
        buf.position(start);
        copy.put(buf);
        return new Packet(copy, start, end);
    }

    public byte[] copyRaw() {
        final byte[] copy = new byte[getEnd() - start];
        final ByteBuffer buf = this.buf.duplicate();
        buf.position(start);
        buf.get(copy);
        return copy;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "id=" + getId() +
                ", flags=" + getFlags() +
                ", query=" + getQuery() +
                ", opcode=" + getOpcode() +
                ", authoritative=" + getAuthoritative() +
                ", truncated=" + getTruncated() +
                ", recursionDesired=" + getRecursionDesired() +
                ", recursionAvailable=" + getRecursionAvailable() +
                ", authenticatedData=" + getAuthenticatedData() +
                ", checkingDisabled=" + getCheckingDisabled() +
                ", responseCode=" + getResponseCode() +
                ", numQuestions=" + getNumQuestions() +
                ", numAnswers=" + getNumAnswers() +
                ", numNameservers=" + getNumNameservers() +
                ", numAdditional=" + getNumAdditional() +
                ", questions=" + getQuestions() +
                ", answers=" + getAnswers() +
                ", nameservers=" + getNameservers() +
                ", additional=" + getAdditional() +
                ", opt=" + getOpt() +
                ", lowestTtl=" + getLowestTtl() +
                ", start=" + getStart() +
                ", end=" + getEnd() +
                ", length=" + getLength() +
                '}';
    }
}
