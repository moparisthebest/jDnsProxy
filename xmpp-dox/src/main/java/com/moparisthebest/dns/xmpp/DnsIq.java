package com.moparisthebest.dns.xmpp;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.UnparsedIQ;
import org.jxmpp.jid.Jid;

import java.nio.ByteBuffer;
import java.util.Base64;

public class DnsIq extends IQ {

	private static final Base64.Decoder decoder = Base64.getDecoder();
	private static final Base64.Encoder encoder = Base64.getEncoder().withoutPadding();

	public static final String ELEMENT = "dns";
	public static final String NAMESPACE = "urn:xmpp:dox:0";

	private final ByteBuffer bb;

	private DnsIq(final ByteBuffer bb) {
		super(ELEMENT, NAMESPACE);
		this.bb = bb;
	}

	public static DnsIq responseFor(final IQ iq, final ByteBuffer bb) {
		final DnsIq ret = new DnsIq(bb);
		ret.setStanzaId(iq.getStanzaId());
		ret.setTo(iq.getFrom());
		ret.setType(Type.result);
		return ret;
	}

	public static DnsIq requestTo(final Jid to, final ByteBuffer bb) {
		final DnsIq ret = new DnsIq(bb);
		ret.setTo(to);
		ret.setType(Type.get);
		return ret;
	}

	public static byte[] parseDnsIq(final IQ iq) {
		// todo: yikes this whole method is awful, what happened to XML ? investigate this later
		if(!(iq instanceof UnparsedIQ))
			return null;
		final UnparsedIQ uiq = (UnparsedIQ) iq;
		final String actualRequest = uiq.getContent().toString().replaceAll("<[^>]+>", "");
		//System.out.println("actualRequest: " + actualRequest);
		return decoder.decode(actualRequest);
	}

	@Override
	protected IQChildElementXmlStringBuilder getIQChildElementBuilder(final IQChildElementXmlStringBuilder buf) {
		buf.rightAngleBracket();
		buf.append(encoder.encodeToString(bb.array()));
		return buf;
	}
}
