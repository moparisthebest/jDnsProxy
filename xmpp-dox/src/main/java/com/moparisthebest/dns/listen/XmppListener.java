package com.moparisthebest.dns.listen;

import com.moparisthebest.dns.dto.Packet;
import com.moparisthebest.dns.net.ParsedUrl;
import com.moparisthebest.dns.resolve.BaseRequestResponse;
import com.moparisthebest.dns.resolve.Resolver;
import com.moparisthebest.dns.xmpp.ConnectionDetails;
import com.moparisthebest.dns.xmpp.DnsIq;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.*;
import org.jxmpp.jid.Jid;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class XmppListener implements Listener {

    private final ConnectionDetails cd;

    private final Resolver resolver;
    private final ExecutorService executor;

    private boolean running = true;
    private Thread thisThread = null;

    public XmppListener(final ParsedUrl parsedUrl, final Resolver resolver, final ExecutorService executor) {
        this.cd = new ConnectionDetails(parsedUrl);
        this.resolver = resolver;
        this.executor = executor;
    }

    @Override
    public void run() {
        while (running)
            try {
                final AbstractXMPPConnection connection = cd.login();

                thisThread = Thread.currentThread();

                connection.registerIQRequestHandler(new IQRequestHandler() {
                    @Override
                    public IQ handleIQRequest(final IQ req) {
                        //System.out.println("request: " + req);
                        //System.out.println("request XML: " + req.toXML(StreamOpen.CLIENT_NAMESPACE));

                        final byte[] request = DnsIq.parseDnsIq(req);
                        if (request != null) {
                            //System.out.println("good request: " + req);
                            final XmppRequestResponse requestResponse = new XmppRequestResponse(req.getFrom(), new Packet(request));

                            resolver.resolveAsync(requestResponse).whenCompleteAsync((urr, t) -> {
                                if (t != null) {
                                    t.printStackTrace();
                                    return;
                                }
                                //debugPacket(urr.getResponse().getBuf());

                                final IQ resp = DnsIq.responseFor(req, urr.getResponse().getBuf());

                                try {
                                    //System.out.println("dns response: " + resp.toString());
                                    //System.out.println("dns response XML: " + resp.toXML(StreamOpen.CLIENT_NAMESPACE));
                                    connection.sendStanza(resp);
                                } catch (SmackException.NotConnectedException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }, executor);
                        }

                        // todo: respond with error Iq?
                        return null;
                    }

                    @Override
                    public Mode getMode() {
                        return Mode.sync;
                    }

                    @Override
                    public IQ.Type getType() {
                        return IQ.Type.get;
                    }

                    @Override
                    public String getElement() {
                        return DnsIq.ELEMENT;
                    }

                    @Override
                    public String getNamespace() {
                        return DnsIq.NAMESPACE;
                    }
                });

                while (running)
                    Thread.sleep(Long.MAX_VALUE);

            } catch (IOException | XMPPException | SmackException e) {
                e.printStackTrace();
                try {
                    // let's not burn the CPU
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    //ignore
                }
            } catch (InterruptedException e) {
                if (running) {
                    // not good
                    e.printStackTrace();
                    try {
                        // let's not burn the CPU
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        //ignore
                    }
                } else {
                    // being shutdown
                    return;
                }
            }
    }

    @Override
    public void close() {
        running = false;
        if (thisThread != null)
            thisThread.interrupt();
    }

    public class XmppRequestResponse extends BaseRequestResponse {

        private final Jid requester;

        public XmppRequestResponse(final Jid requester, final Packet request) {
            super(request);
            this.requester = requester;
        }

        public Jid getRequester() {
            return requester;
        }

        @Override
        public String toString() {
            return "XmppRequestResponse{" +
                    "requester=" + requester +
                    "} " + super.toString();
        }
    }
}
