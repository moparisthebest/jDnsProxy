package com.moparisthebest.dns.net;

@FunctionalInterface
public interface FullBufChanCompletionHandler {
    void completed(BufChan bc);
}
