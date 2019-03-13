package com.moparisthebest.dns.xmpp;

import com.moparisthebest.dns.net.ParsedUrl;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.util.XmppStringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

public class ConnectionDetails {

	private final String jid;
	private final String password;
	private final InetSocketAddress isa;

	private final long replyTimeout;

	public ConnectionDetails(final String jid, final String password, final InetSocketAddress isa, final long replyTimeout) {
		Objects.requireNonNull(jid, "user must be non-null");
		Objects.requireNonNull(isa, "isa must be non-null");
		this.jid = jid;
		this.password = password;
		this.isa = isa;
		this.replyTimeout = replyTimeout;
	}

	public ConnectionDetails(final ParsedUrl parsedUrl) {
		this(parsedUrl.getProps().get("user"), parsedUrl.getProps().get("pass"),
				(InetSocketAddress) parsedUrl.getAddr(),
				Long.parseLong(parsedUrl.getProps().getOrDefault("replyTimeout", "5000")));
	}

	public AbstractXMPPConnection login() throws InterruptedException, XMPPException, SmackException, IOException {

		final XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder()
				.setUsernameAndPassword(XmppStringUtils.parseLocalpart(jid), password)
				.setXmppDomain(XmppStringUtils.parseDomain(jid))
				.setHostAddress(isa.getAddress())
				.setPort(isa.getPort());

		final String resource = XmppStringUtils.parseResource(jid);
		if(resource != null && !resource.isEmpty()) {
			builder.setResource(resource);
		}

		final AbstractXMPPConnection connection = new XMPPTCPConnection(builder.build());
		connection.setReplyTimeout(replyTimeout);
		connection.connect().login();

		return connection;
	}
}
