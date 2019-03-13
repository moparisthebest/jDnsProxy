package com.moparisthebest.dns.net;

import java.io.IOException;

public interface FullReadCompletionHandler extends FunctionalCompletionHandler<Integer, BufChan>, FullBufChanCompletionHandler {
    @Override
    default void completed(final Integer result, final BufChan bc) {
        if (result == -1) {
            try {
                //System.out.println("closing");
                bc.sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        } else if(bc.buf.hasRemaining()) {
            // full read not completed
            bc.sock.read(bc.buf, bc, this);
            return;
        }
        completed(bc);
    }

    default FullReadCompletionHandler getThisFullReadCompletionHandler() {
        return this;
    }
}
