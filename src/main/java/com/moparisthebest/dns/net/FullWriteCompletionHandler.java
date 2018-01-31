package com.moparisthebest.dns.net;

import java.io.IOException;

public interface FullWriteCompletionHandler extends FunctionalCompletionHandler<Integer, BufChan>, FullBufChanCompletionHandler {
    @Override
    default void completed(final Integer result, final BufChan bc) {
        if (bc.buf.hasRemaining()) {
            bc.sock.write(bc.buf, bc, this);
        } else {
            bc.buf.clear();
        }
        completed(bc);
    }

    default FullWriteCompletionHandler getThisFullWriteCompletionHandler() {
        return this;
    }
}
