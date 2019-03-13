XEP-XXXX DNS Queries over XMPP (DoX)
------------------------------------

Submitted [XEP](https://xmpp.org/extensions/inbox/dox.html)

```
# put your jid+pass details in jdns.xmpp.resolver.properties
$ java -jar jDnsProxy.jar jdns.xmpp.resolver.properties &
$ dig -p5353 @127.0.0.1 +short +tcp example.org
93.184.216.34
```

wire format of protocol (this is the request+response from the query above, A record for example.org):

request:
```xml
<iq to='dns@example.org/listener' id='27tZp-7' type='get'>
  <dns xmlns='urn:xmpp:dox:0'>vOIBIAABAAAAAAABB2V4YW1wbGUDb3JnAAABAAEAACkQAAAAAAAADAAKAAj5HO5JuEe+mA</dns>
</iq>
```

response:
```xml
<iq to='dns@example.org/resolver' id='27tZp-7' type='result'>
  <dns xmlns='urn:xmpp:dox:0'>vOKBoAABAAEAAAABB2V4YW1wbGUDb3JnAAABAAHADAABAAEAAAhjAARduNgiAAApEAAAAAAAAAA</dns>
</iq>
```

The content of the dns element is the DNS on-the-wire format is defined in [RFC1035].
The body MUST be encoded with base64 [RFC4648].
Padding characters for base64 MUST NOT be included.

[RFC1035]: https://tools.ietf.org/html/rfc1035
[RFC4648]: https://tools.ietf.org/html/rfc4648
