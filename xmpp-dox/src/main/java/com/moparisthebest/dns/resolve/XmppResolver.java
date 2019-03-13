package com.moparisthebest.dns.resolve;

import com.moparisthebest.dns.dto.Packet;
import com.moparisthebest.dns.net.ParsedUrl;
import com.moparisthebest.dns.xmpp.ConnectionDetails;
import com.moparisthebest.dns.xmpp.DnsIq;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

import java.io.*;

public class XmppResolver implements Resolver {

    private final Jid to;
    private final AbstractXMPPConnection connection;

    public XmppResolver(final ParsedUrl parsedUrl) {
        try {
            to = JidCreate.from(parsedUrl.getProps().get("resolverJid"));
            this.connection = new ConnectionDetails(parsedUrl).login();
        } catch (InterruptedException | XMPPException | SmackException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Packet resolve(final Packet request) throws Exception {
        // todo: should do this async
        final IQ req = DnsIq.requestTo(to, request.getBuf());
        //System.out.println("dns request: " + req.toString());
        //System.out.println("dns request XML: " + req.toXML(StreamOpen.CLIENT_NAMESPACE));
        final IQ resp = connection.sendIqRequestAndWaitForResponse(req);
        final byte[] ret = DnsIq.parseDnsIq(resp);
        if(ret == null)
            throw new Exception("XMPP request failed");
        return new Packet(ret);
    }
}
