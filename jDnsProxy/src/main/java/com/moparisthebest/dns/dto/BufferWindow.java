package com.moparisthebest.dns.dto;

import java.nio.ByteBuffer;

public interface BufferWindow {
    ByteBuffer getBuf();
    int getStart();
    int getEnd();

    default byte readI8(final int offset) {
        return getBuf().get(getStart() + offset);
    }

    default int readU8(final int offset) {
        return Byte.toUnsignedInt(getBuf().get(getStart() + offset));
    }

    default short readI16(final int offset) {
        return getBuf().getShort(getStart() + offset);
    }

    default int readU16(final int offset) {
        return Short.toUnsignedInt(getBuf().getShort(getStart() + offset));
    }

    default void writeU16(final int offset, final int u16) {
        getBuf().putShort(getStart() + offset, (short)u16);
    }

    default int readI32(final int offset) {
        return getBuf().getInt(getStart() + offset);
    }

    default void writeI32(final int offset, final int i32) {
        getBuf().putInt(getStart() + offset, i32);
    }

    default long readU32(final int offset) {
        return Integer.toUnsignedLong(getBuf().getInt(getStart() + offset));
    }

    default byte readBit(final int offset, final int bit) {
        return (getBuf().get(getStart() + offset) & (1 << bit)) != 0 ? (byte)0 : 1;
    }
}
