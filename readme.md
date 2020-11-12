jDnsProxy
---------

[![Build Status](https://ci.moparisthe.best/job/moparisthebest/job/jDnsProxy/job/master/badge/icon%3Fstyle=plastic)](https://ci.moparisthe.best/job/moparisthebest/job/jDnsProxy/job/master/)

Simple fast and lightweight DNS proxy and cache that listens on TCP or UDP ports and relays the request
to various upstream [DNS-over-TCP](https://tools.ietf.org/html/rfc1035), [DNS-over-TLS](https://tools.ietf.org/html/rfc7858),
or [DNS-over-HTTPS](https://tools.ietf.org/html/draft-hoffman-dns-over-https) servers, optionally over http or socks 
proxies (like tor), and optionally pinning public keys for complete TLS security.  Implements a simple 
response cache respecting TTLs but also implementing proper [Serve-Stale](https://tools.ietf.org/html/draft-ietf-dnsop-serve-stale) functionality.

This should support any current and future DNS record generically, as well as providing full DNSSEC support if upstream
resolvers do.

Sample/default configuration is in [jdnsproxy.properties](https://github.com/moparisthebest/jDnsProxy/blob/master/jdnsproxy.properties) and should be documented clearly there.

Build/run like so:
```
mvn clean package
java -jar jDnsProxy/target/jDnsProxy.jar ./jdnsproxy.properties

# or with xmpp:// listener+resolver support:
java -jar xmpp-dox/target/xmpp-dox-full.jar ./xmpp-dox/jdnsproxy.xmpp.resolver.properties

# or with http:// listener support:
java -jar http-doh-listener/target/http-doh-listener-full.jar ./jdnsproxy.properties

# or with support for *everything* we support in this repo (other jars we don't know about can also add additional protocol support)
java -jar jDnsProxy-all/target/jDnsProxy-all.jar ./jdnsproxy.properties
```

Implemented specs:

  * [RFC-1035: DOMAIN NAMES - IMPLEMENTATION AND SPECIFICATION](https://tools.ietf.org/html/rfc1035)
  * [RFC-7858: Specification for DNS over Transport Layer Security (TLS)](https://tools.ietf.org/html/rfc7858)
  * [RFC 8484: DNS Queries over HTTPS (DoH)](http://tools.ietf.org/html/rfc8484)
  * [Draft: Serving Stale Data to Improve DNS Resiliency](https://tools.ietf.org/html/draft-ietf-dnsop-serve-stale)
  * [RFC-6891: Extension Mechanisms for DNS (EDNS(0))](https://tools.ietf.org/html/rfc6891)
  * [DNS EDNS0 Option Codes (OPT)](https://www.iana.org/assignments/dns-parameters/dns-parameters.xhtml#dns-parameters-11)
  * [RFC-3225: Indicating Resolver Support of DNSSEC](https://tools.ietf.org/html/rfc3225)
  * [XEP-0418: DNS Queries over XMPP (DoX)](https://xmpp.org/extensions/xep-0418.html)

Use these for quick testing:
```
dig -p5353 @127.0.0.1 debian.org +tries=1 +retry=0 +tcp
dig -p5353 @127.0.0.1 debian.org +tries=1 +retry=0 +tcp +dnssec

dig -p5353 @127.0.0.1 debian.org +tries=1 +retry=0
dig -p5353 @127.0.0.1 debian.org +tries=1 +retry=0 +dnssec
```

And use this to extract TLS public keys in pinning format:
```
openssl s_client -connect 'dns.google.com:443' 2>&1 < /dev/null | sed -n '/-----BEGIN/,/-----END/p' | openssl x509 -noout -pubkey | openssl asn1parse -noout -inform pem -out /dev/stdout | openssl dgst -sha256 -binary | openssl base64
```

License
-------
MIT License, refer to LICENSE.txt
